package com.qiibeta.bitmapviewsample.model;


import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.text.format.DateUtils;

import com.qiibeta.bitmapviewsample.GalleryApplication;
import com.qiibeta.bitmapviewsample.support.OrientationInfoUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import static android.text.format.DateUtils.FORMAT_SHOW_DATE;
import static android.text.format.DateUtils.FORMAT_SHOW_WEEKDAY;
import static android.text.format.DateUtils.FORMAT_SHOW_YEAR;

public class SystemAlbumDataSource {
    public static interface AppDataTimeSourceCallback {
        public void onQuery(Uri dirUri, ArrayList<AppFile> files);
    }

    public static final Uri ROOT_URI = Uri.parse("file:///");

    private static SystemAlbumDataSource sInstance = new SystemAlbumDataSource();

    private HashMap<Uri, ArrayList<AppDataTimeSourceCallback>> mTimeCallbacksMap = new HashMap<>();

    private ArrayList<AppFile> mTimeAlbumFiles = new ArrayList<>();

    private Handler mHandler;
    private BitmapFactory.Options mOptions;

    private SystemAlbumDataSource() {
        this.mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean handleMessage(Message msg) {
                ArrayList<AppFile> files = (ArrayList<AppFile>) msg.obj;
                switch (msg.what) {
                    case 0:
                        break;
                    case 1:
                        mTimeAlbumFiles.clear();
                        mTimeAlbumFiles.addAll(files);
                        refreshToTimeAlbumCallbacks(mTimeAlbumFiles);
                        break;
                }
                return true;
            }
        });
        mOptions = new BitmapFactory.Options();
        mOptions.inJustDecodeBounds = true;
        GalleryApplication.getInstance().getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, mContentObserver);
    }

    public static SystemAlbumDataSource getInstance() {
        return sInstance;
    }


    public void addCallback(Uri uri, AppDataTimeSourceCallback callback) {
        ArrayList<AppDataTimeSourceCallback> callbacks = this.mTimeCallbacksMap.get(uri);
        if (callbacks == null) {
            callbacks = new ArrayList<>();
            this.mTimeCallbacksMap.put(uri, callbacks);
        }
        callbacks.add(callback);
    }

    public void removeCallback(Uri uri, AppDataTimeSourceCallback callback) {
        ArrayList<AppDataTimeSourceCallback> callbacks = this.mTimeCallbacksMap.get(uri);
        if (callbacks != null) {
            callbacks.remove(callback);
        }
    }

    private void refreshToTimeAlbumCallbacks(ArrayList<AppFile> files) {
        ArrayList<AppDataTimeSourceCallback> rootCallbacks = mTimeCallbacksMap.get(ROOT_URI);
        if (rootCallbacks != null) {
            for (AppDataTimeSourceCallback callback : rootCallbacks) {
                callback.onQuery(ROOT_URI, files);
            }
        }

        for (AppFile file : files) {
            ArrayList<AppDataTimeSourceCallback> callbacks = mTimeCallbacksMap.get(file.getUri());
            if (callbacks != null) {
                for (AppDataTimeSourceCallback callback : callbacks) {
                    callback.onQuery(file.getUri(), file.getChildren());
                }
            }
        }
    }

    public void queryDirectory(Uri uri) {
        if (mTimeAlbumFiles.size() > 0) {
            refreshToTimeAlbumCallbacks(mTimeAlbumFiles);
        }
        GalleryApplication.getInstance().submitBackgroundTask(new Runnable() {
            @Override
            public void run() {
                String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_MODIFIED};

                Cursor cursor = GalleryApplication.getInstance().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                        null, MediaStore.Images.Media._ID + " desc");
                if (cursor == null) {
                    return;
                }

                int count = cursor.getCount();
                int imageColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                int imageTitleIndex = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);
                int imagePathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int imageTimeIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED);

                HashMap<File, ArrayList<AppFile>> albumMap = new HashMap<>();

                if (cursor.moveToFirst()) {
                    do {
                        String path = cursor.getString(imagePathIndex);
                        BitmapFactory.decodeFile(path, mOptions);
                        if (mOptions.outWidth > 0 && mOptions.outHeight > 0) {
                            long modifiedTimestamp = cursor.getLong(imageTimeIndex);
                            File file = new File(path);
                            File parent = file.getParentFile();

                            ArrayList<AppFile> children = albumMap.get(parent);
                            if (children == null) {
                                children = new ArrayList<>();
                                albumMap.put(parent, children);
                            }

                            final int degree = OrientationInfoUtility.getOrientation(path);
                            int fixWidth = mOptions.outWidth;
                            int fixHeight = mOptions.outHeight;
                            if (degree == 90 || degree == 270) {
                                fixWidth = mOptions.outHeight;
                                fixHeight = mOptions.outWidth;
                            }

                            AppFile appFile = AppFile.newInstance(file, fixWidth, fixHeight);
                            appFile.setModifiedTimestamp(modifiedTimestamp * 1000);
                            children.add(appFile);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();

                ArrayList<AppFile> albumList = new ArrayList<>();
                Set<File> keys = albumMap.keySet();
                for (File file : keys) {
                    AppFile folder = AppFile.newInstance(file, 0, 0);
                    ArrayList<AppFile> children = albumMap.get(file);
                    for (AppFile child : children) {
                        folder.addChildrenFile(child);
                    }
                    albumList.add(folder);
                }

                ArrayList<AppFile> timeAlbumList = sortByTime(new ArrayList<AppFile>(albumList));
                mHandler.obtainMessage(1, timeAlbumList).sendToTarget();
            }
        });
    }

    private ArrayList<AppFile> sortByTime(ArrayList<AppFile> albumList) {
        ArrayList<AppFile> allFiles = new ArrayList<>();
        for (AppFile folder : albumList) {
            allFiles.addAll(folder.getChildren());
        }

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);

        HashMap<String, ArrayList<AppFile>> hashMap = new HashMap<>();
        for (AppFile file : allFiles) {
            calendar.setTimeInMillis(file.getModifiedTimestamp());

            int fileYear = calendar.get(Calendar.YEAR);

            String title = null;
            if (fileYear != year) {
                title = DateUtils.formatDateTime(GalleryApplication.getInstance(), file.getModifiedTimestamp(), FORMAT_SHOW_WEEKDAY | FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR);
            } else {
                title = DateUtils.formatDateTime(GalleryApplication.getInstance(), file.getModifiedTimestamp(), FORMAT_SHOW_WEEKDAY | FORMAT_SHOW_DATE);
            }

            ArrayList<AppFile> timeAlbum = hashMap.get(title);
            if (timeAlbum == null) {
                timeAlbum = new ArrayList<>();
                hashMap.put(title, timeAlbum);
            }
            timeAlbum.add(file);
        }

        ArrayList<AppFile> timeAlbumList = new ArrayList<>();
        Set<String> keySet = hashMap.keySet();
        for (String key : keySet) {
            AppFile file = AppFile.newInstance(key);
            file.addChildrenFiles(hashMap.get(key));
            file.setModifiedTimestamp(file.getChild(0).getModifiedTimestamp());
            timeAlbumList.add(file);
        }
        Collections.sort(timeAlbumList, sTimeComparator);
        return timeAlbumList;
    }

    private static Comparator<AppFile> sTimeComparator = new Comparator<AppFile>() {
        @Override
        public int compare(AppFile left, AppFile right) {
            long leftModifiedTimestamp = left.getModifiedTimestamp();
            long rightModifiedTimestamp = right.getModifiedTimestamp();
            if (leftModifiedTimestamp > rightModifiedTimestamp) {
                return -1;
            } else if (leftModifiedTimestamp < rightModifiedTimestamp) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    private final ContentObserver mContentObserver = new ContentObserver(mHandler) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            //empty
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            SystemAlbumDataSource.getInstance().queryDirectory(SystemAlbumDataSource.ROOT_URI);
        }
    };
}
