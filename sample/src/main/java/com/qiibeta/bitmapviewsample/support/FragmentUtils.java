package com.qiibeta.bitmapviewsample.support;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;


public class FragmentUtils {
    public static void commit(Activity activity, FragmentManager manager, FragmentTransaction transaction) {
        if (activity.isFinishing()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed()) {
            return;
        }
        transaction.commitAllowingStateLoss();
        manager.executePendingTransactions();
    }
}
