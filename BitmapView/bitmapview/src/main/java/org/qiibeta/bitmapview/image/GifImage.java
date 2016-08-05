package org.qiibeta.bitmapview.image;


import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;

import java.io.RandomAccessFile;

//gif must be smaller than 1MB 1024*1024
public class GifImage extends AppImage {
    public static GifImage newInstance(Bitmap bitmap, String path) {
        final GifImage image = new GifImage();
        try {
            RandomAccessFile file = new RandomAccessFile(path, "r");
            byte[] b = new byte[(int) file.length()];
            file.readFully(b);
            image.mGifDecoder = new GifDecoder(b);
        } catch (Exception e) {
            e.printStackTrace();
        }

        image.mBitmap = bitmap;
        return image;
    }

    private Bitmap mBitmap;
    private Matrix mFixOrientation = new Matrix();

    private GifDecoder mGifDecoder;
    private Exception mException;
    private volatile GifFrame mCurrentFrame;
    private long mNextFrameRender;
    private Thread mThread;
    private volatile boolean mIsDecoding;

    @Override
    public void update(RectF viewRect, Matrix matrix) {
        //empty
    }

    @Override
    public void onDraw(Canvas canvas, Matrix matrix) {
        startTaskIfNeeded();
        if (this.mBitmap != null) {
            mFixOrientation.set(matrix);

            if (mException == null && mCurrentFrame != null) {
                canvas.drawBitmap(mCurrentFrame.image, mFixOrientation, null);
            } else {
                canvas.drawBitmap(this.mBitmap, mFixOrientation, null);
            }
        }
    }

    @Override
    public int getWidth() {
        return this.mBitmap != null ? this.mBitmap.getWidth() : 0;
    }

    @Override
    public int getHeight() {
        return this.mBitmap != null ? this.mBitmap.getHeight() : 0;
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
    }

    private Runnable mDecodeGifRunnable = new Runnable() {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    mGifDecoder.nextFrame();
                    while (System.currentTimeMillis() < mNextFrameRender) {
                        Thread.sleep(15);
                    }
                    long now = System.currentTimeMillis();
                    if (now >= mNextFrameRender) {
                        // see if a frame is available
                        if (mGifDecoder.getLastFrame() != mCurrentFrame) {
                            // we have a frame waiting, grab it i guess.
                            mCurrentFrame = mGifDecoder.getLastFrame();
                            postInvalidate();
                            // check if we need to drop frames, or maintain timing
                            if (now > mNextFrameRender + getDelay())
                                mNextFrameRender = now + getDelay();
                            else
                                mNextFrameRender += getDelay();
                        }
                        if (mGifDecoder.getStatus() == GifDecoder.STATUS_FINISH)
                            mGifDecoder.restart();
                    }
                } catch (OutOfMemoryError e) {
                    mException = new Exception(e);
                    Thread.currentThread().interrupt();
                    mIsDecoding = false;
                } catch (Exception e) {
                    mException = e;
                    Thread.currentThread().interrupt();
                    mIsDecoding = false;
                }
            }
        }
    };

    private long getDelay() {
        // error case?
        if (mCurrentFrame == null)
            return 1000 / 10;
        long delay = mCurrentFrame.delay;
        if (delay == 0)
            delay = 1000 / 10;
        return delay;
    }

    private void startTaskIfNeeded() {
        if (mIsDecoding)
            return;
        if (mException != null)
            return;

        mIsDecoding = true;
        mThread = new Thread(mDecodeGifRunnable);
        mThread.start();
    }

    private static void log(String log) {
        Log.d("GifImage", log);
    }
}
