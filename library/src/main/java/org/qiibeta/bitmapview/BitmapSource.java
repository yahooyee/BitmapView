package org.qiibeta.bitmapview;


import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.net.Uri;

import org.qiibeta.bitmapview.image.AppImage;
import org.qiibeta.bitmapview.image.BitmapImage;
import org.qiibeta.bitmapview.image.GifImage;
import org.qiibeta.bitmapview.image.TileImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.qiibeta.bitmapview.OrientationInfoUtility.ORIENTATION_ROTATE_0;

public abstract class BitmapSource {
    public static BitmapSource newInstance(Context context, Uri uri, @OrientationInfoUtility.ORIENTATION_ROTATE int thumbnailOrientation, Bitmap thumbnailBitmap) {
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("BitmapView cant open network url");
        }
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            return new FileSource(uri, thumbnailOrientation, thumbnailBitmap);
        } else {
            //todo
            //read orientation info need to create byte array object
            return new InputStreamSource(context.getApplicationContext(), uri, thumbnailOrientation, thumbnailBitmap);
        }
    }

    public static BitmapSource newInstance(Context context, Uri uri, Bitmap thumbnailBitmap) {
        return newInstance(context.getApplicationContext(), uri, ORIENTATION_ROTATE_0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(Bitmap thumbnailBitmap) {
        return newInstance(ORIENTATION_ROTATE_0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(@OrientationInfoUtility.ORIENTATION_ROTATE int thumbnailOrientation, Bitmap thumbnailBitmap) {
        return new SimpleBitmapSource(thumbnailOrientation, thumbnailBitmap);
    }

    public static BitmapSource newInstance(String filePath, Bitmap thumbnailBitmap) {
        return newInstance(filePath, ORIENTATION_ROTATE_0, thumbnailBitmap);
    }

    public static BitmapSource newInstance(String filePath, @OrientationInfoUtility.ORIENTATION_ROTATE int thumbnailOrientation, Bitmap thumbnailBitmap) {
        return new FileSource(Uri.fromFile(new File(filePath)), thumbnailOrientation, thumbnailBitmap);
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

    private static class InputStreamSource extends BitmapSource {
        private Context mContext;
        private Uri mUri;
        private int mThumbnailOrientation;
        private Bitmap mThumbnailBitmap;
        private boolean mIsGif;

        private InputStreamSource(Context context, Uri uri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
            this.mContext = context;
            this.mUri = uri;
            this.mThumbnailOrientation = thumbnailOrientation;
            this.mThumbnailBitmap = thumbnailBitmap;

            InputStream inputStream = getInputStream();
            if (ImageFormatUtility.isGif(inputStream) && inputStream != null) {
                try {
                    this.mIsGif = inputStream.available() <= 4 * 1024 * 1024;//only support 4mb gif
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            closeSilently(inputStream);
        }

        public Uri getUri() {
            return this.mUri;
        }

        public AppImage getBitmapImage() {
            if (mIsGif) {
                InputStream inputStream = getInputStream();
                AppImage image = GifImage.newInstance(mThumbnailBitmap, inputStream);
                closeSilently(inputStream);
                return image;
            } else {
                return BitmapImage.newInstance(mThumbnailOrientation, mThumbnailBitmap);
            }
        }

        private InputStream getInputStream() {
            ContentResolver contentResolver = this.mContext.getContentResolver();
            try {
                return contentResolver.openInputStream(this.mUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        public TileImage getTileImage() {
            InputStream inputStream = getInputStream();
            if (inputStream != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                closeSilently(inputStream);
                int width = options.outWidth;
                int height = options.outHeight;

                if (width <= 0 || height <= 0) {
                    return null;
                }

                inputStream = getInputStream();
                try {
                    BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(inputStream, false);
                    closeSilently(inputStream);
                    inputStream = getInputStream();
                    int orientation = OrientationInfoUtility.getOrientation(inputStream);
                    closeSilently(inputStream);
                    return TileImage.newInstance(width, height, orientation, bitmapRegionDecoder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    private static class FileSource extends BitmapSource {
        private Uri mUri;
        private int mThumbnailOrientation;
        private Bitmap mThumbnailBitmap;
        private String mFilePath;
        private boolean mIsGif;

        private FileSource(Uri fileUri, int thumbnailOrientation, Bitmap thumbnailBitmap) {
            super();
            this.mFilePath = fileUri.getPath();
            if (ImageFormatUtility.isGif(this.mFilePath)) {
                File file = new File(this.mFilePath);
                this.mIsGif = file.length() <= 4 * 1024 * 1024;//only support 4mb gif
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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(this.mFilePath, options);
            int width = options.outWidth;
            int height = options.outHeight;

            if (width <= 0 || height <= 0) {
                return null;
            }

            try {
                BitmapRegionDecoder bitmapRegionDecoder = BitmapRegionDecoder.newInstance(this.mFilePath, false);
                return TileImage.newInstance(width, height, OrientationInfoUtility.getOrientation(this.mFilePath), bitmapRegionDecoder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static void closeSilently(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception ignored) {

            }
        }
    }
}
