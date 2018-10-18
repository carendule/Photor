package com.photor.home.focusstack;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;
import android.widget.Toast;

import com.example.file.FileUtils;
import com.example.focusstackinglib.FocusStackProcessing;
import com.example.photopicker.PhotoPicker;
import com.example.photopicker.PhotoPreview;
import com.photor.R;
import com.photor.base.activity.PhotoOperateBaseActivity;
import com.photor.base.adapters.PhotoAdapter;
import com.photor.base.adapters.event.PhotoItemClickListener;
import com.photor.home.exposure.ExposureBaseActivity;
import com.photor.home.exposure.event.ExposureEnum;
import com.photor.home.focusstack.event.FocusStackEnum;
import com.photor.util.AlertDialogsHelper;

import java.io.File;
import java.util.Arrays;

/**
 * @author htwxujian@gmail.com
 * @date 2018/10/18 14:25
 */
public class FocusStackActivity extends PhotoOperateBaseActivity {

    private String resFocusStackPath = null;
    private static final int MAX_PHOTO_COUNT = 15;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initUI();
    }

    private void initUI() {
        // 0. 设置图片选择的step view
        stepView = findViewById(R.id.photo_operate_step_view);
        stepView.setSteps(Arrays.asList(getResources().getStringArray(R.array.focus_stack_steps)));

        // 1. 初始化显示选择图片的RecyclerView
        recyclerView = findViewById(R.id.photo_operate_rv);
        photoAdapter = new PhotoAdapter(selectedPhotos, this);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3, OrientationHelper.VERTICAL));
        recyclerView.setAdapter(photoAdapter);

        recyclerView.addOnItemTouchListener(new PhotoItemClickListener(this,
                new PhotoItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        if (photoAdapter.getItemViewType(position) == PhotoAdapter.TYPE_ADD) {
                            PhotoPicker.builder()
                                    .setSelected(selectedPhotos)
                                    .setPhotoCount(MAX_PHOTO_COUNT)
                                    .start(FocusStackActivity.this);
                        } else {
                            PhotoPreview.builder()
                                    .setPhotos(selectedPhotos)
                                    .setCurrentItem(position)
                                    .start(FocusStackActivity.this);
                        }
                    }
                }));

        // 2. 设置 选择图片/进行图片景深合成 操作的按钮
        operateBtn = findViewById(R.id.photo_operate_btn);
        updateStepInfo(FocusStackEnum.FOCUS_STACK_SELECT_PHOTO.getCode());  // 设置操作步骤信息
        operateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentStep = stepView.getCurrentStep();
                if (currentStep == FocusStackEnum.FOCUS_STACK_SELECT_PHOTO.getCode()) {
                    // 1. 第一步：选择景深合成的原始照片
                    PhotoPicker.builder()
                            .setGridColumnCount(4)
                            .setPhotoCount(MAX_PHOTO_COUNT)
                            .start(FocusStackActivity.this);
                } else if (currentStep == FocusStackEnum.FOCUS_STACK_RESULT.getCode()) {
                    // 2. 第二步：景深合成
                    if (selectedPhotos.size() < 2) {
                        Toast.makeText(FocusStackActivity.this,
                                getResources().getString(R.string.focus_stack_photo_count_not_enough),
                                Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        // 开始景深合成操作
                        new FocusStackTask().execute();
                    }
                }
            }
        });
    }

    // 根据情况显示step view
    private void updateStepInfo(int currentStep) {
        // 首先更新step view的信息
        if (currentStep < stepView.getStepCount()) {
            stepView.go(currentStep, true);
        }

        if (currentStep == FocusStackEnum.FOCUS_STACK_SELECT_PHOTO.getCode()) {
            operateBtn.setText(R.string.focus_stack_btn_select_label);
        } else if (currentStep == FocusStackEnum.FOCUS_STACK_RESULT.getCode()) {
            operateBtn.setText(R.string.focus_stack_btn_operate_label);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK &&
                (PhotoPicker.REQUEST_CODE == requestCode || PhotoPreview.REQUEST_CODE == requestCode)) {
            if (selectedPhotos.size() <= 0) {
                updateStepInfo(ExposureEnum.EXPOSURE_SELECT_PHOTOS.getCode());
            } else {
                updateStepInfo(ExposureEnum.EXPOSURE_RESULT.getCode());
            }
        }
    }

    /**
     * 进行景深合成的任务
     */
    private class FocusStackTask extends AsyncTask<Void, Void, Bitmap> {
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            resFocusStackPath = FileUtils.generateImgAbsPath();
            dialog = AlertDialogsHelper.getLoadingDialog(FocusStackActivity.this,
                    getResources().getString(R.string.loading), false);
            dialog.show();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (selectedPhotos == null || selectedPhotos.size() <= 0) {
                return null;
            }
            Bitmap bitmap = FocusStackProcessing.processImage(selectedPhotos, 70, (short) 7, 5.0f, resFocusStackPath);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
//                FileUtils.saveImgBitmap(resFocusStackPath, bitmap);  // native代码中已经存储了景深合成的图像，所以不需要存储第二次
                dialog.dismiss();
                // 开启显示结果图片的Activity
                FocusStackOperator.builder()
                        .setFocusStackResPath(resFocusStackPath)
                        .setFocusStackResUri(Uri.fromFile(new File(resFocusStackPath)))
                        .setIsFromOperate(true)
                        .start(FocusStackActivity.this);

            } else {
                dialog.dismiss();
                Toast.makeText(FocusStackActivity.this,
                        ExposureEnum.EXPOSURE_MERGE_FAILED.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
