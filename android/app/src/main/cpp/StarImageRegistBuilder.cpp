//
// Created by 许舰 on 2018/1/11.
//

#include "StarImageRegistBuilder.h"
#include "Util.h"

struct registration_internal_data {
    StarImageRegistBuilder* starImageRegistBuilder;  // 类对象指针
    void (StarImageRegistBuilder::*pmf)(StarImage&, int, int); // 类成员函数指针
    StarImage* resultStarImage;
    int rowStart;
    int rowEnd;
};

void* registration_internal_thread(void* registration_internal_data_arg) {
    registration_internal_data* ptr_data_arg = static_cast<registration_internal_data*> (registration_internal_data_arg);
    // 取出 StarImageRegistBuilder 类
    StarImageRegistBuilder* ptrSIRB = ptr_data_arg->starImageRegistBuilder;
    void (StarImageRegistBuilder::*pmf)(StarImage&, int, int) = ptr_data_arg->pmf;  // 析取成员函数

    StarImage& resultStarImage = *(ptr_data_arg->resultStarImage);
    int rowStart = ptr_data_arg->rowStart;
    int rowEnd = ptr_data_arg->rowEnd;

    (ptrSIRB->*pmf)(resultStarImage, rowStart, rowEnd);  // 调用成员函数

    /**
     * 这里没有返回值的话，那么线程函数执行完成时将会出错。必须要有返回值
     * 报错信息类似于：Fatal signal 4 (SIGILL), code 1, fault addr 0xc84f7ede pid 8
     */
    return (void*) 1;
}

/**
 *
 * @param targetImage:  train image
 * @param sourceImages: query image
 * @param rowParts
 * @param columnParts
 */
StarImageRegistBuilder::StarImageRegistBuilder(Mat_<Vec3b>& targetImage, std::vector<Mat_<Vec3b>>& sourceImages,
                                               Mat& skyMaskMat, int rowParts, int columnParts, bool imageMode) {
    this->rowParts = rowParts;
    this->columnParts = columnParts;
    this->imageCount = (int)sourceImages.size() + 1;

    this->skyMaskMat = skyMaskMat;
    this->skyBoundaryRange = (int)(skyMaskMat.rows * 0.005);

    this->targetImage = targetImage;
    this->sourceImages = sourceImages;

    // 开始对每一张图片进行分块操作
    for (int index = 0; index < sourceImages.size(); index ++) {
        StarImage starImage = StarImage(sourceImages[index], this->rowParts, this->columnParts);
        this->sourceStarImages.push_back(starImage);
    }

    this->targetStarImage = StarImage(targetImage, this->rowParts, this->columnParts);
    this->imageMode = imageMode;
}


/**
 *
 * @param imgPath
 */
void StarImageRegistBuilder::addSourceImagePath(string imgPath) {
    /**
     * 必须首先调用 addTargetImagePath。
     * 如果图片大小和targetImage不相同，那么不能加入图片
     */
    Mat image = imread(imgPath, IMREAD_UNCHANGED);
    if (image.rows != targetStarImage.getImage().rows || image.cols != targetStarImage.getImage().cols) {
        return;
    }
    this->sourceStarImages.push_back(StarImage(image, this->rowParts, this->columnParts));
}

/**
 *
 * @param imgPath
 */
void StarImageRegistBuilder::setTargetImagePath(string imgPath) {
    this->targetStarImage = StarImage(imread(imgPath, IMREAD_UNCHANGED), this->rowParts, this->columnParts);
}

/**
 *
 * @return
 */
Mat_<Vec3b> StarImageRegistBuilder::registration(int mergeMode) {

    // 最终配准的图像信息

    StarImage resultStarImage = StarImage(Mat(this->targetStarImage.getImage().rows,
                                              this->targetStarImage.getImage().cols,
                                              this->targetStarImage.getImage().type(), cv::Scalar(0, 0, 0)),
                                          this->rowParts, this->columnParts, true); // true 表示 clone，深拷贝，不然会出现图片重叠的现象

    // 多线程操作实例（每一个线程处理一行小图片部分）
    pthread_t processThreads[REGISTER_THREAD_NUMS];
    // 初始化，并且设置线程是可连接的
    pthread_attr_t threadAttr;

    pthread_attr_init(&threadAttr);
    pthread_attr_setdetachstate(&threadAttr, PTHREAD_CREATE_JOINABLE);

    registration_internal_data dataArgs[REGISTER_THREAD_NUMS];
    int threadRowStep = (int)ceil(this->rowParts * 1.0 / REGISTER_THREAD_NUMS);

    for (int threadIndex = 0; threadIndex < REGISTER_THREAD_NUMS; threadIndex ++) {
        dataArgs[threadIndex] = {
                .starImageRegistBuilder = NULL,
                .pmf = NULL,
                .resultStarImage = NULL,
                .rowStart = 0,
                .rowEnd = 0
        };

        dataArgs[threadIndex].starImageRegistBuilder = this;
        dataArgs[threadIndex].pmf = &StarImageRegistBuilder::registration_internal; // 填充成员函数地址
        dataArgs[threadIndex].rowStart = threadIndex * threadRowStep;

        if (threadIndex == REGISTER_THREAD_NUMS - 1) {
            // 最后一个线程（防止没有整除的情况）
            dataArgs[threadIndex].rowEnd = this->rowParts;
        } else {
            dataArgs[threadIndex].rowEnd = (threadIndex + 1) * threadRowStep;
        }

        dataArgs[threadIndex].resultStarImage = &resultStarImage;

        int rc = pthread_create(&processThreads[threadIndex], NULL,
                                registration_internal_thread, (void*) &dataArgs[threadIndex]);
        if (rc) {
            LOGD("create register thread: %d failed", threadIndex);
        } else {
            LOGD("create register thread: %d success", threadIndex);
        }
    }

    // 主线程需要等待子线程完成
    pthread_attr_destroy(&threadAttr);
    for (int threadIndex = 0; threadIndex < REGISTER_THREAD_NUMS; threadIndex ++) {
        int rc = pthread_join(processThreads[threadIndex], NULL);
        LOGD("pthread_join threadIndex: %d", threadIndex);

        if (rc) {
            LOGD("join register thread: %d failed", threadIndex);
        } else {
            LOGD("join register thread: %d success", threadIndex);
        }
    }

//    // 开始对图像的每一个部分进行对齐操作，分别与targetStarImage 做对比
//    for (int index = 0; index < this->sourceStarImages.size(); index ++) {
//        StarImage tmpStarImage = this->sourceStarImages[index];  // 直接赋值，不是指针操作，
//        // 对于每一小块图像都做配准操作
//        for (int rPartIndex = 0; rPartIndex < this->rowParts; rPartIndex ++) {
//            for (int cPartIndex = 0; cPartIndex < this->columnParts; cPartIndex ++) {
//                Mat homo;
//                bool existHomo = false;
//
//                Mat tmpRegistMat = this->getImgTransform(tmpStarImage.getStarImagePart(rPartIndex, cPartIndex),
//                                                         this->targetStarImage.getStarImagePart(rPartIndex, cPartIndex), homo, existHomo);
//
//                Mat_<Vec3b>& queryImgTransform = this->sourceImages[index];
//                if (existHomo) {
//                    queryImgTransform = getTransformImgByHomo(queryImgTransform, homo);
//                } else {
//                    queryImgTransform = this->targetImage;
//                }
//                resultStarImage.getStarImagePart(rPartIndex, cPartIndex).addImagePixelValue(tmpRegistMat, queryImgTransform, this->skyMaskMat, this->imageCount);
//            }
//        }
//    }

    // 对于配准图像和待配准图像做平均值操作（先买上目标图像的那一部分，这一段代码不能放在source整合的前面，不然图片会出现缝隙，原因待查）
    for (int rPartIndex = 0; rPartIndex < this->rowParts; rPartIndex ++) {
        for (int cPartIndex = 0; cPartIndex < this->columnParts; cPartIndex++) {
            Mat_<Vec3b> targetImg = this->targetStarImage.getStarImagePart(rPartIndex, cPartIndex).getImage();
            resultStarImage.getStarImagePart(rPartIndex, cPartIndex).addImagePixelValue(targetImg, this->targetImage, this->skyMaskMat, this->imageCount);
        }
    }

    // 对配准好的图像进行整合
    return resultStarImage.mergeStarImageParts();
}


/**
 * 多线程配准处理函数
 * @param resultStarImage
 * @param rowStart
 * @param rowEnd
 * [rowStart, rowEnd)
 */
void StarImageRegistBuilder::registration_internal(StarImage& resultStarImage, int rowStart, int rowEnd) {

    LOGD("enter registration_internal: %d ~ %d success", rowStart, rowEnd);

    for (int rPartIndex = rowStart; rPartIndex < rowEnd; rPartIndex ++) {
        // 开始对图像的每一个部分进行对齐操作，分别与targetStarImage 做对比
        for (int index = 0; index < this->sourceStarImages.size(); index ++) {
            StarImage& tmpStarImage = this->sourceStarImages[index];  // 直接赋值，不是指针操作，
            // 对于每一小块图像都做配准操作
            for (int cPartIndex = 0; cPartIndex < this->columnParts; cPartIndex ++) {
                LOGD("rPartIndex %d, index %d, cPartIndex %d. Start", rPartIndex, index, cPartIndex);
                Mat homo;
                bool existHomo = false;

                Mat tmpRegistMat = this->getImgTransform(tmpStarImage.getStarImagePart(rPartIndex, cPartIndex),
                                                         this->targetStarImage.getStarImagePart(rPartIndex, cPartIndex), homo, existHomo);

                Mat_<Vec3b> queryImgTransform = this->sourceImages[index];
                if (existHomo) {
                    queryImgTransform = getTransformImgByHomo(queryImgTransform, homo);
                } else {
                    queryImgTransform = this->targetImage;
                }
                resultStarImage.getStarImagePart(rPartIndex, cPartIndex).addImagePixelValue(tmpRegistMat, queryImgTransform, this->skyMaskMat, this->imageCount);
                LOGD("rPartIndex %d, index %d, cPartIndex %d. End", rPartIndex, index, cPartIndex);
            }
        }
    }
    LOGD("leave registration_internal: %d ~ %d success", rowStart, rowEnd);
}


/**
 *
 * @param sourceImagePart
 * @param targetImagePart
 * @return
 */
Mat StarImageRegistBuilder::getImgTransform(StarImagePart& sourceImagePart, StarImagePart& targetImagePart, Mat& oriImgHomo, bool& existHomo) {
    Mat sourceImg = sourceImagePart.getImage(); // query image
    Mat targetImg = targetImagePart.getImage(); // train image

    // 取出当前mask起始点的位置
    int rMaskIndex = sourceImagePart.getRowPartIndex() * sourceImg.rows;
    int cMaskIndex = sourceImagePart.getColumnPartIndex() * sourceImg.cols;


//        if( !sourceImg.data || !targetImg.data )
//        { std::cout<< " --(!) Error loading images " << std::endl; return NULL; }

    //-- Step 1: Detect the keypoints using SURF Detector, compute the descriptors
    int minHessian = 400;

    Ptr<SURF> detector = SURF::create( minHessian );
    std::vector<KeyPoint> keypoints_1, keypoints_2;

    detector->detect( sourceImg, keypoints_1 );
    detector->detect( targetImg, keypoints_2 );

    // SurfDescriptorExtractor extractor;
    Ptr<SURF> extractor = SURF::create();
    Mat descriptors_1, descriptors_2;
    extractor->compute(sourceImg, keypoints_1, descriptors_1);
    extractor->compute(targetImg, keypoints_2, descriptors_2);

    //-- Step 2: Matching descriptor vectors using FLANN matcher
    FlannBasedMatcher matcher;
    std::vector< vector<DMatch> > knnMatches;
    try {
        /**
         * 经过调试，发现照片有些部分是全暗的，根本无法找到特征点，这个时候 FlannBasedMatcher 会报
         * opencv knnMatch error: (-210) type=0
         */
        matcher.knnMatch(descriptors_1, descriptors_2, knnMatches, 2);
    } catch (cv::Exception) {
        return this->imageMode ? targetImg : sourceImg; // 直接采集source img的信息。
    }

    // 1. 最近邻次比律法，取出错误匹配点
    std::vector<DMatch> tempMatches; // 符合条件的匹配对
    for (int index = 0; index < knnMatches.size(); index ++) {
        DMatch firstMatch = knnMatches[index][0];
        DMatch secondMatch = knnMatches[index][1];
        if (firstMatch.distance < 0.9 * secondMatch.distance) {
            tempMatches.push_back(firstMatch);
        }
    }

    // 2. 对应 query image中的多个特征点对应 target image中的同一个特征点的情况（导致计算出的映射关系不佳），只取最小的匹配
    vector<Point2f> imagePoints1, imagePoints2;
    std::map<int, DMatch> matchRepeatRecords;
    for (int index = 0; index < tempMatches.size(); index ++) {
//        int queryIdx = matches[index].queryIdx;
        int trainIdx = tempMatches[index].trainIdx;

        // 记录标准图像中的每个点被配准了多少次，如果被配准多次，那么说明这个特征点匹配不合格
        if (matchRepeatRecords.count(trainIdx) <= 0) {
            matchRepeatRecords[trainIdx] = tempMatches[index];
        } else {
            // 多个query image的特征点对应 target image的特征点时，只取距离最小的一个匹配（这个算是双向匹配的改进）
            if (matchRepeatRecords[trainIdx].distance > tempMatches[index].distance) {
                matchRepeatRecords[trainIdx] = tempMatches[index];
            }
        }
    }


    // 3. 计算匹配特征点对的标准差信息（标准差衡量标准之间只有相互的，那么要将标准差，以及match等一切中间过程存储起来，之后再进行配准，太耗内存）
    std::map<int, DMatch>::iterator iter;
//    std::vector<double> matchDist;
//    double matchDistCount = 0.0;  // dist数组个数
//    double matchDistSum = 0.0;  // dist数组总和
//    double matchDistMean = 0.0;  // dist数组平均值
//    for (iter = matchRepeatRecords.begin(); iter != matchRepeatRecords.end(); iter ++) {
//        matchDistSum += iter->second.distance;
//        matchDistCount += 1;
//    }
//    matchDistMean = matchDistSum / matchDistCount;
//    double matchDistAccum = 0.0;
//    for (iter = matchRepeatRecords.begin(); iter != matchRepeatRecords.end(); iter ++) {
//        double distance = iter->second.distance;
//        matchDistAccum += (distance - matchDistMean) * (distance - matchDistMean);
//    }
//    double matchDistStdev = sqrt(matchDistAccum / (matchDistCount - 1));

    // 3.1 获取准确的最大最小值
    double maxMatchDist = 0;
    double minMatchDist = 100;
    for (iter = matchRepeatRecords.begin(); iter != matchRepeatRecords.end(); iter ++) {
        if (iter->second.distance < minMatchDist) {
            minMatchDist = iter->second.distance;
        }
        if (iter->second.distance > maxMatchDist) {
            maxMatchDist = iter->second.distance;
        }
    }

    // 4. 根据特征点匹配对，分离出两幅图像中已经被匹配的特征点
    std::vector<DMatch> matches;
    double matchThreshold = minMatchDist + (maxMatchDist - minMatchDist) * 0.1;  // 阈值越大，留下的特征点越多（这个阈值是一个做文章的地方）
    double slopeThreshold = 0.3;
    for (iter = matchRepeatRecords.begin(); iter != matchRepeatRecords.end(); iter ++) {

        DMatch match = iter->second;
        int queryIdx = match.queryIdx;
        int trainIdx = match.trainIdx;

        // 将检测出的靠近边缘的特征点去除
        int qy = (int)(keypoints_1[queryIdx].pt.y + this->skyBoundaryRange + rMaskIndex),
                ty = (int)(keypoints_2[trainIdx].pt.y + this->skyBoundaryRange + rMaskIndex);
        int qx = (int)(keypoints_1[queryIdx].pt.x + cMaskIndex), tx = (int)(keypoints_2[trainIdx].pt.x + cMaskIndex);

        if (qy >= this->skyMaskMat.rows || ty >= this->skyMaskMat.rows) {
            continue;
            // Mat.at(行数, 列数)
        } else if ( this->skyMaskMat.at<uchar>(qy, qx) == 0 || this->skyMaskMat.at<uchar>(ty, tx) == 0 ) {
            continue;
        }

        // 5. 设置 匹配点之间的 distance 阈值来选出 质量好的特征点（方差策略的替代方案）
        // 6. 计算两个匹配点之间的斜率
        double slope = abs( (keypoints_1[queryIdx].pt.y - keypoints_2[trainIdx].pt.y) * 1.0 /
                            (keypoints_1[queryIdx].pt.x -
                             (keypoints_2[trainIdx].pt.x + targetImg.cols) ) );

        // && iter->second.distance < matchThreshold
        if ( slope < slopeThreshold && iter->second.distance < matchThreshold) {
            matches.push_back(iter->second);
            imagePoints1.push_back(keypoints_1[queryIdx].pt);
            imagePoints2.push_back(keypoints_2[trainIdx].pt);
        }
    }

    // 测试代码：
    Mat img_matches;
    drawMatches( sourceImg, keypoints_1, targetImg, keypoints_2, matches, img_matches );

    int IMG_MATCH_POINT_THRESHOLD = 10;  // 这里是个做文章的地方

    // 对应图片部分中没有特征点的情况（导致计算出的映射关系不佳，至少要4对匹配点才能计算出匹配关系）
    if (imagePoints1.size() >= IMG_MATCH_POINT_THRESHOLD && imagePoints2.size() < IMG_MATCH_POINT_THRESHOLD) {
        // 没有特征点信息，那么说明这个区域是没有特征的，所以返回 查询图片部分，作为内容填充
        return this->imageMode ? targetImg : sourceImg;
    } else if (imagePoints1.size() < IMG_MATCH_POINT_THRESHOLD && imagePoints2.size() >= IMG_MATCH_POINT_THRESHOLD) {
        return targetImg;
    } else if (imagePoints1.size() < IMG_MATCH_POINT_THRESHOLD && imagePoints2.size() < IMG_MATCH_POINT_THRESHOLD) {
        return targetImg;  // 特征点都不足时，以targetImage为基础进行的配准
    }

    // 获取图像1到图像2的投影映射矩阵 尺寸为3*3
    Mat homo = findHomography(imagePoints1, imagePoints2, CV_RANSAC);
    // 也可以使用getPerspectiveTransform方法获得透视变换矩阵，不过要求只能有4个点，效果稍差
    // Mat homo = getPerspectiveTransform(imagePoints1,imagePoints2);
    /**
     * 这里如果有一副图片中的特征点过少，导致查询图片部分 中的多个特征点直接 和 目标图片部分 中的同一个特征点相匹配，
     * 那么会导致算不出变换矩阵，变换矩阵为 [] 。导致错误。
     */
    if (homo.rows < 3 || homo.cols < 3) {
        existHomo = false;
        if (imagePoints1.size() > imagePoints2.size()) {
            return this->imageMode ? targetImg : sourceImg; // 因为是星空图片，移动不会很大，在目标图片部分的特征点几乎没有的情况下，那么直接返回待配准图像进行填充细节。
        } else {
            return targetImg;
        }

    }

    oriImgHomo = homo;
    existHomo = true;
    //图像配准
    Mat sourceImgTransform;
    warpPerspective(sourceImg, sourceImgTransform ,homo , Size(targetImg.cols, targetImg.rows));

    return sourceImgTransform;
}
