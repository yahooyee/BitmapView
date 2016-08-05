package org.qiibeta.bitmapview;


import android.media.ExifInterface;

import java.io.IOException;

class OrientationInfoUtility {
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
