package com.xinlan.imageeditlibrary.editimage.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

/**
 * 禁用ViewPager滑动事件
 * 
 * @author panyi
 * xujian 2018/10/27
 * 
 */


public class CustomViewPager extends ViewPager {
	// 设置是否响应滑动事件的标志为false
	private boolean isCanScroll = false;

	public CustomViewPager(Context context) {
		super(context);
	}

	@Override
	public void setCurrentItem(int item, boolean smoothScroll) {
		isCanScroll = true;
		super.setCurrentItem(item, smoothScroll);
		isCanScroll = false;
	}

	@Override
	public void setCurrentItem(int item) {
		setCurrentItem(item, false);
	}

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScanScroll(boolean isCanScroll) {
		this.isCanScroll = isCanScroll;
	}

	@Override
	public void scrollTo(int x, int y) {
		// 控制是否响应滑动事件
		if (isCanScroll) {
			super.scrollTo(x, y);
		}
	}
}






