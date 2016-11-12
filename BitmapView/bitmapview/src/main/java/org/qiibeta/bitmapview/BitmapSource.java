package org.qiibeta.bitmapview;


import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import org.qiibeta.bitmapview.image.AppImage;
import org.qiibeta.bitmapview.image.BitmapImage;
import org.qiibeta.bitmapview.image.GifImage;
import org.qiibeta.bitmapview.image.TileImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public abstract class BitmapSource {
    private static final String FILE_SCHEME = "file";

    public static BitmapSource newInstance(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
        String scheme = uri.getScheme();
        if (FILE_SCHEME.equals(scheme)) {
            return new FileSource(uri, thumbnailOrientation, thumbnailBitmap);
        } else {
            return new InputStreamSource(uri, thumbnailOrientation, thumbnailBitmap);
        }
    }

    public static BitmapSource newInstance(Bitmap thumbnailBitmap) {
        return newInstance(0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(int thumbnailOrientation, Bitmap thumbnailBitmap) {
        return new SimpleBitmapSource(thumbnailOrientation, thumbnailBitmap);
    }

    public static BitmapSource newInstance(Bitmap thumbnailBitmap, String filePath) {
        return newInstance(0, thumbnailBitmap, filePath);
    }

    public static BitmapSource newInstance(int thumbnailOrientation, Bitmap thumbnailBitmap, String filePath) {
        return new FileSource(Uri.fromFile(new File(filePath)), thumbnailOrientation, thumbnailBitmap);
    }

    public static BitmapSource newInstance(Uri uri, Bitmap thumbnailBitmap) {
        return newInstance(uri, 0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(Uri uri, int orientation, Bitmap thumbnailBitmap, InputStream fullPictureInputStream) {
        return newInstance(uri, orientation, thumbnailBitmap, orientation, fullPictureInputStream);
    }

    public static BitmapSource newInstance(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap, int fullPictureOrientation, InputStream fullPictureInputStream) {
        return new InputStreamSource(uri, thumbnailOrientation, thumbnailBitmap, fullPictureOrientation, fullPictureInputStream);
    }

    public abstract Uri getUri();

    public abstract AppImage getBitmapImage();

    public abstract TileImage getTileImage();

    private static class SimpleBitmapSource extends BitmapSource {
        private int mThumbnailOrientation;
        private Bitmap mThumbnailBitmap;

        public SimpleBitmapSource(int thumbnailOrientation, Bitmap bitmap) {
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = bitmap;
        }

        @Override
        public Uri getUri() {
            return null;
        }

        @Override
        public AppImage getBitmapImage() {
            return BitmapImage.newInstance(mThumbnailOrientation, mThumbnailBitmap);
        }

        @Override
        public TileImage getTileImage() {
            return null;
        }
    }

    //todo 支持gif 支持方向 获取contentResolver
    private static class InputStreamSource extends BitmapSource {
        private Uri mUri;
        private int mThumbnailOrientation;
        private int mFullOrientation;
        private Bitmap mThumbnailBitmap;
        private InputStream mFullPictureInputStream;

        private InputStreamSource(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
            ContentResolver contentResolver = null;
            try {
                this.mFullPictureInputStream = contentResolver.openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            this.mFullOrientation = thumbnailOrientation;
            this.mUri = uri;
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = thumbnailBitmap;
        }

        private InputStreamSource(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap, int fullPictureOrientation, InputStream fullPictureInputStream) {
            this.mUri = uri;
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = thumbnailBitmap;
            this.mFullOrientation = fullPictureOrientation;
            this.mFullPictureInputStream = fullPictureInputStream;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public AppImage getBitmapImage() {
            return BitmapImage.newInstance(mThumbnailOrientation, mThumbnailBitmap);
        }

        public TileImage getTileImage() {
            if (this.mFullPictureInputStream != null) {
                return TileImage.newInstance(mFullOrientation, mFullPictureInputStream);
            } else {
                return null;
            }
        }
    }

    private static class FileSource extends BitmapSource {
        private Uri mUri;
        private int mThumbnailOrientation;
        private int mFullOrientation;
        private Bitmap mThumbnailBitmap;
        private String mFilePath;
        private boolean mIsGif;

        private FileSource(Uri fileUri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
            super();
            this.mFilePath = fileUri.getPath();
            if (ImageFormatUtility.isGif(this.mFilePath)) {
                File file = new File(this.mFilePath);
                this.mIsGif = file.length() <= 4 * 1024 * 1024;//only support 4mb gif
            } else {
                this.mFullOrientation = OrientationInfoUtility.getOrientation(this.mFilePath);
            }

            this.mUri = fileUri;
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = thumbnailBitmap;
        }

        private FileSource(Uri fileUri, int thumbnailOrientation, Bitmap thumbnailBitmap, int fullPictureOrientation) {
            super();
            this.mFilePath = fileUri.getPath();
            if (ImageFormatUtility.isGif(this.mFilePath)) {
                File file = new File(this.mFilePath);
                this.mIsGif = file.length() <= 4 * 1024 * 1024;//only support 4mb gif
                this.mFullOrientation = 0;
            } else {
                this.mFullOrientation = fullPictureOrientation;
            }
            this.mUri = fileUri;
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = thumbnailBitmap;
        }

        public Uri getUri() {
            return this.mUri;
        }

        public AppImage getBitmapImage() {
            return mIsGif ? GifImage.newInstance(mThumbnailBitmap, this.mFilePath) : BitmapImage.newInstance(mThumbnailOrientation, mThumbnailBitmap);
        }

        public TileImage getTileImage() {
            if (ImageFormatUtility.isGif(mFilePath)) {
                return null;
            }
            return TileImage.newInstance(mFullOrientation, mFilePath);
        }
    }
}
