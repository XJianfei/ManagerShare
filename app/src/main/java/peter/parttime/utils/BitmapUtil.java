package peter.parttime.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.peter.parttime.managershare.ManagerShareActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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

    public static boolean saveBitmapToFile(Bitmap bm, String path) throws IOException {
        if (bm == null)
            return false;

        File file = new File(path);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (!parent.mkdirs())
                return false;
        }

        OutputStream os = new FileOutputStream(file);
        bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
        os.close();
        return true;
    }

    public static Bitmap getBitmapFromFile(final String path) {
        if (path == null) return null;
        File file = new File(path);
        if (!file.exists()) return null;
        Bitmap bm = BitmapFactory.decodeFile(path);
        return bm;
    }

}
