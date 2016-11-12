package org.qiibeta.bitmapview;


import android.media.ExifInterface;
import android.support.annotation.IntDef;

import java.io.IOException;
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

    static int getOrientation(String filePath) {
        int orientationInt = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                orientationInt = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                orientationInt = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                orientationInt = 270;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return orientationInt;
    }
}
