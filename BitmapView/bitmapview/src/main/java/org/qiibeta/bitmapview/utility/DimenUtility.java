package org.qiibeta.bitmapview.utility;


import android.content.Context;
import android.content.res.Resources;

public class DimenUtility {
    private static int sScreenWidth = -1;
    private static int sScreenHeight = -1;

    public static int getScreenWidth() {
        if (sScreenWidth == -1) {
            sScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        }
        return sScreenWidth;
    }

    public static int getScreenHeight() {
        if (sScreenHeight == -1) {
            sScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        }
        return sScreenHeight;
    }

	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
//
//	public static int px2dip(float pxValue) {
//		final float scale = GalleryApplication.getInstance().getResources().getDisplayMetrics().density;
//		return (int) (pxValue / scale + 0.5f);
//	}

    public static int bytes2MB(long bytes) {
        return (int) (bytes / 1024 / 1024);
    }
}
