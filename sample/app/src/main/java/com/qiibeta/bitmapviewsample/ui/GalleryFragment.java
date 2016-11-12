package com.qiibeta.bitmapviewsample.ui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.qiibeta.bitmapviewsample.BuildConfig;
import com.qiibeta.bitmapviewsample.ContainerActivity;
import com.qiibeta.bitmapviewsample.R;
import com.qiibeta.bitmapviewsample.model.AppFile;
import com.qiibeta.bitmapviewsample.support.FixViewPager;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoader;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoaderOption;
import com.qiibeta.bitmapviewsample.support.mrc.BitmapMRC;

import org.qiibeta.bitmapview.BitmapSource;
import org.qiibeta.bitmapview.GestureBitmapView;


public class GalleryFragment extends BaseFragment {
    private static final String EXTRA_FILE = "file";
    private static final String EXTRA_POSITION = "position";

    public static GalleryFragment newInstance(AppFile file, int position) {
        GalleryFragment fragment = new GalleryFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA_FILE, file);
        bundle.putInt(EXTRA_POSITION, position);
        fragment.setArguments(bundle);
        return fragment;
    }

    private static int sScreenWidth;
    private static int sScreenHeight;

    private Toolbar mToolbar;
    private FixViewPager mGalleryViewPager;
    private AppFile mFile;
    private int mPosition;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        sScreenWidth = metrics.widthPixels;
        sScreenHeight = metrics.heightPixels;

        mGalleryViewPager = (FixViewPager) getView().findViewById(R.id.view_pager);

        Bundle bundle = getArguments();
        mFile = bundle.getParcelable(EXTRA_FILE);
        mPosition = bundle.getInt(EXTRA_POSITION, 0);

        mGalleryViewPager.addOnPageChangeListener(new FixViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mToolbar.setTitle(mFile.getChild(position).getTitle());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mGalleryViewPager.setAdapter(new GalleryAdapter());
        mGalleryViewPager.setCurrentItem(mPosition);
        mToolbar.setTitle(mFile.getChild(mPosition).getTitle());
        mToolbar.setTitleTextColor(Color.BLACK);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });
    }

    private void animateShowOrHideBackground() {
        int fromColor;
        int toColor;
        if (mToolbar.getVisibility() == View.VISIBLE) {
            mToolbar.animate().translationY(-mToolbar.getHeight()).alpha(0.0f).withLayer().withEndAction(new Runnable() {
                @Override
                public void run() {
                    mToolbar.setVisibility(View.INVISIBLE);
                }
            });
            fromColor = Color.WHITE;
            toColor = Color.BLACK;

            ((ContainerActivity) getActivity()).showStatsBar(false);
        } else {
            mToolbar.animate().translationY(0).alpha(1.0f).withLayer().withStartAction(new Runnable() {
                @Override
                public void run() {
                    mToolbar.setVisibility(View.VISIBLE);
                }
            });
            fromColor = Color.BLACK;
            toColor = Color.WHITE;

            ((ContainerActivity) getActivity()).showStatsBar(true);
        }

        ObjectAnimator anim = ObjectAnimator.ofArgb(mGalleryViewPager,
                "backgroundColor", fromColor,
                toColor);
        anim.setAutoCancel(true);
        anim.start();
    }

    @Override
    public boolean onBackPressed() {
        if (mToolbar.getVisibility() != View.VISIBLE) {
            animateShowOrHideBackground();
            return true;
        }
        return super.onBackPressed();
    }

    private class GalleryAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final GestureBitmapView imageView = new GestureBitmapView(getActivity());
            imageView.setDebug(BuildConfig.DEBUG);
            AppFile file = mFile.getChild(position);
            final Uri uri = file.getUri();
            imageView.setTag(uri);

            imageView.setBackgroundSize(file.getWidth(), file.getHeight());
            ImageLoader.getInstance().loadImage(uri, sScreenWidth, sScreenHeight, ImageLoaderOption.SCALE_TYPE_FIT, new ImageLoader.Callback() {
                @Override
                public void onGetImageSize(int imageWidth, int imageHeight) {

                }

                @Override
                public void onSuccess(final Uri uri, final BitmapMRC bitmap, boolean timeConsumingUnder15ms) {
                    if (uri.equals(imageView.getTag())) {
                        bitmap.retain();
                        Activity activity = getActivity();
                        if (activity == null) {
                            return;
                        }
                        imageView.setBitmapSource(BitmapSource.newInstance(activity, uri, bitmap.getBitmap()));
                    }
                }

                @Override
                public void onFailure(Uri uri, Exception exception) {

                }
            });

            imageView.setOnClickListener(new GestureBitmapView.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animateShowOrHideBackground();
                }
            });
            container.addView(imageView);
            return imageView;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return mFile.getChildrenCount();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }
}
