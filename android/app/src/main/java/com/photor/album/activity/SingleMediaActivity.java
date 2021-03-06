package com.photor.album.activity;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.ChangeBounds;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.example.file.FileUtils;
import com.example.preference.PreferenceUtil;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.photor.BuildConfig;
import com.photor.R;
import com.photor.album.adapter.ImageAdapter;
import com.photor.album.entity.Album;
import com.photor.album.entity.Media;
import com.photor.util.Measure;
import com.photor.album.views.PagerRecyclerView;
import com.photor.base.activity.BaseActivity;
import com.photor.base.activity.PhotoExifDetailActivity;
import com.photor.base.fragment.AlbumFragment;
import com.photor.data.TrashBinRealmModel;
import com.photor.util.AlertDialogsHelper;
import com.photor.util.BasicCallBack;
import com.example.color.ColorPalette;
import com.photor.util.SnackBarHandler;
import com.example.theme.ThemeHelper;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.xinlan.imageeditlibrary.editimage.EditImageActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;

import static com.example.constant.PhotoOperator.EXTRA_IS_SAVED_CROP_RES;
import static com.example.constant.PhotoOperator.EXTRA_ORI_IMG_PATH;
import static com.example.constant.PhotoOperator.EXTRA_PHOTO_IS_FROM_OPERATE_RESULT;
import static com.example.constant.PhotoOperator.EXTRA_PHOTO_TO_PDF_PATH;
import static com.example.constant.PhotoOperator.REQUEST_ACTION_EDITIMAGE;
import static com.photor.util.ActivitySwitchHelper.context;
import static com.photor.util.ActivitySwitchHelper.getContext;

public class SingleMediaActivity extends BaseActivity implements ImageAdapter.OnSingleTap, ImageAdapter.EnterTransition {

    public static final String ACTION_OPEN_ALBUM = "android.intent.action.pagerAlbumMedia";
    private static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";

    private Boolean trashdis;  // 是否是浏览回收站中的文件夹
    private ArrayList<Media> trashbinlistd;  // 回收站中照片的总数

    private PreferenceUtil SP;
    private static int SLIDE_SHOW_INTERVAL = 5000;  // 控制幻灯片播放的时长

    private RelativeLayout relativeLayout, ActivityBackground;

    public Boolean allPhotoMode;  // 是否由全部照片模式跳转而来的
    public int all_photo_pos;  // 全部照片模式下，照片的当前位置信息
    public int size_all;  // 全部照片模式下，照片的总数量

    public static String pathForDescription;  // 相册模式下传递过来的照片路径
    private boolean customUri = false;  // 图片是否是以URI形式传递来的
    private boolean fullScreenMode = false; // 当前是否以全屏模式来显示照片
    private String imgEditResPath;  // 编辑图片后生成结果图片的位置

    // 跟显示系统UI相关
    private Handler handler;
    private Runnable runnable;

    private ImageAdapter adapter;

    // 幻灯片播放
    private boolean slideshow = false;

    // 数据库实例
    private Realm realm;

    public int current_image_pos; // 记录当前图片的下标位置（在全局图片显示的模式下）

    @Nullable
    @BindView(R.id.view_switcher_single_media)
    ViewSwitcher viewSwitcher;

    @Nullable
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Nullable
    @BindView(R.id.photos_pager)
    PagerRecyclerView mViewPager;  // 图片的RecyclerView

    @Nullable
    @BindView(R.id.toolbar_bottom)
    ActionMenuView bottomBar;

    @Nullable
    @BindView(R.id.PhotoPager_Layout)
    View parentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);
        ButterKnife.bind(this);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        };
        startHandler();  // 5秒时候会自动隐藏系统界面

        ActivityBackground = relativeLayout = findViewById(R.id.PhotoPager_Layout);
        SP = PreferenceUtil.getInstance(getApplicationContext());

        allPhotoMode = getIntent().getBooleanExtra(getString(R.string.all_photo_mode), false);
        all_photo_pos = getIntent().getIntExtra(getString(R.string.position), 0);  // 当前照片的位置
        size_all = getIntent().getIntExtra(getString(R.string.allMediaSize), getAlbum().getCount());  // 当前照片同组照片的总数

        // 回收站文件信息
        trashdis = getIntent().getBooleanExtra("trashbin", false);
        if (getIntent().hasExtra("trashdatalist")) {
            trashbinlistd = getIntent().getParcelableArrayListExtra("trashdatalist");
        }

        pathForDescription = getIntent().getStringExtra("path");

        try {
            Album album;
            Uri uri = getIntent().getData();
            if (Intent.ACTION_VIEW.equals(getIntent().getAction()) || ACTION_REVIEW.equals(getIntent().getAction()) || getIntent().getData() != null) {
                // 从其他应用传递过来的图片
                String path = FileUtils.getMediaPath(getApplicationContext(), getIntent().getData());
                pathForDescription = path;
                File file = null;
                if (path != null) {
                    file = new File(path);
                }

                boolean isFromOperateResult = getIntent().getBooleanExtra(EXTRA_PHOTO_IS_FROM_OPERATE_RESULT, false);
                if (file != null && file.isFile() && !isFromOperateResult) {
                    // 图片在本地路径上
                    album = new Album(getApplicationContext(), file);
                } else {
                    // 图片是一个uri（拍照、图片对齐、景深合成、曝光合成等操作的结果）
                    album = new Album(getApplicationContext(), getIntent().getData());
                    customUri = true;
                }
                getAlbums().addAlbum(0, album);
            }

            setUpSwitcherAnimation();
            initUI();
            setUpUI();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * startHandler stopHandler 用来控制进入或者是退出全屏模式
     */
    private void startHandler() {
        handler.postDelayed(runnable, 5000);
    }

    private void stopHandler() {
        handler.removeCallbacks(runnable);
    }

    private void toggleSystemUI() {
        if (fullScreenMode) {
            showSystemUI();
        } else {
            hideSystemUI();
        }
    }

    private void setupSystemUI() {
        toolbar.animate().translationY(Measure.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                .setDuration(0).start();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void showSystemUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toolbar.animate().translationY(Measure.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                        .setDuration(240).start();
                bottomBar.animate().translationY(0).setInterpolator(new DecelerateInterpolator())
                        .setDuration(240).start();

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                fullScreenMode = false;
                changeBackGroundColor();
            }
        });
    }

    /**
     * 照片全屏显示的时候，隐藏系统的UI信息
     */
    private void hideSystemUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // AccelerateInterpolator 在动画开始的地方速率改变比较慢，然后开始加速
                // -toolbar.getHeight() 起始点Y坐标最终停靠的位置
                toolbar.animate().translationY(-toolbar.getHeight()).setInterpolator(new AccelerateInterpolator())
                        .setDuration(200).start();
                bottomBar.animate().translationY(+bottomBar.getHeight()).setInterpolator(new AccelerateInterpolator())
                        .setDuration(200).start();

                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
                fullScreenMode = true;
                changeBackGroundColor();  // 切换背景色信息
                stopHandler();
            }
        });
    }

    /**
     * 设置照片的切换效果
     */
    private void setUpSwitcherAnimation() {
        Animation in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);

        viewSwitcher.setInAnimation(in);
        viewSwitcher.setOutAnimation(out);
    }

    /**
     * 设置照片背景界面的渐变色（在切换全屏的时候）
     */
    private void changeBackGroundColor() {
        int colorTo;
        int colorFrom;
        if (fullScreenMode) {
            // 进入全屏模式
            colorFrom = ThemeHelper.getBackgroundColor(getApplicationContext());
            colorTo = (ContextCompat.getColor(SingleMediaActivity.this, R.color.md_black_1000));
        } else {
            // 退出全屏模式
            colorFrom = (ContextCompat.getColor(SingleMediaActivity.this, R.color.md_black_1000));
            colorTo = ThemeHelper.getBackgroundColor(getApplicationContext());
        }
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(240);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ActivityBackground.setBackgroundColor((Integer)valueAnimator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }

    private void initUI() {

        final Menu bottomMenu = bottomBar.getMenu();
        getMenuInflater().inflate(R.menu.menu_bottom_view_pager, bottomMenu);

        if (trashdis) {
            // 如果是查看垃圾箱中的文件（只保留恢复和删除选项）
            bottomMenu.findItem(R.id.action_edit).setVisible(false);
            bottomMenu.findItem(R.id.action_share).setVisible(false);
            bottomMenu.findItem(R.id.restore_action).setVisible(true);
            bottomMenu.findItem(R.id.action_details).setVisible(false);
            bottomMenu.findItem(R.id.action_crop).setVisible(false);
        }

        // 将ActionMenuView的点击事件跟OptionMenuItem的一起绑定起来，然后一起处理点击事件
        for (int i = 0; i < bottomMenu.size(); i ++) {
            bottomMenu.getItem(i).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    stopHandler();
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        // 设置状态栏
        setSupportActionBar(toolbar);
        toolbar.bringToFront();
        toolbar.setNavigationIcon(ThemeHelper.getToolbarIcon(getApplicationContext(), CommunityMaterial.Icon.cmd_arrow_left));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        // 显示界面的toolbar
        setupSystemUI();

        // 设置显示照片RecycleView的界面
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false);
        mViewPager.setLayoutManager(linearLayoutManager);
        mViewPager.setHasFixedSize(true);
        mViewPager.setLongClickable(true);

        // 监听手机状态栏的变化来控制app导航栏是否显示
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // 状态栏处于可见状态，显示导航栏
                            showSystemUI();
                        } else {
                            // 状态栏属于隐藏状态，隐藏导航栏
                            hideSystemUI();
                        }
                    }
                });

        setUpViewPager();

        // https://cstsinghua.github.io/2018/03/26/Android%E5%B1%8F%E5%B9%95%E6%96%B9%E5%90%91/
        // 监听android 的横竖屏幕
        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        if (display.getRotation() == Surface.ROTATION_90) {
            // 当前已经处于横屏状态
            Configuration configuration = new Configuration();
            configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
            onConfigurationChanged(configuration);
        }
    }

    private void setUpUI() {
        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(
                ColorPalette.getTransparentColor(ThemeHelper.getPrimaryColor(getApplicationContext()), 255)
        );
        toolbar.setPopupTheme(ThemeHelper.getPopupToolbarStyle(getApplicationContext()));

        ActivityBackground.setBackgroundColor(ThemeHelper.getBackgroundColor(getApplicationContext()));

    }

    /**
     * 分享图片至外部应用
     */
    private void shareToOthers() {
        Uri uri = null;
        String name = null;
        String mediaPath = null;

        if (!allPhotoMode) {
            mediaPath = getAlbum().getCurrentMedia().getPath();
            name = getAlbum().getCurrentMedia().getName();
        } else {
            mediaPath = AlbumFragment.listAll.get(current_image_pos).getPath();
            name = AlbumFragment.listAll.get(current_image_pos).getName();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(getApplicationContext(),
                    BuildConfig.APPLICATION_ID + ".provider", new File(mediaPath));
        } else {
            uri = Uri.fromFile(new File(mediaPath));
        }

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, name);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/png");

        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_image)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 加载导航栏的菜单信息
        getMenuInflater().inflate(R.menu.menu_nav_view_pager, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                supportFinishAfterTransition();
                return true;
            case R.id.action_share:
                handler.removeCallbacks(slideShowRunnable);
                shareToOthers();
                return true;
            case R.id.action_slideshow:
                handler.removeCallbacks(slideShowRunnable);
                setSlideShowDialog();
                return true;
            case R.id.action_delete:
                handler.removeCallbacks(slideShowRunnable);
                deleteAction();
                return true;
            case R.id.action_details:
                handler.removeCallbacks(slideShowRunnable);
                Intent exifIntent = new Intent(SingleMediaActivity.this, PhotoExifDetailActivity.class);
                exifIntent.putExtra(EXTRA_ORI_IMG_PATH, pathForDescription);  // 设置原图的路径
                exifIntent.putExtra(EXTRA_IS_SAVED_CROP_RES, false);  // 说明不是经过裁剪之后的图片
                startActivity(exifIntent);
                return true;
            case R.id.action_crop:
                handler.removeCallbacks(slideShowRunnable);
                Intent cropIntent = new Intent(SingleMediaActivity.this, ImageCropActivity.class);
                cropIntent.putExtra(EXTRA_ORI_IMG_PATH, pathForDescription);
                startActivity(cropIntent);
                return true;
            case R.id.action_edit:
                imgEditResPath = FileUtils.generateImgEditResPath();  // 生成编辑后结果图片的位置
                EditImageActivity.start(this, pathForDescription, imgEditResPath, REQUEST_ACTION_EDITIMAGE);
                return true;
            case R.id.action_to_pdf:
                // 生成pdf文件
                new TransImgToPdfTask().execute();
                return true;
            case R.id.restore_action:
                // 从回收站恢复的菜单项信息
                AlertDialog.Builder restoreDialogBuilder = new AlertDialog.Builder(SingleMediaActivity.this, ThemeHelper.getDialogStyle());
                AlertDialogsHelper.getTextDialog(this, restoreDialogBuilder, R.string.restore, R.string.restore_image, null);
                restoreDialogBuilder.setNegativeButton(getString(R.string.cancel), null);
                restoreDialogBuilder.setPositiveButton(getString(R.string.restore), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        restoreImage(trashbinlistd.get(current_image_pos).getPath());
                    }
                });
                AlertDialog restoreDialog = restoreDialogBuilder.create();
                restoreDialog.show();
                AlertDialogsHelper.setButtonTextColor(new int[]{DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
                        ThemeHelper.getAccentColor(this), restoreDialog);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 从回收站恢复文件信息
     * @param path
     */
    private void restoreImage(String path) {
        Realm.init(this);
        Realm realm = Realm.getDefaultInstance();
        RealmResults<TrashBinRealmModel> results = realm.where(TrashBinRealmModel.class).equalTo("trashbinpath", path).findAll();
        String oldPath = results.get(0).getOldpath();
        String oldFolder = oldPath.substring(0, oldPath.lastIndexOf("/"));
        // 恢复到原来的文件夹
        if (restoreMove(context, results.get(0).getTrashbinpath(), oldFolder)) {
            // 从数据库中删除回收站的记录
            if (removeFromRealm(results.get(0).getTrashbinpath())) {
                deleteFromList(path);
                size_all = trashbinlistd.size();
                if (size_all > 0) {
                    adapter.notifyDataSetChanged();
                    getSupportActionBar().setTitle(current_image_pos + 1 + " / " + size_all);
                } else {
                    onBackPressed();
                }
            }
        }
    }

    private boolean restoreMove(Context context, String source, String targetDir) {
        File from = new File(source);
        File to = new File(targetDir);
        return FileUtils.moveFile(context, from, to);
    }


    private boolean removeFromRealm(String path) {
        boolean[] delete = {false};
        Realm.init(this);
        Realm realm = Realm.getDefaultInstance();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                RealmResults<TrashBinRealmModel> results = realm.where(TrashBinRealmModel.class).equalTo("trashbinpath", path).findAll();
                delete[0] = results.deleteAllFromRealm();
            }
        });
        return delete[0];
    }


    /**
     * 生成pdf文件的任务
     */
    private class TransImgToPdfTask extends AsyncTask<Void, Void, String> {
        private Dialog dialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = AlertDialogsHelper.getLoadingDialog(SingleMediaActivity.this, getResources().getString(R.string.loading), false);
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... voids) {
            String pdfPath = FileUtils.generateImgToPdf(SingleMediaActivity.this, pathForDescription);
            return pdfPath;
        }

        @Override
        protected void onPostExecute(String pdfPath) {
            super.onPostExecute(pdfPath);
            dialog.dismiss();
            // 开启pdf文件预览页面
            Intent intent = new Intent(SingleMediaActivity.this, PdfPreviewActivity.class);
            intent.setData(Uri.fromFile(new File(pdfPath)));
            intent.putExtra(EXTRA_PHOTO_TO_PDF_PATH, pdfPath);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_ACTION_EDITIMAGE:
                    // 图片编辑完成之后
                    handleImageAfterEditor(data);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 处理被编辑之后的照片
     * @param data
     */
    private void handleImageAfterEditor(Intent data) {
        String newFilePath = data.getStringExtra(EditImageActivity.EXTRA_FILE_OUTPUT);
        boolean isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false);
        if (isImageEdit){
            Toast.makeText(this, getString(R.string.save_path, newFilePath), Toast.LENGTH_LONG).show();
            // 扫描新的文件到MediaStore库
            FileUtils.updateMediaStore(this, new File(newFilePath), false,null);
        }else{//未编辑  还是用原来的图片
            newFilePath = data.getStringExtra(EditImageActivity.EXTRA_FILE_PATH);
        }
        // 接下来做显示图片的操作
    }

    @Override
    public void singleTap() {
        toggleSystemUI();
        // slideshow的情况
        if (slideshow) {
            handler.removeCallbacks(slideShowRunnable);
            slideshow = false;
            Toast.makeText(this, getString(R.string.slide_show_off), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void startPostponedTransition() {
        getWindow().setSharedElementEnterTransition(new ChangeBounds().setDuration(300));
        startPostponedEnterTransition();  // 启动共享元素动画
    }

    private void setUpViewPager() {
        // 控制显示UI界面的回调
        BasicCallBack basicCallBack = new BasicCallBack() {
            @Override
            public void callBack(int status, Object data) {
                toggleSystemUI();
            }
        };

        if (!allPhotoMode && !trashdis) {
            // 相册模式下点击某一个图片进入该界面
            adapter = new ImageAdapter(basicCallBack, getAlbum().getMedias(), this, this);
            getSupportActionBar().setTitle((getAlbum().getCurrentMediaIndex() + 1) + " / " + getAlbum().getMedias().size()); // 设置标题栏为当前图片下标
            mViewPager.setOnPageChangeListener(new PagerRecyclerView.OnPageChangeListener() {
                @Override
                public void onPageChanged(int oldPosition, int newPosition) {
                    getAlbum().setCurrentPhotoIndex(newPosition);
                    toolbar.setTitle((newPosition + 1) + " / " + getAlbum().getMedias().size());
                    invalidateOptionsMenu();
                    // 记录当前的图片文件路径
                    pathForDescription = getAlbum().getMedias().get(newPosition).getPath();
                }
            });
            // 设置RecyclerView滚动到当前的item下标
            mViewPager.scrollToPosition(getAlbum().getCurrentMediaIndex());
        } else if (allPhotoMode && !trashdis) {
            // 全部照片模式下，点击一个图片进入该界面
            adapter = new ImageAdapter(basicCallBack, AlbumFragment.listAll, this, this);
            getSupportActionBar().setTitle(all_photo_pos + " / " + size_all);
            current_image_pos = all_photo_pos;
            // OnPageChangeListener 是由OnScroller事件来触发的
            mViewPager.setOnPageChangeListener(new PagerRecyclerView.OnPageChangeListener() {
                @Override
                public void onPageChanged(int oldPosition, int newPosition) {
                    current_image_pos = newPosition;
                    getAlbum().setCurrentPhotoIndex(getAlbum().getCurrentMediaIndex());
                    toolbar.setTitle((newPosition + 1) + " / " + size_all);
                    invalidateOptionsMenu();
                    pathForDescription = AlbumFragment.listAll.get(newPosition).getPath();
                }
            });
            mViewPager.scrollToPosition(all_photo_pos);
        } else if (trashdis && !allPhotoMode) {
            // 显示回收站中的文件信息
            adapter = new ImageAdapter(basicCallBack, trashbinlistd, this, this);
            getSupportActionBar().setTitle(all_photo_pos + 1 + " / " + size_all);
            current_image_pos = all_photo_pos;
            mViewPager.setOnPageChangeListener(new PagerRecyclerView.OnPageChangeListener() {
                @Override
                public void onPageChanged(int oldPosition, int newPosition) {
                    current_image_pos = newPosition;
                    getAlbum().setCurrentPhotoIndex(getAlbum().getCurrentMediaIndex());
                    toolbar.setTitle(newPosition + 1 + " / " + size_all);
                    invalidateOptionsMenu();
                    pathForDescription = trashbinlistd.get(newPosition).getPath();
                }
            });
            mViewPager.scrollToPosition(current_image_pos);
        }
        mViewPager.setAdapter(adapter);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.setMargins(0,0, 0,0);
        } else {
            params.setMargins(0,0,0,0);
        }

        toolbar.setLayoutParams(params);
        setUpViewPager();
    }

    private Runnable slideShowRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (!allPhotoMode) {
                    // 相册模式
                    mViewPager.scrollToPosition((getAlbum().getCurrentMediaIndex() + 1) % getAlbum().getMedias().size());
                } else {
                    // 全部照片模式
                    mViewPager.scrollToPosition((current_image_pos + 1) % AlbumFragment.listAll.size());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (getAlbum().getCurrentMediaIndex() + 1 == getAlbum().getMedias().size() - 1) {
                    handler.removeCallbacks(slideShowRunnable);
                    slideshow = false;
                    toggleSystemUI();
                } else {
                    handler.postDelayed(this, SLIDE_SHOW_INTERVAL);
                }
            }
        }
    };

    /**
     * 设置幻灯片播放
     */
    private void setSlideShowDialog() {
        final AlertDialog.Builder slideshowDialog = new AlertDialog.Builder(SingleMediaActivity.this, R.style.AlertDialog_Light);
        final View SlideshowDialogLayout = getLayoutInflater().inflate(R.layout.dialog_slideshow, null);
        final TextView slideshowDialogTitle = (TextView) SlideshowDialogLayout.findViewById(R.id.slideshow_dialog_title);
        final CardView slideshowDialogCard = (CardView) SlideshowDialogLayout.findViewById(R.id.slideshow_dialog_card);
        final EditText editTextTimeInterval = (EditText) SlideshowDialogLayout.findViewById(R.id.slideshow_edittext);

        slideshowDialogTitle.setBackgroundColor(ThemeHelper.getPrimaryColor(this));
        slideshowDialogCard.setBackgroundColor(ThemeHelper.getCardBackgroundColor(this));
        editTextTimeInterval.getBackground().mutate().setColorFilter(ThemeHelper.getTextColor(this), PorterDuff.Mode.SRC_ATOP);
        editTextTimeInterval.setTextColor(ThemeHelper.getTextColor(this));
        editTextTimeInterval.setHintTextColor(ThemeHelper.getSubTextColor(this));

        slideshowDialog.setView(SlideshowDialogLayout);
        AlertDialog dialog = slideshowDialog.create();

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok_action), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String value = editTextTimeInterval.getText().toString();
                if (!"".equals(value)) {
                    slideshow = true;
                    int intValue = Integer.valueOf(value);
                    SLIDE_SHOW_INTERVAL = intValue * 1000;
                    if (SLIDE_SHOW_INTERVAL > 1000) {
                        hideSystemUI();
                        Toast.makeText(SingleMediaActivity.this, getString(R.string.slide_show_on), Toast.LENGTH_SHORT).show();
                        handler.postDelayed(slideShowRunnable, SLIDE_SHOW_INTERVAL);
                    } else {
                        Toast.makeText(SingleMediaActivity.this, "Minimum duration is 2 sec", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        AlertDialogsHelper.setButtonTextColor(new int[]{DialogInterface.BUTTON_POSITIVE}, ThemeHelper.getAccentColor(this), dialog);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(slideShowRunnable);
        if (customUri || trashdis) {
            getAlbums().deleteAlbumByIndexSoft(0);  // 删除供拍照使用的临时相册
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        handler.removeCallbacks(slideShowRunnable);
        if (customUri || trashdis) {
            getAlbums().deleteAlbumByIndexSoft(0);  // 删除供拍照使用的临时相册
        }
    }

    private void deleteAction() {
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(SingleMediaActivity.this, R.style.AlertDialog_Light);
        if (trashdis) {
            AlertDialogsHelper.getTextDialog(this, deleteDialog, R.string.delete, R.string.delete_image_bin, null);
        } else {
            AlertDialogsHelper.getTextCheckboxDialog(SingleMediaActivity.this,
                    deleteDialog,
                    R.string.delete,
                    R.string.delete_photo_message,
                    null,
                    "移至回收站",
                    ThemeHelper.getAccentColor(this));
        }
        String buttonDelete = getString(R.string.delete);
        deleteDialog.setNegativeButton(getString(R.string.cancel), null);
        deleteDialog.setPositiveButton(buttonDelete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteCurrentMedia();
            }
        });

        AlertDialog alertDialog = deleteDialog.create();
        alertDialog.show();
        AlertDialogsHelper.setButtonTextColor(new int[]{DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE},
                ThemeHelper.getAccentColor(this),
                alertDialog);
    }

    /**
     * 将文件加入回收站
     * @return
     */
    private boolean addToTrash(){
        String pathOld = null;
        String oldpath = null;
        int no = 0;
        boolean succ = false;
        if(!allPhotoMode){
            // 某一个相册下的照片
            oldpath = getAlbum().getCurrentMedia().getPath();
        } else {
            // 全部照片模式下的照片信息
            oldpath = AlbumFragment.listAll.get(current_image_pos).getPath();
        }
        File file = new File(Environment.getExternalStorageDirectory() + "/" + ".nomedia");

        if (file.exists() && file.isDirectory()) {
            if (!allPhotoMode) {
                pathOld = getAlbum().getCurrentMedia().getPath();
                succ = getAlbum().moveCurrentMedia(getApplicationContext(), file.getAbsolutePath());
            } else {
                // 全部照片模式
                pathOld = AlbumFragment.listAll.get(current_image_pos).getPath();
                succ = getAlbum().moveAnyMedia(getApplicationContext(), file.getAbsolutePath(), AlbumFragment.listAll.get
                        (current_image_pos).getPath());
            }
            if (succ) {
                Snackbar snackbar = SnackBarHandler.showWithBottomMargin2(parentView, getString(R.string
                                .trashbin_move_onefile),
                        bottomBar.getHeight(), Snackbar.LENGTH_SHORT);
                final String finalOldpath = oldpath;
                snackbar.setAction("撤销", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        removeTrashObjectFromRealm(finalOldpath);
                        getAlbum().moveAnyMedia(getApplicationContext(), getAlbum().getPath(), finalOldpath);
                    }
                });
                snackbar.show();
            } else {
                SnackBarHandler.showWithBottomMargin(parentView, String.valueOf(no) + " " + getString(R.string
                                .trashbin_move_error),
                        bottomBar.getHeight());
            }
        } else {
            if (file.mkdir()) {
                if (!allPhotoMode) {
                    pathOld = getAlbum().getCurrentMedia().getPath();
                    succ = getAlbum().moveCurrentMedia(getApplicationContext(), file.getAbsolutePath());
                } else {
                    pathOld = getAlbum().getCurrentMedia().getPath();
                    succ = getAlbum().moveAnyMedia(getApplicationContext(), file.getAbsolutePath(), AlbumFragment.listAll.get
                            (current_image_pos).getPath());
                }
                if (succ) {
                    SnackBarHandler.showWithBottomMargin(parentView, String.valueOf(no) + " " + getString(R.string
                                    .trashbin_move_onefile), bottomBar.getHeight());
                } else {
                    SnackBarHandler.showWithBottomMargin(parentView, String.valueOf(no) + " " + getString(R.string
                                    .trashbin_move_error),
                            bottomBar.getHeight());
                }
            }
        }
        addTrashObjectsToRealm(pathOld);
        return succ;
    }

    /**
     * 将被删除的图片写入 realm 数据库
     */
    private void addTrashObjectsToRealm(String mediaPath) {
        String trashbinpath = Environment.getExternalStorageDirectory() + "/" + ".nomedia";
        Realm.init(getContext());
        realm = Realm.getDefaultInstance();

        int index = mediaPath.lastIndexOf("/");
        String name = mediaPath.substring(index + 1);
        String trashpath = trashbinpath + "/" + name;

        // 首先检查当前文件是否已经存在与垃圾箱中（否则会出现主键冲突）
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                TrashBinRealmModel model = realm.where(TrashBinRealmModel.class)
                        .equalTo("trashbinpath", trashpath).findFirst();
                if (model != null) {
                    model.deleteFromRealm();
                }
            }
        });

        realm.beginTransaction();
        TrashBinRealmModel trashBinRealmModel = realm.createObject(TrashBinRealmModel.class, trashpath);
        trashBinRealmModel.setOldpath(mediaPath);
        trashBinRealmModel.setDatetime(new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));
        trashBinRealmModel.setTimeperiod("null");
        realm.commitTransaction();
    }

    /**
     * 删除回收站中的照片信息
     */
    private void removeTrashObjectFromRealm(String mediaPath) {
        String trashbinpath = Environment.getExternalStorageDirectory() + "/" + ".nomedia";
        Realm.init(getContext());
        realm = Realm.getDefaultInstance();

        int index = mediaPath.lastIndexOf("/");
        String name = mediaPath.substring(index + 1);
        String trashpath = trashbinpath + "/" + name;

        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                TrashBinRealmModel model = realm.where(TrashBinRealmModel.class)
                        .equalTo("trashbinpath", trashpath).findFirst();
                if (model != null) {
                    model.deleteFromRealm();
                }
            }
        });
    }


    private void deleteCurrentMedia() {
        boolean success = false;
        if (!allPhotoMode && !trashdis) {
            // 某一个相册下显示的图片信息
            if (AlertDialogsHelper.check) {
                success = addToTrash();  // 删除至回收站的操作
            } else {
                success = getAlbum().deleteCurrentMedia(getApplicationContext());
            }

            if (!success) {
                // 申请SD卡权限
                requestSdCardPermissions();
            }

            if (getAlbum().getMedias().size() == 0) {
                if (customUri) {
                    getAlbums().deleteAlbumByIndexSoft(0);  // 删除供拍照使用的临时相册
                    finish();
                } else {
                    getAlbums().removeCurrentAlbum();
                    finish();
                }
            }
            adapter.notifyDataSetChanged();
            getSupportActionBar().setTitle((getAlbum().getCurrentMediaIndex() + 1) + " / " + getAlbum().getMedias().size());
        } else if (allPhotoMode && !trashdis) {
            // 以全部的模式显示照片
            int c = all_photo_pos;
            if (AlertDialogsHelper.check) {
                success = addToTrash();  // 收入回收站
            } else {
                deleteMedia(AlbumFragment.listAll.get(all_photo_pos).getPath());
                success = true;
            }

            if (success) {
                AlbumFragment.listAll.remove(all_photo_pos);
                size_all = AlbumFragment.listAll.size();
                adapter.notifyDataSetChanged();
            }

            if (current_image_pos != size_all) {
                getSupportActionBar().setTitle((c + 1) + " / " + size_all);
            }
        } else if (trashdis && !allPhotoMode) {
            // 垃圾回收站的模式
            int c = current_image_pos;
            Realm.init(this);
            Realm realm = Realm.getDefaultInstance();
            realm.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    RealmResults<TrashBinRealmModel> results = realm.where(TrashBinRealmModel.class)
                            .equalTo("trashbinpath", trashbinlistd.get(current_image_pos).getPath()).findAll();
                    results.deleteAllFromRealm();
                }
            });
            deleteFromList(trashbinlistd.get(current_image_pos).getPath());
            size_all = trashbinlistd.size();
            if (size_all > 0) {
                adapter.notifyDataSetChanged();
                getSupportActionBar().setTitle(c + 1 + " / " + size_all);
            } else {
                onBackPressed();
            }
        }
    }

    /**
     * 从对应的图片列表中删除图片文件信息
     * @param path
     */
    private void deleteFromList(String path) {
        if (trashdis) {
            // 从回收站删除文件
            for(int i = 0; i < trashbinlistd.size(); i ++) {
                if (trashbinlistd.get(i).getPath().equals(path)) {
                    trashbinlistd.remove(i);
                    break;
                }
            }
        }
    }

    private Disposable requestSdCardPermissions() {
        Disposable disposable = new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    if (granted) { // Always true pre-M
                        // I can control the camera now
                        SnackBarHandler.show(bottomBar, "权限申请成功", Snackbar.LENGTH_SHORT);
                    } else {
                        // Oups permission denied
                        SnackBarHandler.show(bottomBar, "权限申请失败", Snackbar.LENGTH_SHORT);
                    }
                });
        return disposable;
    }

    private void deleteMedia(String path) {
        String[] projection = {MediaStore.Images.Media._ID};

        // Match on the file path
        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{path};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
        }
        c.close();
    }
}
