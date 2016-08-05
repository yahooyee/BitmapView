package com.qiibeta.bitmapviewsample;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.qiibeta.bitmapviewsample.support.FragmentUtils;
import com.qiibeta.bitmapviewsample.ui.BaseFragment;
import com.qiibeta.bitmapviewsample.ui.TimeListFragment;

public class ContainerActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 12345;
    private BaseFragment mTimeListFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);


        setContentView(R.layout.activity_container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        displayFrontPage();

        getWindow().setBackgroundDrawable(null);
    }

    public void showStatsBar(boolean show) {
        if (show) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        } else {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY; //sticky模式下,就算出现状态栏,稍后就又消失了
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    private void displayFrontPage() {
        final FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        mTimeListFragment = (BaseFragment) fragmentManager.findFragmentByTag(TimeListFragment.class.getName());
        if (mTimeListFragment == null) {
            mTimeListFragment = TimeListFragment.newInstance();
            transaction.add(R.id.fragment_container, mTimeListFragment, TimeListFragment.class.getName());
        }
        FragmentUtils.commit(this, fragmentManager, transaction);
    }

    public void pushFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();

        BaseFragment currentFragment = mTimeListFragment;
        if (count >= 1) {
            String currentTopFragmentName = fragmentManager.getBackStackEntryAt(count - 1).getName();
            currentFragment = (BaseFragment) fragmentManager.findFragmentByTag(currentTopFragmentName);
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        String tag = fragment.getClass().getName();
        transaction.add(R.id.fragment_container, fragment, tag).addToBackStack(tag);
        FragmentUtils.commit(this, fragmentManager, transaction);

        if (currentFragment != null) {
            currentFragment.getView().setVisibility(View.INVISIBLE);
            currentFragment.setUserVisibleHint(false);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        int count = fragmentManager.getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();
            return;
        }

        String currentTopFragmentName = fragmentManager.getBackStackEntryAt(count - 1).getName();
        String previousTopFragmentName = count >= 2 ? fragmentManager.getBackStackEntryAt(count - 1 - 1).getName() : null;
        final BaseFragment currentFragment = (BaseFragment) fragmentManager.findFragmentByTag(currentTopFragmentName);

        if (currentFragment.onBackPressed()) {
            return;
        }
        BaseFragment previousFragment = (BaseFragment) (previousTopFragmentName != null ? fragmentManager.findFragmentByTag(previousTopFragmentName) : this.mTimeListFragment);
        previousFragment.getView().setVisibility(View.VISIBLE);
        previousFragment.setUserVisibleHint(true);

        ContainerActivity.super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    displayFrontPage();
                } else {
                    Toast.makeText(ContainerActivity.this, "Please give permission to read storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}
