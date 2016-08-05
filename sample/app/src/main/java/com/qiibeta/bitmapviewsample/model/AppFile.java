package com.qiibeta.bitmapviewsample.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.File;
import java.util.ArrayList;

public class AppFile implements Parcelable {
    protected AppFile(Parcel in) {
        mId = in.readLong();
        mUri = in.readParcelable(Uri.class.getClassLoader());
        mChildrenCount = in.readInt();
        mTitle = in.readString();
        isFolder = in.readByte() != 0;
        mWidth = in.readInt();
        mHeight = in.readInt();
        mModifiedTimestamp=in.readLong();
        mChildrenFiles = in.createTypedArrayList(AppFile.CREATOR);
    }

    public static final Creator<AppFile> CREATOR = new Creator<AppFile>() {
        @Override
        public AppFile createFromParcel(Parcel in) {
            return new AppFile(in);
        }

        @Override
        public AppFile[] newArray(int size) {
            return new AppFile[size];
        }
    };

    public static AppFile newInstance(File file, int width, int height) {
        AppFile appFile = new AppFile();
        appFile.setId(file.getAbsolutePath().hashCode());
        appFile.setUri(Uri.fromFile(file));
        appFile.setTitle(file.getName());
        appFile.setFolder(file.isDirectory());
        if (!file.isDirectory()) {
            appFile.setWidth(width);
            appFile.setHeight(height);
        }
        return appFile;
    }

    public static AppFile newInstance(String title) {
        AppFile appFile = new AppFile();
        appFile.setId(title.hashCode());
        appFile.setUri(Uri.parse(title));
        appFile.setTitle(title);
        appFile.setFolder(true);
        return appFile;
    }

    private long mId;
    private Uri mUri;
    private int mChildrenCount;
    private String mTitle;
    private int mWidth;
    private int mHeight;
    private long mModifiedTimestamp;

    private boolean isFolder;
    private ArrayList<AppFile> mChildrenFiles = new ArrayList<>();

    private AppFile() {

    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public Uri getUri() {
        return mUri;
    }

    public void setUri(Uri uri) {
        mUri = uri;
    }

    public int getChildrenCount() {
        return mChildrenCount;
    }

    public void setChildrenCount(int childrenCount) {
        mChildrenCount = childrenCount;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public int getWidth() {
        return mWidth;
    }

    public void setWidth(int width) {
        this.mWidth = width;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setHeight(int height) {
        this.mHeight = height;
    }

    public long getModifiedTimestamp() {
        return mModifiedTimestamp;
    }

    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.mModifiedTimestamp = modifiedTimestamp;
    }

    public void addChildrenFile(AppFile appFile) {
        this.mChildrenFiles.add(appFile);
        this.mChildrenCount = this.mChildrenFiles.size();
    }

    public void addChildrenFiles(ArrayList<AppFile> appFiles) {
        this.mChildrenFiles.addAll(appFiles);
        this.mChildrenCount = this.mChildrenFiles.size();
    }

    public boolean removeChild(AppFile childFile) {
        boolean result = this.mChildrenFiles.remove(childFile);
        this.mChildrenCount = this.mChildrenFiles.size();
        return result;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public AppFile getChild(int position) {
        return this.mChildrenFiles.get(position);
    }

    public ArrayList<AppFile> getChildren() {
        return this.mChildrenFiles;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeParcelable(mUri, flags);
        dest.writeInt(mChildrenCount);
        dest.writeString(mTitle);
        dest.writeByte((byte) (isFolder ? 1 : 0));
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeLong(mModifiedTimestamp);
        dest.writeTypedList(mChildrenFiles);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AppFile)) {
            return false;
        }

        AppFile other = (AppFile) o;
        boolean equal = this.mUri.equals(other.mUri);
        if (!equal) {
            return false;
        }

        return !isFolder || mChildrenFiles.equals(other.mChildrenFiles);
    }

    @Override
    public int hashCode() {
        if (!isFolder) {
            return mUri.hashCode();
        }
        return mChildrenFiles.hashCode();
    }
}
