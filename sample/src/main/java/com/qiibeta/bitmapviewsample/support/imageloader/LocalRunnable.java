package com.qiibeta.bitmapviewsample.support.imageloader;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.qiibeta.bitmapviewsample.support.Constants;
import com.qiibeta.bitmapviewsample.support.OrientationInfoUtility;

import java.io.IOException;
import java.util.ArrayList;

public class LocalRunnable implements Runnable {
    public static interface Callback {
        public void onResult(LocalRunnable runnable, Bitmap bitmap);
    }

    private static final Object sDecodeLock = new Object();

    private Uri mUri;
    private Callback mCallback;
    private ArrayList<ImageLoaderOption> mOptions = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mIsCanceled;

    public LocalRunnable(Uri uri, Callback callback) {
        this.mUri = uri;
        this.mCallback = callback;
    }

    public void addOption(ImageLoaderOption option) {
        this.mOptions.add(option);
    }

    public ArrayList<ImageLoaderOption> getOptions() {
        return this.mOptions;
    }

    public void cancel() {
        this.mIsCanceled = true;
    }

    private boolean isCanceled() {
        return this.mIsCanceled;
    }

    @Override
    public void run() {
        if (this.mIsCanceled) {
            Log.d("LocalRunnable", "Task is canceled, skip");
            return;
        }
        String cacheKey = ImageLoader.getKey(mUri, mOptions.get(0));

        BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
        factoryOptions.inJustDecodeBounds = true;
        String path = this.mUri.getPath();
        BitmapFactory.decodeFile(path, factoryOptions);

        final int imageWidth = factoryOptions.outWidth;
        final int imageHeight = factoryOptions.outHeight;

        if (imageWidth <= 0 || imageHeight <= 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onResult(LocalRunnable.this, null);
                }
            });
            return;
        }

        final int degree = OrientationInfoUtility.getOrientation(path);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int backgroundWidth = imageWidth;
                int backgroundHeight = imageHeight;
                if (degree == 90 || degree == 270) {
                    backgroundWidth = imageHeight;
                    backgroundHeight = imageWidth;
                }
                for (ImageLoaderOption option : mOptions) {
                    if (option.getCallback() != null)
                        option.getCallback().onGetImageSize(backgroundWidth, backgroundHeight);
                }
            }
        });

        int requireWidth = mOptions.get(0).getWidth();
        int requireHeight = mOptions.get(0).getHeight();

        if (degree == 90 || degree == 270) {
            requireWidth = mOptions.get(0).getHeight();
            requireHeight = mOptions.get(0).getWidth();
        }
        int scaleType = mOptions.get(0).getScaleType();

        Bitmap bitmap = null;
        if (scaleType == ImageLoaderOption.SCALE_TYPE_CROP) {
            //如果是大图,区域裁剪
            if (imageWidth >= requireWidth && imageHeight >= requireHeight) {
                int cropSampleSize = calculateInSampleCropSize(factoryOptions, requireWidth, requireHeight);
                factoryOptions.inSampleSize = cropSampleSize;
                try {
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(path, false);
                    int regionWidth = requireWidth * cropSampleSize;
                    int regionHeight = requireHeight * cropSampleSize;

                    int x = (imageWidth - regionWidth) / 2;
                    int y = (imageHeight - regionHeight) / 2;
                    bitmap = decodeRegion(decoder, new Rect(x, y, x + regionWidth, y + regionHeight), factoryOptions);
                } catch (IOException e) {
                    e.printStackTrace();
                    //万一是gif,或者jpg但是没法用BitmapRegionDecoder（这种图片很多的）
                    //todo 万一图片高宽比太大,生成的缩略图太大怎么办
                    factoryOptions.inSampleSize = calculateInSampleCropSize(factoryOptions, requireWidth, requireHeight);
                    factoryOptions.inJustDecodeBounds = false;
                    factoryOptions.inMutable = true;
                    bitmap = decodeFile(path, factoryOptions);
                    bitmap = cropBitmapIfNeeded(bitmap, requireWidth, requireHeight);//切掉多余的
                }
            } else {
                //万一太小,先用fit,然后再根据高宽比,裁剪去掉多余部分
                factoryOptions.inSampleSize = calculateInSampleFitSize(factoryOptions, requireWidth, requireHeight);
                factoryOptions.inJustDecodeBounds = false;
                factoryOptions.inMutable = true;
                bitmap = decodeFile(path, factoryOptions);
                bitmap = cropBitmapIfNeeded(bitmap, requireWidth, requireHeight);//虽然是小图但是也要切掉多余的部分
            }
        } else {
            //直接用fit,不要裁剪
            factoryOptions.inSampleSize = calculateInSampleFitSize(factoryOptions, requireWidth, requireHeight);
            factoryOptions.inJustDecodeBounds = false;
            factoryOptions.inMutable = true;
            bitmap = decodeFile(path, factoryOptions);
        }
        if (degree != 0 && bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            Bitmap rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            bitmap = rotateBitmap;
        }

        final Bitmap finalBitmap = bitmap;

        if (this.mIsCanceled) {
            if (finalBitmap != null) {
                Log.d("LocalRunnable", "任务已被取消,回收解析后的Bitmap");
                finalBitmap.recycle();
            }
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.onResult(LocalRunnable.this, finalBitmap);
            }
        });
    }

    private static Bitmap decodeRegion(BitmapRegionDecoder decoder, Rect rect, BitmapFactory.Options options) {
        Bitmap bitmap = null;
        options.inPreferredConfig = Constants.PREFERRED_CONFIG;
        if (options.inPreferredConfig == Bitmap.Config.RGB_565) {
            options.inDither = true;
        }
        synchronized (sDecodeLock) {
            bitmap = decoder.decodeRegion(rect, options);
        }
        if (options.inBitmap != null) {
            if (bitmap != options.inBitmap) {
                Log.d("LocalRunnable", "decodeRegion inBitmap failed");
                options.inBitmap.recycle();
            } else {
                Log.d("LocalRunnable", "decodeRegion inBitmap success");
            }
        }
        return bitmap;
    }

    private static Bitmap decodeFile(String path, BitmapFactory.Options options) {
        Bitmap bitmap = null;
        options.inPreferredConfig = Constants.PREFERRED_CONFIG;
        if (options.inPreferredConfig == Bitmap.Config.RGB_565) {
            options.inDither = true;
        }
        synchronized (sDecodeLock) {
            bitmap = BitmapFactory.decodeFile(path, options);
        }
        if (options.inBitmap != null) {
            if (bitmap != options.inBitmap) {
                Log.d("LocalRunnable", "decodeFile inBitmap failed");
                options.inBitmap.recycle();
            } else {
                Log.d("LocalRunnable", "decodeFile inBitmap success");
            }
        }
        return bitmap;
    }

    private Bitmap cropBitmapIfNeeded(Bitmap bitmap, int requireWidth, int requireHeight) {
        if (bitmap == null) {
            return bitmap;
        }
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        if (bitmapWidth == requireWidth && bitmapHeight == requireHeight) {
            return bitmap;
        } else if ((float) requireWidth / (float) bitmapWidth == (float) requireHeight / (float) bitmapHeight) {
            //比例相同
            return bitmap;
        } else if (bitmapWidth > requireWidth && bitmapHeight > requireHeight) {
            Bitmap result = Bitmap.createBitmap(bitmap, (bitmapWidth - requireWidth) / 2, (bitmapHeight - requireHeight) / 2, requireWidth, requireHeight);
            bitmap.recycle();
            return result;
        } else {
            float scale = Math.min((float) bitmapWidth / (float) requireWidth, (float) bitmapHeight / (float) requireHeight);
            int resultWidth = (int) (requireWidth * scale);
            int resultHeight = (int) (requireHeight * scale);
            Bitmap result = Bitmap.createBitmap(bitmap, (bitmapWidth - resultWidth) / 2, (bitmapHeight - resultHeight) / 2, resultWidth, resultHeight);
            bitmap.recycle();
            return result;
        }
    }

    //计算crop
    public static int calculateInSampleCropSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    //计算fit,为了解决图片非常大,高宽差距太大的问题
    public static int calculateInSampleFitSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
