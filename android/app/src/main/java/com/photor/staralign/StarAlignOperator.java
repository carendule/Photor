package com.photor.staralign;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by xujian on 2018/3/1.
 */

public class StarAlignOperator {

    public static final int REQUEST_RESULT_CODE = 123; // 用于启动StarAlignResultActivity的Request Code。

    // 用于启动StarAlignResultActivity
    public static final String EXTRA_ALIGN_RESULT_PATH = "extra_align_result_path"; // 对齐结果的图片路径


    public static StarAlignResultBuilder resultBuilder() {
        return new StarAlignResultBuilder();
    }
    public static class StarAlignResultBuilder {  // 这个类如果是private，那么外部类无法调用这了builder类里面的函数。
        private Bundle bundle;
        private Intent intent;

        public StarAlignResultBuilder() {
            bundle = new Bundle();
            intent = new Intent();
        }

        public StarAlignResultBuilder setAlignResultPath(String alignResultPath) {
            bundle.putString(EXTRA_ALIGN_RESULT_PATH, alignResultPath);
            return this;
        }

        public void start(Activity activity) {
            intent.setClass(activity, StarAlignResultActivity.class);
            intent.putExtras(bundle);
            activity.startActivityForResult(intent, REQUEST_RESULT_CODE);
        }
    }

    public static StarAlignSplitBuilder splitBuilder() {
        return new StarAlignSplitBuilder();
    }

    // 用于启动 StarAlignSplitActivity
    public static final String EXTRA_MASK_IMG_PATH = "extra_mask_img_path";
    public static final String EXTRA_BASE_SELECT_PHOTO_PATH = "extra_base_select_photo_path";

    public static final int REQUEST_BOUNDARY_CODE = 124;  // 用于启动 StarAlignSplitActivity的Request Code。

    public static class StarAlignSplitBuilder {
        private Bundle bundle;
        private Intent intent;

        public StarAlignSplitBuilder() {
            bundle = new Bundle();
            intent = new Intent();
        }

        public StarAlignSplitBuilder setBasePhotoPath(String basePhotoPath) {
            bundle.putString(EXTRA_BASE_SELECT_PHOTO_PATH, basePhotoPath);
            return this;
        }

        public void start(Activity activity) {
            intent.setClass(activity, StarAlignSplitActivity.class);
            intent.putExtras(bundle);
            activity.startActivityForResult(intent, REQUEST_BOUNDARY_CODE);
        }
    }
}