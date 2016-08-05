package com.qiibeta.bitmapviewsample.ui;


import android.app.Activity;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import com.qiibeta.bitmapviewsample.BuildConfig;
import com.qiibeta.bitmapviewsample.model.AppFile;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoader;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoaderOption;
import com.qiibeta.bitmapviewsample.support.mrc.BitmapMRC;

import org.qiibeta.bitmapview.BitmapSource;
import org.qiibeta.bitmapview.GestureBitmapView;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryItemHolder> {
    public static interface OnItemClickListener {
        public void onClick(AppFile file);
    }

    private static int sScreenWidth;
    private static int sScreenHeight;

    private Activity mActivity;
    private AppFile mFile;
    private GalleryAdapter.OnItemClickListener mOnItemClickListener;
    private RecyclerView mRecyclerView;

    public GalleryAdapter(Activity activity, RecyclerView recyclerView, GalleryAdapter.OnItemClickListener onItemClickListener) {
        mActivity = activity;
        mRecyclerView = recyclerView;
        mOnItemClickListener = onItemClickListener;

        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        sScreenWidth = metrics.widthPixels;
        sScreenHeight = metrics.heightPixels;
    }

    public void setFile(AppFile file) {
        this.mFile = file;
        this.notifyDataSetChanged();
    }

    @Override
    public GalleryItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = new GestureBitmapView(mActivity);
        itemView.setLayoutParams(new RecyclerView.LayoutParams(sScreenWidth, sScreenHeight));
        return new GalleryItemHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final GalleryItemHolder holder, int position) {
        AppFile file = mFile.getChild(position);
        final Uri uri = file.getUri();

        final GestureBitmapView imageView = holder.mImageView;
        imageView.setDebug(BuildConfig.DEBUG);

        if (uri.equals(imageView.getTag())) {
            return;
        }
        imageView.setTag(uri);
        imageView.setBitmapSource(null);

        ImageLoader.getInstance().loadImage(uri, sScreenWidth, sScreenHeight, ImageLoaderOption.SCALE_TYPE_FIT, new ImageLoader.Callback() {
            @Override
            public void onGetImageSize(int imageWidth, int imageHeight) {
                if (uri.equals(imageView.getTag()))
                    imageView.setBackgroundSize(imageWidth, imageHeight);
            }

            @Override
            public void onSuccess(final Uri uri, final BitmapMRC bitmap, boolean timeConsumingUnder15ms) {
                if (uri.equals(imageView.getTag())) {
                    bitmap.retain();
                    imageView.setBitmapSource(BitmapSource.newInstance(uri, bitmap.getBitmap()));
                }
            }

            @Override
            public void onFailure(Uri uri, Exception exception) {

            }
        });

        imageView.setOnClickListener(new GestureBitmapView.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnClickListener.onClick(imageView);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mFile != null ? mFile.getChildrenCount() : 0;
    }

    @Override
    public long getItemId(int position) {
        return mFile.getChild(position).getId();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = mRecyclerView.getLayoutManager().getPosition(v);
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onClick(mFile.getChild(position));
            }
        }
    };

    static class GalleryItemHolder extends RecyclerView.ViewHolder {
        public GestureBitmapView mImageView;

        public GalleryItemHolder(View itemView) {
            super(itemView);
            mImageView = (GestureBitmapView) itemView;
        }
    }
}

