package com.qiibeta.bitmapviewsample.ui;


import android.app.Activity;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.qiibeta.bitmapviewsample.R;
import com.qiibeta.bitmapviewsample.model.AppFile;
import com.qiibeta.bitmapviewsample.support.Constants;
import com.qiibeta.bitmapviewsample.support.DimenUtility;
import com.qiibeta.bitmapviewsample.support.ViewUtility;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoader;
import com.qiibeta.bitmapviewsample.support.imageloader.ImageLoaderOption;
import com.qiibeta.bitmapviewsample.support.mrc.BitmapMRC;
import com.qiibeta.bitmapviewsample.support.FolderImageView;

import java.util.ArrayList;

public class TimeListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static interface OnItemClickListener {
        public void onClick(AppFile file, int index);
    }

    private static final int ITEM_VIEW_TYPE_CATEGORY = 0;
    private static final int ITEM_VIEW_TYPE_NORMAL = 1;

    private Activity mActivity;
    private ArrayList<AppFile> mFileList;
    private TimeListAdapter.OnItemClickListener mOnItemClickListener;
    private RecyclerView mRecyclerView;
    private int mItemSize;

    public TimeListAdapter(Activity activity, RecyclerView recyclerView, TimeListAdapter.OnItemClickListener onItemClickListener) {
        mActivity = activity;
        mRecyclerView = recyclerView;
        mOnItemClickListener = onItemClickListener;
        mItemSize = (DimenUtility.getScreenWidth() - DimenUtility.dip2px(2) * (Constants.COLUMN_COUNT + 1)) / Constants.COLUMN_COUNT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_VIEW_TYPE_NORMAL) {
            View itemView = ViewUtility.getInflater(mActivity).inflate(R.layout.item_picture, parent, false);
            itemView.setOnClickListener(mOnClickListener);
            return new TimeListAdapter.FolderHolder(itemView);
        } else if (viewType == ITEM_VIEW_TYPE_CATEGORY) {
            View itemView = ViewUtility.getInflater(mActivity).inflate(R.layout.item_category, parent, false);
            return new TimeListAdapter.CategoryHolder(itemView);
        }
        return null;
    }


    @Override
    public void onViewRecycled(RecyclerView.ViewHolder viewHolder) {
        super.onViewRecycled(viewHolder);
        if (viewHolder instanceof FolderHolder) {
            final FolderHolder holder = (FolderHolder) viewHolder;
            Object tag = holder.mImageView.getTag();
            if (tag == null) {
                return;
            }
            Uri uri = (Uri) tag;
            ImageLoader.getInstance().cancelLoadImage(uri, mItemSize, mItemSize, ImageLoaderOption.SCALE_TYPE_CROP);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        AppFile file = mFileList.get(position);

        if (viewHolder instanceof FolderHolder) {
            final FolderHolder holder = (FolderHolder) viewHolder;
            final Uri uri = file.getUri();

            Object tag = holder.mImageView.getTag();
            if (tag == uri) {
                return;
            }
            holder.mImageView.setTag(uri);
            holder.mImageView.setImageBitmap(null);
            holder.mImageView.setBackgroundColor(0xFFDDDDDD);

            ImageLoader.getInstance().loadImage(uri, mItemSize, mItemSize, ImageLoaderOption.SCALE_TYPE_CROP, new ImageLoader.Callback() {
                @Override
                public void onGetImageSize(int imageWidth, int imageHeight) {

                }

                @Override
                public void onSuccess(Uri uri, BitmapMRC bitmap, boolean timeConsumingUnder15ms) {
                    Object tag = holder.mImageView.getTag();
                    if (tag != uri) {
                        return;
                    }
                    holder.mImageView.setImageBitmap(bitmap);
                }

                @Override
                public void onFailure(Uri uri, Exception exception) {

                }
            });
        } else {
            CategoryHolder categoryHolder = (CategoryHolder) viewHolder;
            categoryHolder.mTitleView.setText(file.getTitle());
        }
    }

    @Override
    public int getItemCount() {
        return mFileList != null ? mFileList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return isCategory(position) ? ITEM_VIEW_TYPE_CATEGORY : ITEM_VIEW_TYPE_NORMAL;
    }

    @Override
    public long getItemId(int position) {
        return this.mFileList.get(position).getId();
    }

    public boolean isCategory(int position) {
        return this.mFileList.get(position).isFolder();
    }

    public boolean replaceFiles(ArrayList<AppFile> fileList) {
        ArrayList<AppFile> tmp = new ArrayList<>();
        for (AppFile file : fileList) {
            tmp.add(file);
            tmp.addAll(file.getChildren());
        }

        if (mFileList == null) {
            mFileList = tmp;
            return true;
        } else {
            if (mFileList.equals(tmp)) {
                return false;
            }

            mFileList.clear();
            mFileList.addAll(tmp);
            return true;
        }
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = mRecyclerView.getLayoutManager().getPosition(v);
            AppFile selectedFile = mFileList.get(position);
            if (mOnItemClickListener != null) {
                AppFile file = AppFile.newInstance("Time");

                for (int i = 0; i < getItemCount(); i++) {
                    if (!isCategory(i)) {
                        file.addChildrenFile(mFileList.get(i));
                    }
                }
                mOnItemClickListener.onClick(file, file.getChildren().indexOf(selectedFile));
            }
        }
    };

    static class FolderHolder extends RecyclerView.ViewHolder {
        public FolderImageView mImageView;

        public FolderHolder(View itemView) {
            super(itemView);
            mImageView = ViewUtility.viewById(itemView, R.id.image_view);
        }
    }

    static class CategoryHolder extends RecyclerView.ViewHolder {
        public TextView mTitleView;

        public CategoryHolder(View itemView) {
            super(itemView);
            mTitleView = ViewUtility.viewById(itemView, R.id.tv_category);
        }
    }
}

