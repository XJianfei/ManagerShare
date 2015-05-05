package peter.parttime.utils;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkUtil {
    public static boolean isNetworkAvailed(ConnectivityManager cm) {
        if (cm == null) return false;
        /*
        NetworkInfo[] infos = cm.getAllNetworkInfo();
        for (final NetworkInfo info : infos) {
            if (info.isConnected())
                return true;
        }
        */
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        return info.isConnected();
    }

    public static boolean isWifiWorking(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        if (info.getType() != ConnectivityManager.TYPE_WIFI) return false;
        return info.isConnected();
    }

    public static boolean isMobileNetworkWorking(ConnectivityManager cm) {
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        if (info.getType() != ConnectivityManager.TYPE_MOBILE) return false;
        return info.isConnected();
    }
}
