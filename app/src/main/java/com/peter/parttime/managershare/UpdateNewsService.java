package com.peter.parttime.managershare;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import peter.parttime.utils.MiscUtil;

import static com.peter.parttime.managershare.ManagerShareActivity.parseJsonForPapers;
import static peter.parttime.utils.MiscUtil.readFromFile;

public class UpdateNewsService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ManagerShareActivity.dbg("on start command");
        return super.onStartCommand(intent, flags, startId);
    }

    //private static final int REGULAR_UPDATE_HOME_TIME = 30 * 60 * 1000;
    private static final int REGULAR_UPDATE_HOME_TIME = 30 * 60 * 1000;
    private class UpdateTask extends TimerTask {
        @Override
        public void run() {
            ManagerShareActivity.dbg("update news service task");
            new Timer().schedule(new UpdateTask(), REGULAR_UPDATE_HOME_TIME);
            try {
                List<ManagerShareActivity.Paper> locals = parseJsonForPapers(readFromFile(ManagerShareActivity.NEWS_JSON_PATH));
                List<ManagerShareActivity.Paper> news = ManagerShareActivity.parseDocument(ManagerShareActivity.getWebDocument(0), locals.get(0).mHref);
                /* for test
                news.add(new ManagerShareActivity.Paper("创业是个坑，挖坑的就是你自己", "xxxxxx",
                        "http://img.managershare.com/uploads/2015/05/14309301496562.jpg_200x150.jpg",
                        "date", "/post/178564"));
                        */
                if (!news.isEmpty()) {
                    ManagerShareActivity.info("update news: " + news.size());
                    locals.addAll(0, news);
                    ManagerShareActivity.writeJsonToFile(ManagerShareActivity.papersToJson(locals), ManagerShareActivity.NEWS_JSON_PATH);
                    sendNewArticleNotification(news);
                    sendBroadcast(new Intent(ManagerShareActivity.ACTION_NEWS_COMMING));
                }
            } catch (JSONException e) {
                ManagerShareActivity.error("update service:" + MiscUtil.getStackTrace(e));
            } catch (IOException e) {
                ManagerShareActivity.error("update service:" + MiscUtil.getStackTrace(e));
            }
        }
    }
    NotificationManager mNotificationManager = null;
    private void sendNewArticleNotification(List<ManagerShareActivity.Paper> news) {
        if (news.isEmpty()) return;
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle("" + news.get(0).mTitle)
                .setContentText(news.get(0).mTitle)
                .setSmallIcon(R.drawable.favicon)
                .setTicker("" + news.get(0).mTitle)
                .setAutoCancel(true)
                .setNumber(news.size())
                .setWhen(System.currentTimeMillis());



        Intent intent = new Intent(this, ManagerShareActivity.class);
        Intent[] is = new Intent[1];
        is[0] = intent;
        PendingIntent pi = PendingIntent.getActivities(this,
                0, is, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pi);
        mNotificationManager.notify(0, builder.build());
    }

    @Override
    public void onCreate() {
        ManagerShareActivity.dbg("on create");
        super.onCreate();
        new Timer().schedule(new UpdateTask(), REGULAR_UPDATE_HOME_TIME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
