package com.qiibeta.bitmapviewsample.support;

import android.content.res.Resources;

import com.qiibeta.bitmapviewsample.GalleryApplication;

public class DimenUtility {
	private static int sScreenWidth = -1;

	public static int getScreenWidth() {
		if (sScreenWidth == -1) {
			sScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
		}
		return sScreenWidth;
	}

	public static int dip2px(float dpValue) {
		final float scale = GalleryApplication.getInstance().getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
}
