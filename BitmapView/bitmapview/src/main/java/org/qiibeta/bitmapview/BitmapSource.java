package org.qiibeta.bitmapview;


import android.graphics.Bitmap;
import android.net.Uri;

import org.qiibeta.bitmapview.image.AppImage;
import org.qiibeta.bitmapview.image.BitmapImage;
import org.qiibeta.bitmapview.image.GifImage;
import org.qiibeta.bitmapview.image.TileImage;

import java.io.File;
import java.io.InputStream;

public class BitmapSource {

    public static BitmapSource newInstance(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
        BitmapSource source = new BitmapSource();

        String path = uri.getPath();
        if (ImageFormatUtility.isGif(path)) {
            File file = new File(path);
            source.mIsGif = file.length() <= 4 * 1024 * 1024;//only support 4mb gif
        } else {
            source.mFullOrientation = OrientationInfoUtility.getOrientation(path);
            source.mFilePath = path;
        }

        source.uri = uri;
        source.mThumbnailOrientation = thumbnailOrientation;
        source.mThumbnailBitmap = thumbnailBitmap;
        return source;
    }

    public static BitmapSource newInstance(Uri uri, Bitmap thumbnailBitmap) {
        return newInstance(uri, 0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(Uri uri, int orientation, Bitmap thumbnailBitmap, InputStream fullPictureInputStream) {
        return newInstance(uri, orientation, thumbnailBitmap, orientation, fullPictureInputStream);
    }

    public static BitmapSource newInstance(Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap, int fullPictureOrientation, InputStream fullPictureInputStream) {
        BitmapSource source = new BitmapSource();
        source.uri = uri;
        source.mThumbnailOrientation = thumbnailOrientation;
        source.mFullOrientation = fullPictureOrientation;
        source.mThumbnailBitmap = thumbnailBitmap;
        source.mFullPictureInputStream = fullPictureInputStream;
        return source;
    }

    private Uri uri;
    private int mThumbnailOrientation;
    private int mFullOrientation;
    private Bitmap mThumbnailBitmap;
    private InputStream mFullPictureInputStream;
    private String mFilePath;
    private boolean mIsGif;

    private BitmapSource() {
        //empty
    }

    Uri getUri() {
        return this.uri;
    }

    AppImage getBitmapImage() {
        return mIsGif ? GifImage.newInstance(mThumbnailBitmap, uri.getPath()) : BitmapImage.newInstance(mThumbnailOrientation, mThumbnailBitmap);
    }

    TileImage getTileImage() {
        if (mFullPictureInputStream != null) {
            return TileImage.newInstance(mFullOrientation, mFullPictureInputStream);
        } else if (mFilePath != null) {
            if (ImageFormatUtility.isGif(mFilePath)) {
                return null;
            }
            return TileImage.newInstance(mFullOrientation, mFilePath);
        } else {
            return null;
        }
    }
}
