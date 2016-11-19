package com.qiibeta.bitmapviewsample.support;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;

import com.qiibeta.bitmapviewsample.GalleryApplication;

public class ViewUtility {
    public static LayoutInflater getInflater(Activity activity) {
        LayoutInflater inflate = (LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflate;
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public static <T extends View> T viewById(View view, int id) {
        return (T) view.findViewById(id);
    }

    @SuppressWarnings({"unchecked", "UnusedDeclaration"})
    public static <T extends View> T viewById(Activity activity, int id) {
        return (T) activity.findViewById(id);
    }

    public static int getNavigationBarHeight() {
        boolean hasMenuKey = ViewConfiguration.get(GalleryApplication.getInstance()).hasPermanentMenuKey();
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
        int resourceId = Resources.getSystem().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0 && !hasMenuKey && !hasBackKey) {
            return Resources.getSystem().getDimensionPixelSize(resourceId);
        }
        return 0;
    }
}
