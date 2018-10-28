package com.xinlan.imageeditlibrary.editimage.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import com.xinlan.imageeditlibrary.R;
import com.xinlan.imageeditlibrary.editimage.utils.RectUtil;


/**
 * @author panyi
 * xujian 2018/10/17
 */
public class StickerItem {
    private static final float MIN_SCALE = 0.15f;
    private static final int HELP_BOX_PAD = 25;

    private static final int BUTTON_WIDTH = Constants.STICKER_BTN_HALF_SIZE;

    public Bitmap bitmap;
    public Rect srcRect;// 原始图片坐标
    public RectF dstRect;// 绘制目标坐标
    private Rect helpToolsRect;  // 帮助按钮图标的矩形范围
    public RectF deleteRect;// 删除按钮位置
    public RectF rotateRect;// 旋转按钮位置

    public RectF helpBox;  // 最终图片边界的矩形区域（比绘制的图片的区域要大一些）
    public Matrix matrix;// 变化矩阵
    public float roatetAngle = 0;  // 记录当前图标被旋转的角度信息
    boolean isDrawHelpTool = false;
    private Paint dstPaint = new Paint();
    private Paint paint = new Paint();
    private Paint helpBoxPaint = new Paint();

    private float initWidth;// 加入屏幕时原始宽度

    private static Bitmap deleteBit; // （左上角）删除按钮
    private static Bitmap rotateBit;  // （右下角）旋转按钮

    private Paint debugPaint = new Paint();
    public RectF detectRotateRect;

    public RectF detectDeleteRect;

    public StickerItem(Context context) {

        // 四周黑色边框的画笔
        helpBoxPaint.setColor(Color.BLACK);
        helpBoxPaint.setStyle(Style.STROKE);
        helpBoxPaint.setAntiAlias(true);
        helpBoxPaint.setStrokeWidth(4);

        dstPaint = new Paint();
        dstPaint.setColor(Color.RED);
        dstPaint.setAlpha(120);

        debugPaint = new Paint();
        debugPaint.setColor(Color.GREEN);
        debugPaint.setAlpha(120);

        // 导入工具按钮位图
        if (deleteBit == null) {
            deleteBit = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.sticker_delete);
        }// end if
        if (rotateBit == null) {
            rotateBit = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.sticker_rotate);
        }// end if
    }

    public void init(Bitmap addBit, View parentView) {
        this.bitmap = addBit;
        this.srcRect = new Rect(0, 0, addBit.getWidth(), addBit.getHeight());
        // 计算addBit 和 其父View的长宽比信息，使父View能容纳得下图片
        int bitWidth = Math.min(addBit.getWidth(), parentView.getWidth() >> 1);  // addBit缩放之后的长宽信息
        int bitHeight = (int) bitWidth * addBit.getHeight() / addBit.getWidth();
        // 准备居中显示缩放后的addBit
        int left = (parentView.getWidth() >> 1) - (bitWidth >> 1);
        int top = (parentView.getHeight() >> 1) - (bitHeight >> 1);
        // 缩放后的addBit所占据的矩形信息
        this.dstRect = new RectF(left, top, left + bitWidth, top + bitHeight);
        this.matrix = new Matrix();
        this.matrix.postTranslate(this.dstRect.left, this.dstRect.top);  // 平移到左上角位置
        this.matrix.postScale((float) bitWidth / addBit.getWidth(),
                (float) bitHeight / addBit.getHeight(), this.dstRect.left,
                this.dstRect.top);  // 设置缩放（以left top为原始点）
        initWidth = this.dstRect.width();// 记录原始宽度
        // item.matrix.setScale((float)bitWidth/addBit.getWidth(),
        // (float)bitHeight/addBit.getHeight());
        this.isDrawHelpTool = true;
        this.helpBox = new RectF(this.dstRect);
        updateHelpBoxRect();

        helpToolsRect = new Rect(0, 0, deleteBit.getWidth(),
                deleteBit.getHeight());

        deleteRect = new RectF(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH, helpBox.left + BUTTON_WIDTH, helpBox.top
                + BUTTON_WIDTH);
        rotateRect = new RectF(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH, helpBox.right + BUTTON_WIDTH, helpBox.bottom
                + BUTTON_WIDTH);

        // 用于检测用户的点击是否落在了该delete rotate 按钮上
        detectRotateRect = new RectF(rotateRect);
        detectDeleteRect = new RectF(deleteRect);
    }

    /**
     * 作为操作的边界，需要比原始图片的范围大一些
     */
    private void updateHelpBoxRect() {
        this.helpBox.left -= HELP_BOX_PAD;
        this.helpBox.right += HELP_BOX_PAD;
        this.helpBox.top -= HELP_BOX_PAD;
        this.helpBox.bottom += HELP_BOX_PAD;
    }

    /**
     * 位置更新
     *
     * @param dx
     * @param dy
     */
    public void updatePos(final float dx, final float dy) {
        this.matrix.postTranslate(dx, dy);// 记录到矩阵中

        dstRect.offset(dx, dy);

        // 工具按钮随之移动
        helpBox.offset(dx, dy);
        deleteRect.offset(dx, dy);
        rotateRect.offset(dx, dy);

        this.detectRotateRect.offset(dx, dy);
        this.detectDeleteRect.offset(dx, dy);
    }

    /**
     * 旋转 缩放 更新
     *
     * @param dx
     * @param dy
     */
    public void updateRotateAndScale(final float oldx, final float oldy,
                                     final float dx, final float dy) {
        float c_x = dstRect.centerX();
        float c_y = dstRect.centerY();

        float x = this.detectRotateRect.centerX();
        float y = this.detectRotateRect.centerY();

        // float x = oldx;
        // float y = oldy;

        float n_x = x + dx;  // 新的旋转按钮的pos
        float n_y = y + dy;

        float xa = x - c_x;  // 旧的旋转按钮距中心的距离
        float ya = y - c_y;

        float xb = n_x - c_x;  // 新的旋转按钮距中心的距离
        float yb = n_y - c_y;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        // System.out.println("srcLen--->" + srcLen + "   curLen---->" +
        // curLen);

        float scale = curLen / srcLen;// 计算缩放比

        float newWidth = dstRect.width() * scale;
        if (newWidth / initWidth < MIN_SCALE) {// 最小缩放值检测
            return;
        }

        this.matrix.postScale(scale, scale, this.dstRect.centerX(),
                this.dstRect.centerY());// 存入scale矩阵「将缩放信息存入变换矩阵」
        // this.matrix.postRotate(5, this.dstRect.centerX(),
        // this.dstRect.centerY());
        RectUtil.scaleRect(this.dstRect, scale);// 缩放目标矩形

        // 重新计算工具箱坐标
        helpBox.set(dstRect);
        updateHelpBoxRect();// 重新计算
        rotateRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);
        deleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        detectRotateRect.offsetTo(helpBox.right - BUTTON_WIDTH, helpBox.bottom
                - BUTTON_WIDTH);
        detectDeleteRect.offsetTo(helpBox.left - BUTTON_WIDTH, helpBox.top
                - BUTTON_WIDTH);

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        float angle = (float) Math.toDegrees(Math.acos(cos));
        // System.out.println("angle--->" + angle);

        // 定理
        float calMatrix = xa * yb - xb * ya;// 行列式计算 确定转动方向

        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        // System.out.println("angle--->" + angle);
        roatetAngle += angle;
        this.matrix.postRotate(angle, this.dstRect.centerX(),
                this.dstRect.centerY());

        RectUtil.rotateRect(this.detectRotateRect, this.dstRect.centerX(),
                this.dstRect.centerY(), roatetAngle);
        RectUtil.rotateRect(this.detectDeleteRect, this.dstRect.centerX(),
                this.dstRect.centerY(), roatetAngle);
        // System.out.println("angle----->" + angle + "   " + flag);

        // System.out
        // .println(srcLen + "     " + curLen + "    scale--->" + scale);

    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap(this.bitmap, this.matrix, null);// 贴图元素绘制
        // canvas.drawRect(this.dstRect, dstPaint);

        if (this.isDrawHelpTool) {// 绘制辅助工具线
            canvas.save();
            canvas.rotate(roatetAngle, helpBox.centerX(), helpBox.centerY());  // 设置helpbox的旋转信息
            canvas.drawRoundRect(helpBox, 10, 10, helpBoxPaint); // 绘制边缘的矩形框框信息
            // 绘制工具按钮  deleteBit bitmap 图片信息；helpToolsRect 图标大小rect信息；deleteRect 图标的具体显示位置信息
            canvas.drawBitmap(deleteBit, helpToolsRect, deleteRect, null);
            canvas.drawBitmap(rotateBit, helpToolsRect, rotateRect, null);
            canvas.restore();
            // canvas.drawRect(deleteRect, dstPaint);
            // canvas.drawRect(rotateRect, dstPaint);

            //debug
//             canvas.drawRect(detectRotateRect, debugPaint);
//             canvas.drawRect(detectDeleteRect, debugPaint);
//             canvas.drawRect(helpBox , debugPaint);
        }// end if

        // detectRotateRect
    }
}// end class
