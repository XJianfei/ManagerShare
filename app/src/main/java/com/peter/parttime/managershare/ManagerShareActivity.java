package com.peter.parttime.managershare;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ManagerShareActivity extends Activity implements
        SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "WebCrawler";

    private SwipeRefreshLayout mSwipeLayout;
    private RecyclerView mRecyclerView;
    private PaperAdapter mPaperAdapter;
    private LinearLayoutManager mLayoutManager;
    private List<Paper> mPapers = new CopyOnWriteArrayList<Paper>();

    private NotificationManager mNotificationManager = null;

    private boolean mLoading = false;
    private ProgressBar mLoadingProgressBar = null;

    public boolean isLoading() { return mLoading;}
    private void setLoading(boolean l) { mLoading = l;}

    private ThumbnailDownloader<ImageView> mThumbnailDownloader;

    private final static String html = "http://www.managershare.com/";

    public static void dbg(String msg) {
        Log.d(TAG, "" + msg);
    }
    public static void error(String msg) {
        Log.e(TAG, "" + msg);
    }
    public static void info(String msg) {
        Log.i(TAG, "" + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.clearQueue();
    }


    private long mLastScrollTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        */
        setContentView(R.layout.activity_manager_share);

        mThumbnailDownloader = new ThumbnailDownloader<ImageView>(new Handler());
        mThumbnailDownloader.setListener(new ThumbnailDownloader.Listener<ImageView>() {
            @Override
            public void onThumbnailDownloaded(ImageView imageView,
                                              Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mPaperAdapter = new PaperAdapter(this, mPapers, mThumbnailDownloader);

        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (isLoading()) return;

                int last = mLayoutManager.findLastVisibleItemPosition();
                int count = mLayoutManager.getItemCount();

                if ((last + 1) == count && dy > 0) {
                    dbg("Loading more");
                    setLoading(true);
                    mCurrentPage++;
                    mLoadingProgressBar.setVisibility(View.VISIBLE);

                    startGetNextPage();
                }
            }
        });

        mPaperAdapter.setOnItemClickListener(mOnItemClickListener);
        mRecyclerView.setAdapter(mPaperAdapter);
        mRecyclerView.setScrollBarSize(30);
        mRecyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

        mLoadingProgressBar = (ProgressBar) findViewById(R.id.loadingprogressbar);

        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeLayout.setOnRefreshListener(this);

        startGetNextPage();

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HOME_PAGE_REGULAR, REGULAR_UPDATE_HOME_TIME);
        ManagerShareActivity.info("Start Manager share");
    }

    private PaperAdapter.OnItemClickListener mOnItemClickListener =
            new PaperAdapter.OnItemClickListener() {
        @Override
        public void onItemClickListener(View v, Paper p) {
            dbg("onItemClick: " + p.mHref);
            Intent intent = new Intent();
            intent.setComponent(
                    new ComponentName(ManagerShareActivity.this, WebArticleActivity.class));
            intent.putExtra(WebArticleActivity.EXTRA_URL, html + "/" + p.mHref);
            startActivity(intent);
            overridePendingTransition(R.anim.activity_right_in, R.anim.activity_fade_out);
        }
    };

    private Thread mUpdateHomePageThread = null;
    @Override
    public void onRefresh() {
        refreshHomePage(false);
    }

    private Thread mGetNextPageThread = null;
    private void startGetNextPage() {
        if (mGetNextPageThread == null || !mGetNextPageThread.isAlive()) {
            mGetNextPageThread = new Thread(mGetNextPageRunnable);
        }
        if (!mGetNextPageThread.isAlive())
            mGetNextPageThread.start();
    }
    private void refreshHomePage(boolean bg) {
        mUpdateHomePageRunnable.isBackground = bg;
        if (mUpdateHomePageThread == null || !mUpdateHomePageThread.isAlive()) {
            mUpdateHomePageThread = new Thread(mUpdateHomePageRunnable);
        }
        if (!mUpdateHomePageThread.isAlive())
            mUpdateHomePageThread.start();
    }
    private static final int REGULAR_UPDATE_HOME_TIME = 30 * 60 * 1000;
//    private static final int REGULAR_UPDATE_HOME_TIME =  5 * 1000;

    private static final int MSG_LOAD_NEXT_PAGE_DONE = 0;
    private static final int MSG_UPDATE_HOME_PAGE_DONE = 1;
    private static final int MSG_SHOW_LAST_CONTENTS_HINT = 2;
    private static final int MSG_UPDATE_HOME_PAGE_REGULAR = 3;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOAD_NEXT_PAGE_DONE:
                    mPaperAdapter.notifyItemRangeInserted(msg.arg1, msg.arg2);
                    setLoading(false);
                    mLoadingProgressBar.setVisibility(View.GONE);
                        break;
                case MSG_UPDATE_HOME_PAGE_DONE:
                    mPaperAdapter.notifyDataSetChanged();
                    mSwipeLayout.setRefreshing(false);
                    break;
                case MSG_SHOW_LAST_CONTENTS_HINT:
                    Toast.makeText(ManagerShareActivity.this,
                            R.string.update_to_date, Toast.LENGTH_SHORT).show();;
                    mSwipeLayout.setRefreshing(false);
                    break;
                case MSG_UPDATE_HOME_PAGE_REGULAR:
                    ManagerShareActivity.info("update regular");
                    refreshHomePage(true);
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_HOME_PAGE_REGULAR, REGULAR_UPDATE_HOME_TIME);
                    break;

                default:
                    break;
            }
        }
    };

    private boolean isTopTask() {
        final ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> apps = am.getRunningAppProcesses();
        return (apps.get(0).pid == android.os.Process.myPid());
    }

    private boolean mIsShowing = true;
    @Override
    protected void onStop() {
        super.onStop();
        mIsShowing = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsShowing = true;
    }

    private int mCurrentPage = 0;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.37 Safari/537.36";
    private Document getWebDocument(int page) throws IOException {
        String url = html;
        if (page != 0)
            url += "/?&page=" + page;
        dbg("http:" + url);
        Connection conn = Jsoup.connect(url);
        conn.header("User-Aagent", USER_AGENT);
        Document doc = conn.get();
        return doc;
    }
    private Document getWebDocument() throws IOException {
        return getWebDocument(mCurrentPage);
    }
    private class UpdateHomePageRunnable implements Runnable {

        public boolean isBackground = false;
        @Override
        public void run() {
            List<Paper> news = new CopyOnWriteArrayList<Paper>();
            try {
                Document doc = getWebDocument(0);
                Elements papers = doc.select(".post_list li");
                String lastPaper = mPapers.isEmpty() ? "" : mPapers.get(0).mTitle;
                for (Element paper : papers) {
                    String title = paper.select("h3").text();
                    dbg("Update: " + title + "  ?= " + lastPaper);
                    if (lastPaper.equals(title)) {
                        break;
                    }

                    String summary = paper.getElementsByClass("post_summary").text();
                    String imgSrc = paper.select(".lazy").first().attr("data-original");
                    String date = paper.select(".post_meta").text();
                    String href = paper.select("h3 a").attr("href");
                    dbg("Article: " + title + " href: " + href + " # " +
                            summary +
                            " @" + imgSrc);
                    news.add(new Paper(title,
                            summary,
                            imgSrc,
                            date,
                            href));
                }
                if (news.isEmpty() && !isBackground) {
                    mHandler.sendEmptyMessage(MSG_SHOW_LAST_CONTENTS_HINT);
                    isBackground = true;
                    return;
                } else {
                    addAllNews(0, news);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (!news.isEmpty() && !isTopTask())
                sendNewArticleNotification(news);
        }
    }

    private void addAllNews(int position, List<Paper> news) {
        synchronized (mPapers) {
            mPapers.addAll(position, news);
        }
        mHandler.sendEmptyMessage(MSG_UPDATE_HOME_PAGE_DONE);
    }
    private void addAllNews(List<Paper> news) {
        synchronized (mPapers) {
            mPapers.addAll(news);
        }
        Message msg = mHandler.obtainMessage(MSG_LOAD_NEXT_PAGE_DONE,
                mPaperAdapter.getItemCount() - news.size(), mPaperAdapter.getItemCount() - 1);
        mHandler.sendMessage(msg);
    }

    private void sendNewArticleNotification(List<Paper> news) {
        if (news.isEmpty()) return;
        if (mNotificationManager == null)
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ManagerShareActivity.this);

        builder.setContentTitle("" + news.get(0).mTitle)
                .setContentText(news.get(0).mTitle)
                .setSmallIcon(R.drawable.favicon)
                .setTicker("" + news.get(0).mTitle)
                .setAutoCancel(true)
                .setNumber(news.size())
                .setWhen(System.currentTimeMillis());

        Intent intent = new Intent(ManagerShareActivity.this, ManagerShareActivity.class);
        Intent[] is = new Intent[1];
        is[0] = intent;
        PendingIntent pi = PendingIntent.getActivities(ManagerShareActivity.this,
                0, is, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pi);
        mNotificationManager.notify(0, builder.build());
    }

    private UpdateHomePageRunnable mUpdateHomePageRunnable = new UpdateHomePageRunnable();
    private Runnable mGetNextPageRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = getWebDocument();
                Elements papers = doc.select(".post_list li");
                List<Paper> news = new CopyOnWriteArrayList<Paper>();
//                int i = 3;
                for (Element paper: papers) {
//                    if (i-- > 0) continue;
                    String title = paper.select("h3").text();
                    String summary = paper.getElementsByClass("post_summary").text();
                    String imgSrc = paper.select(".lazy").first().attr("data-original");
                    String date = paper.select(".post_meta").text();
                    String href = paper.select("h3 a").attr("href");
                    dbg("Article: " + title + " href: " + href + " # " +
                            summary +
                            " @" + imgSrc);
                    news.add(new Paper(title,
                            summary,
                            imgSrc,
                            date,
                            href));
                }
                addAllNews(news);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manager_share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public static byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.addRequestProperty("User-Agent", USER_AGENT);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = conn.getInputStream();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                error("connect failed");
                return null;
            }

            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    public class Paper {
        String mTitle;
        String mSummary;
        String mPicture;
        String mDate;
        String mHref;

        public Paper (String title, String summary, String picture,
                      String date, String href) {
            mTitle = title;
            mSummary = summary;
            mPicture = picture;
            mDate = date;
            mHref = href;
        }
    }
}
