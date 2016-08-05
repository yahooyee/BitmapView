package com.qiibeta.bitmapviewsample.ui;


import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import com.qiibeta.bitmapviewsample.ContainerActivity;
import com.qiibeta.bitmapviewsample.R;
import com.qiibeta.bitmapviewsample.model.AppFile;
import com.qiibeta.bitmapviewsample.model.SystemAlbumDataSource;
import com.qiibeta.bitmapviewsample.support.Constants;
import com.qiibeta.bitmapviewsample.support.ViewUtility;

import java.util.ArrayList;

public class TimeListFragment extends BaseFragment {
    public static TimeListFragment newInstance() {
        TimeListFragment fragment = new TimeListFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private TimeListAdapter mAlbumListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_time_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitleTextColor(Color.BLACK);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(), mRecyclerView.getPaddingRight(), ViewUtility.getNavigationBarHeight());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mToolbar.setTitle("BitmapView sample");

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), Constants.COLUMN_COUNT);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return mAlbumListAdapter.isCategory(position) ? Constants.COLUMN_COUNT : 1;
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
        mAlbumListAdapter = new TimeListAdapter(getActivity(), mRecyclerView, mOnItemClickListener);
        mAlbumListAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAlbumListAdapter);

        SystemAlbumDataSource.getInstance().addCallback(SystemAlbumDataSource.ROOT_URI, mAppDataSourceCallback);
        SystemAlbumDataSource.getInstance().queryDirectory(SystemAlbumDataSource.ROOT_URI);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        SystemAlbumDataSource.getInstance().removeCallback(SystemAlbumDataSource.ROOT_URI, mAppDataSourceCallback);
    }

    private TimeListAdapter.OnItemClickListener mOnItemClickListener = new TimeListAdapter.OnItemClickListener() {
        @Override
        public void onClick(AppFile file, int index) {
            ContainerActivity activity = (ContainerActivity) getActivity();
            activity.pushFragment(GalleryFragment.newInstance(file, index));
        }
    };

    private SystemAlbumDataSource.AppDataTimeSourceCallback mAppDataSourceCallback = new SystemAlbumDataSource.AppDataTimeSourceCallback() {
        @Override
        public void onQuery(Uri dirUri, ArrayList<AppFile> files) {
            if (SystemAlbumDataSource.ROOT_URI.equals(dirUri) && mAlbumListAdapter.replaceFiles(files)) {
                mAlbumListAdapter.notifyDataSetChanged();
            }
        }
    };
}
