package peter.parttime.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MiscUtil {
    public static String toMD5(String content) {
        MessageDigest md = null;
        String md5 = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(content.getBytes());
            byte[] digests = md.digest();

            int i;
            StringBuffer buf = new StringBuffer("");
            for (byte b : digests) {
                i = b;
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            md5 = buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md5;
    }

    public static boolean storeSerializable(Serializable s, String path) {
        boolean ret = false;
        FileOutputStream fs = null;
        ObjectOutputStream os = null;
        File dir = new File(path).getParentFile();
        dir.mkdirs();
        try {
            fs = new FileOutputStream(path);
            os = new ObjectOutputStream(fs);
            os.writeObject(s);
            ret = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fs != null)
                    fs.close();
                if (os != null)
                    os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public static Serializable restoreSerializable(String path) {
        Serializable s = null;
        FileInputStream fis = null;
        ObjectInputStream oi = null;
        try {
            fis = new FileInputStream(path);
            oi = new ObjectInputStream(fis);
            s = (Serializable) oi.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (oi != null) try {
                oi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }
}
