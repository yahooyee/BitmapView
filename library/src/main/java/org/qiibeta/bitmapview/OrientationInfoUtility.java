package org.qiibeta.bitmapview;


import android.media.ExifInterface;
import android.support.annotation.IntDef;

import org.qiibeta.bitmapview.utility.ImageHeaderParser;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class OrientationInfoUtility {
    @Retention(SOURCE)
    @IntDef({ORIENTATION_ROTATE_0, ORIENTATION_ROTATE_90, ORIENTATION_ROTATE_180, ORIENTATION_ROTATE_270})
    public @interface ORIENTATION_ROTATE {
    }

    public static final int ORIENTATION_ROTATE_0 = 0;
    public static final int ORIENTATION_ROTATE_90 = 90;
    public static final int ORIENTATION_ROTATE_180 = 180;
    public static final int ORIENTATION_ROTATE_270 = 270;


    @ORIENTATION_ROTATE
    static int getOrientation(String filePath) {
        int orientationInt = ORIENTATION_ROTATE_0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                orientationInt = ORIENTATION_ROTATE_90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                orientationInt = ORIENTATION_ROTATE_180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                orientationInt = ORIENTATION_ROTATE_270;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientationInt;
    }

    @ORIENTATION_ROTATE
    static int getOrientation(InputStream inputStream) {
        int orientationInt = ORIENTATION_ROTATE_0;
        ImageHeaderParser parser = new ImageHeaderParser(inputStream);
        try {
            int orientation = parser.getOrientation();
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    orientationInt = ORIENTATION_ROTATE_0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    orientationInt = ORIENTATION_ROTATE_90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    orientationInt = ORIENTATION_ROTATE_180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    orientationInt = ORIENTATION_ROTATE_270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientationInt;
    }
}
