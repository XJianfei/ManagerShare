package peter.parttime.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class BitmapUtil {
    public static Bitmap scaleWithWidth(Bitmap bm, int width) {
        Bitmap b = null;
        int rawWidth = bm.getWidth();
        int rawHeight = bm.getHeight();
        float scale = ((float)width / rawWidth);
        Matrix matric = new Matrix();
        matric.postScale(scale, scale);
        b = Bitmap.createBitmap(bm, 0, 0, rawWidth, rawHeight, matric, true);
        return b;
    }
}
