package com.peter.parttime.managershare;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
//import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import peter.parttime.utils.BitmapUtil;
import peter.parttime.utils.MiscUtil;
import peter.parttime.utils.NetworkUtil;


public class ManagerShareActivity extends Activity implements
        SwipeRefreshLayout.OnRefreshListener {
    public static final String TAG = "WebCrawler";
    public static final String APP_NAME = "panda_crawler";


    private static final long MAX_CACHE_SIZE = 40 * 1024 * 1024;
    private static final long TIME_CLEAR_CACHE = 5 * 60 * 60 * 1000;
    public static final String APP_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APP_NAME;
    public static final String getWebArticleDir() {
        return APP_DIR + "/article/";
    }
    public static final String getWebNewsDir() {
        return APP_DIR + "/news/";
    }
    public static final String getWebImagesDir() {
        return APP_DIR + "/images/";
    }
    public static final String NEWS_JSON_PATH = getWebNewsDir() + "news.json";
    private static final int MAX_CACHE_NEWS = 30;
    private static final int HOME_PAGE_NEWS_COUNT = 20;
    private static final int CONNECT_TIME_OUT = 3000;

    public static final String ACTION_NEWS_COMMING = "com.peter.crawler.ACTION_NEWS_COMMING";
    private BroadcastReceiver mNewsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            dbg("receive action: " + action);
            if (ACTION_NEWS_COMMING.equals(action)) {
                try {
                    List<Paper> locals = parseJsonForPapers(MiscUtil.readFromFile(NEWS_JSON_PATH));
                    List<Paper> temp = new CopyOnWriteArrayList<Paper>();
                    for (Paper p : locals) {
                        if (p.mHref.equals(mPapers.get(0).mHref)) {
                            break;
                        }
                        temp.add(p);
                    }
                    if (!temp.isEmpty()) {
                        addAllNews(0, temp);
                    }
                } catch (Exception e) {
                    error("receiver update: " + MiscUtil.getStackTrace(e));
                }
            }
        }
    };

    private SwipeRefreshLayout mSwipeLayout;
    private RecyclerView mRecyclerView;
    private com.peter.parttime.managershare.PaperAdapter mPaperAdapter;
    private LinearLayoutManager mLayoutManager;
    private List<Paper> mPapers = new CopyOnWriteArrayList<Paper>();
    private List<Paper> mFocusPapers = new CopyOnWriteArrayList<Paper>();

    private NotificationManager mNotificationManager = null;
    private ConnectivityManager mConnectivityManager = null;

    private NetworkReceiver mNetworkReceiver = null;

    private boolean mLoading = false;
    private ProgressBar mLoadingMoreProgressBar = null;
    private ProgressBar mUpdatingProgressBar = null;
    private TextView mHeaderHintTextView = null;


    public boolean isLoading() { return mLoading;}
    private void setLoading(boolean l) { mLoading = l;}

    private ThumbnailDownloader<ImageView> mThumbnailDownloader;

    public final static String html = "http://www.managershare.com/";

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
        unregisterReceiver(mNewsReceiver);
        unregisterReceiver(mNetworkReceiver);
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

        mHandler = new UIHandler(this);
        ManagerShareActivity.info("Start Manager share");

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mHandler.sendEmptyMessage(MSG_INITIALIZE_LATER);

        mUpdatingProgressBar = (ProgressBar) findViewById(R.id.updatingprogressbar);
        mHeaderHintTextView = (TextView) findViewById(R.id.header_hint);
        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mLayoutManager = new LinearLayoutManager(this);
        if (NetworkUtil.isNetworkAvailed(mConnectivityManager)) {
            refreshHomePage(false);
        } else {
            mUpdatingProgressBar.setVisibility(View.GONE);
            showHeaderHint(getString(R.string.no_availed_network), HEADER_HINT_TYPE_WARNING);
        }

        mNetworkReceiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, filter);
        filter = new IntentFilter(ACTION_NEWS_COMMING);
        registerReceiver(mNewsReceiver, filter);

        startService(new Intent(ManagerShareActivity.this, UpdateNewsService.class));

        scheduleClearCacheTask();
    }

    private final void scheduleClearCacheTask() {
        new Timer().schedule(new ClearCacheTask(), TIME_CLEAR_CACHE);
    }
    public void setSwipable(boolean b) {
        mSwipeLayout.setEnabled(b);
    }

    private static final int HEADER_HINT_TYPE_INFO = 0;
    private static final int HEADER_HINT_TYPE_WARNING = 1;
    private void showHeaderHint(String msg, int type) {
        mHeaderHintTextView.setVisibility(View.VISIBLE);
        mHeaderHintTextView.setText(msg);
        switch (type) {
            case HEADER_HINT_TYPE_WARNING:
                mHeaderHintTextView.setTextColor(Color.RED);
                break;
            case HEADER_HINT_TYPE_INFO:
            default:
                mHeaderHintTextView.setTextColor(Color.BLACK);
                break;
        }
    }
    private void hideHeaderHint() {
        mHeaderHintTextView.setVisibility(View.GONE);
    }

    private Toast mInvalidNetworkWarningToast = null;//Toast.makeText(this, getString(R.string.no_availed_network), Toast.LENGTH_SHORT);
    private void showInvalidNetworkWarning() {
        if (mInvalidNetworkWarningToast == null)
            mInvalidNetworkWarningToast = Toast.makeText(this, getString(R.string.no_availed_network), Toast.LENGTH_SHORT);
        mInvalidNetworkWarningToast.show();
    }

    public static void switchToArticle(Activity activity, String uri, String image, View v, View t) {
        Intent intent = new Intent();
        intent.setComponent(
                new ComponentName(activity, WebArticleActivity.class));
        intent.putExtra(WebArticleActivity.EXTRA_URL, html + "/" + uri);
        intent.putExtra(WebArticleActivity.EXTRA_IMAGE_URL, image);

        ActivityCompat.startActivity(activity, intent,
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                        activity,
                        Pair.create(v, "picture"),
                        Pair.create(t, "content")
                ).toBundle());
//        activity.startActivity(intent);
//        activity.overridePendingTransition(R.anim.activity_right_in, R.anim.activity_fade_out);
    }

    private PaperAdapter.OnItemClickListener mOnItemClickListener =
            new PaperAdapter.OnItemClickListener() {
        @Override
        public void onItemClickListener(View v, Paper p) {
            switchToArticle(ManagerShareActivity.this, p.mHref, p.mPicture,
                    v.findViewById(R.id.pic),
                    v.findViewById(R.id.title)
                    );

        }
    };

    private Thread mUpdateHomePageThread = null;
    @Override
    public void onRefresh() {
        if (!NetworkUtil.isNetworkAvailed(mConnectivityManager)) {
            showInvalidNetworkWarning();
            mSwipeLayout.setRefreshing(false);
        }
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
    @Deprecated
    @SuppressWarnings("unused")
    private static final int MSG_UPDATE_HOME_PAGE_REGULAR = 3; // no use anymore
    private static final int MSG_CONNECT_TIME_OUT = 4;
    private static final int MSG_REMOVE_NEWS = 5;
    private static final int MSG_INITIALIZE_LATER = 6;
    private static final int MSG_UPDATE_FOCUS_VIEW = 7;


   private static class UIHandler extends Handler {
       private final WeakReference<ManagerShareActivity> activity;

       public UIHandler(ManagerShareActivity activity) {
           this.activity = new WeakReference<ManagerShareActivity>(activity);
       }

       @Override
       public void handleMessage(Message msg) {
           final ManagerShareActivity a = activity.get();
           if (a != null) {
               switch (msg.what) {
                   case MSG_REMOVE_NEWS:
                       a.mPaperAdapter.notifyItemRangeRemoved(msg.arg1, msg.arg2 - msg.arg1);
                       info("remove news");
                       break;
                   case MSG_CONNECT_TIME_OUT:
                       a.mUpdatingProgressBar.setVisibility(View.GONE);
                       a.mLoadingMoreProgressBar.setVisibility(View.GONE);
                       a.mSwipeLayout.setRefreshing(false);
                       break;
                   case MSG_LOAD_NEXT_PAGE_DONE:
                       a.mPaperAdapter.notifyItemRangeChanged(msg.arg1, msg.arg2 - msg.arg1);
                       a.setLoading(false);
                       a.mLoadingMoreProgressBar.setVisibility(View.GONE);
                       a.mUpdatingProgressBar.setVisibility(View.GONE);
                       break;
                   case MSG_UPDATE_HOME_PAGE_DONE:
                       a.mPaperAdapter.notifyDataSetChanged();
                       a.mSwipeLayout.setRefreshing(false);
                       a.mUpdatingProgressBar.setVisibility(View.GONE);
                       break;
                   case MSG_SHOW_LAST_CONTENTS_HINT:
                       Toast.makeText(a,
                               R.string.update_to_date, Toast.LENGTH_SHORT).show();;
                       a.mSwipeLayout.setRefreshing(false);
                       break;
                   case MSG_INITIALIZE_LATER:
                       a.mThumbnailDownloader = new ThumbnailDownloader<ImageView>(new Handler());
                       a.mThumbnailDownloader.setListener(new ThumbnailDownloader.Listener<ImageView>() {
                           @Override
                           public void onThumbnailDownloaded(ImageView imageView,
                                                             Bitmap bitmap) {
                               imageView.setImageBitmap(bitmap);
                           }
                       });

                       a.mRecyclerView.setLayoutManager(a.mLayoutManager);
                       a.mRecyclerView.setItemAnimator(new DefaultItemAnimator());

                       a.mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
                           @Override
                           public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                               super.onScrollStateChanged(recyclerView, newState);
                           }

                           @Override
                           public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                               super.onScrolled(recyclerView, dx, dy);
                               if (a.isLoading()) return;
                               if (!NetworkUtil.isNetworkAvailed(a.mConnectivityManager)) {
                                   return;
                               }

                               int last = a.mLayoutManager.findLastVisibleItemPosition();
                               int count = a.mLayoutManager.getItemCount();

                               if ((last + 1) == count && dy > 0) {
                                   dbg("Loading more");
                                   a.setLoading(true);
                                   a.mCurrentPage++;
                                   a.mLoadingMoreProgressBar.setVisibility(View.VISIBLE);

                                   a.startGetNextPage();
                               }
                           }
                       });

                       a.mRecyclerView.setScrollBarSize(30);
                       a.mRecyclerView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

                       a.mLoadingMoreProgressBar = (ProgressBar) a.findViewById(R.id.loadingprogressbar);

                       a.mSwipeLayout = (SwipeRefreshLayout) a.findViewById(R.id.swipe_container);
                       a.mSwipeLayout.setOnRefreshListener(a);

                       a.mThumbnailDownloader.start();
                       a.mThumbnailDownloader.getLooper();

                       a.mPaperAdapter = new PaperAdapter(a, a.mPapers, a.mFocusPapers, a.mThumbnailDownloader);
                       a.mPaperAdapter.setOnItemClickListener(a.mOnItemClickListener);

                       try {
                           List<Paper> papers = a.parseJsonForPapers(MiscUtil.readFromFile(NEWS_JSON_PATH));
                           if (!papers.isEmpty()) {
                               int size = papers.size() > HOME_PAGE_NEWS_COUNT ? HOME_PAGE_NEWS_COUNT : papers.size() - 1;
                               a.mPapers.addAll(papers.subList(0, size));
                               a.mPaperAdapter.notifyItemRangeChanged(0, HOME_PAGE_NEWS_COUNT);
                           }
                       } catch (JSONException e) {
                           error("read news from json:" + MiscUtil.getStackTrace(e));
                       } catch (IOException e) {
                           error("read news from json:" + MiscUtil.getStackTrace(e));
                       }

                       a.mRecyclerView.setAdapter(a.mPaperAdapter);
                       break;
                   case MSG_UPDATE_FOCUS_VIEW:
                       a.mPaperAdapter.updateHeader();
                       a.mPaperAdapter.notifyItemRangeChanged(0, 1);
                       break;

                   default:
                       break;
               }
           }
       }
   }

    private static Handler mHandler;

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

    private int mCurrentPage = 1;
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.37 Safari/537.36";

    List<Paper> parseHomeMainPager(Document doc) {
        dbg("parseHomeMainPager");
        List<Paper> news = new CopyOnWriteArrayList<Paper>();
        Elements papers = doc.select(".main_left .focus .slide a");
        for (Element paper: papers) {
            String href = paper.attr("href");
            String title = paper.attr("title");
            Element e = paper.select(".slide_thumbnail img").first();
            String imgSrc = "/";
            if (e == null) {
                continue;
            }
            imgSrc = e.attr("src");
            dbg("Focus Article: " + title + " href: " + href + " # " +
                    "summary " +
                    " @" + imgSrc);
            news.add(new Paper(title,
                    "sumaary",
                    imgSrc,
                    "date",
                    href));
        }
        mFocusPapers.clear();
        mFocusPapers.addAll(news);
        mHandler.sendEmptyMessage(MSG_UPDATE_FOCUS_VIEW);
        return news;
    }
//    private static int reservePaperCount = 3;
    static List<Paper> parseDocument(Document doc, String lastPaper) {
        List<Paper> news = new CopyOnWriteArrayList<Paper>();
        Elements papers = doc.select(".post_list li");
        for (Element paper: papers) {
//            if (reservePaperCount-- > 0) continue;

            String href = paper.select("h3 a").attr("href");
            if (href.equals(lastPaper))
                break;

            String title = paper.select("h3").text();
            String summary = paper.getElementsByClass("post_summary").text();
            String imgSrc = paper.select("img").first().attr("data-original");
            String date = paper.select(".post_meta").text();
            dbg("Article: " + title + " href: " + href + " # " +
                    summary +
                    " @" + imgSrc);
            news.add(new Paper(title,
                    summary,
                    imgSrc,
                    date,
                    href));
        }
        return news;
    }
    static Document getWebDocument(int page) throws IOException {
        String url = html;
        if (page > 1)
            url += "/?&page=" + page;
        dbg("http:" + url);
        Connection conn = Jsoup.connect(url);
        conn.header("User-Aagent", USER_AGENT);
        conn.timeout(CONNECT_TIME_OUT);
        Document doc = conn.get();
        return doc;
    }
    private Document getWebDocument() throws IOException {
        return getWebDocument(mCurrentPage);
    }

    private void setNews(List<Paper> news) {
        mPapers.clear();
        mPapers.addAll(news);
        saveNewsToCache();
        mHandler.sendEmptyMessage(MSG_UPDATE_HOME_PAGE_DONE);
    }
    private void addAllNews(int position, List<Paper> news) {
        synchronized (mPapers) {
            mPapers.addAll(position, news);
            if (position < HOME_PAGE_NEWS_COUNT)
                saveNewsToCache();
        }
        mHandler.sendEmptyMessage(MSG_UPDATE_HOME_PAGE_DONE);
    }
    private void addAllNews(List<Paper> news) {
        synchronized (mPapers) {
            int count = mPapers.size();
            mPapers.addAll(news);
            if (count < HOME_PAGE_NEWS_COUNT)
                saveNewsToCache();
            }
        Message msg = mHandler.obtainMessage(MSG_LOAD_NEXT_PAGE_DONE,
                mPaperAdapter.getItemCount() - news.size(), mPaperAdapter.getItemCount() - 1);
        mHandler.sendMessage(msg);
    }

    private void saveNewsToCache() {
        int count = mPapers.size();
        if (count > MAX_CACHE_NEWS)
            count = MAX_CACHE_NEWS;

        writeJsonToFile(papersToJson(mPapers.subList(0, count - 1)), NEWS_JSON_PATH);
    }

    private void removeNewData(int position) {
        mPapers.remove(position);
    }
    private void removeNew(int position) {
        removeNew(position);
        Message msg = mHandler.obtainMessage(MSG_REMOVE_NEWS, position, 1);
        mHandler.sendMessage(msg);
    }
    private void removeNews(int start, int end) {
        if (start >= mPapers.size())
            start = mPapers.size() - 1;
        if (end >= mPapers.size())
            end = mPapers.size() - 1;
        do {
            removeNewData(start);
        } while (start < end--);
        Message msg = mHandler.obtainMessage(MSG_REMOVE_NEWS, start, end);
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
    private boolean getFromLocal = true;
    private class UpdateHomePageRunnable implements Runnable {

        public boolean isBackground = false;
        @Override
        public void run() {
            List<Paper> news = new CopyOnWriteArrayList<Paper>();
            try {
                Document doc = getWebDocument(0);
                parseHomeMainPager(doc);
                synchronized (mPapers) {
                    String lastPaper = getFromLocal || mPapers.isEmpty() ? "" : mPapers.get(0).mHref;
                    news = parseDocument(doc, lastPaper);
                    if (news.isEmpty() && !isBackground) {
                        mHandler.sendEmptyMessage(MSG_SHOW_LAST_CONTENTS_HINT);
                        isBackground = true;
                        return;
                    } else {
                        if (getFromLocal && !mPapers.isEmpty()) {
                            getFromLocal = false;
                            removeNews(0, mPapers.size() - 1);
                            setNews(news);
                        } else {
                            addAllNews(0, news);
                        }
                    }
                }
            } catch (IOException e) {
                error("connect time out: " + MiscUtil.getStackTrace(e));
                mHandler.sendEmptyMessage(MSG_CONNECT_TIME_OUT);
            }
            if (!news.isEmpty() && !isTopTask())
                sendNewArticleNotification(news);
        }
    }

    private Runnable mGetNextPageRunnable = new Runnable() {
        public boolean TEST_EMPTY_VIEW = false;
        @Override
        public void run() {
            if (TEST_EMPTY_VIEW) {
                TEST_EMPTY_VIEW = false;
                mHandler.sendEmptyMessage(MSG_UPDATE_HOME_PAGE_DONE);
                return;
            }
            try {
                Document doc = getWebDocument();
                synchronized (mPapers) {
                    List<Paper> news = parseDocument(doc, "fuck");
                    // because home page will be added some news, so every page will be changed.
                    // ---xxx | xxx---   . delete the xxx
                    synchronized (mPapers) {
                        String oldest = mPapers.get(mPapers.size() - 1).mHref;
                        int length = news.size();
                        String first = "";
                        for (int index = 0; index < length; index++) {
                            first = news.get(index).mHref;
                            if (oldest.equals(first)) {
                                removeNew(mPapers.size() - 1);
                                oldest = mPapers.get(mPapers.size() - 1).mHref;
                            }
                        }
                    }
                    addAllNews(news);
                }
            } catch (IOException e) {
                e.printStackTrace();
                error("connect time out");
                mHandler.sendEmptyMessage(MSG_CONNECT_TIME_OUT);
            }

        }
    };

    private class NetworkReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false) == false) {
                    hideHeaderHint();
                    if (mPapers.isEmpty())
                        refreshHomePage(false);
                } else {
                    showHeaderHint(getString(R.string.no_availed_network), HEADER_HINT_TYPE_WARNING);
                }
            }
            /*
            ManagerShareActivity.dbg("net work state change: " + action);
            ManagerShareActivity.dbg("no connectivity:" + intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
            ManagerShareActivity.dbg("info:" + intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
            ManagerShareActivity.dbg("network info:" + intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_TYPE));
            ManagerShareActivity.dbg("reason:" + intent.getStringExtra(ConnectivityManager.EXTRA_REASON));
            */
        }
    }


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

    public static class Paper {
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

        public boolean isSame(Paper p) {
            if (p == null) return false;
            return mHref.equals(p.mHref);
        }
    }

    public static final String JSON_NEWS_ARRAY = "news";
    public static final String JSON_NEWS_TITLE = "title";
    public static final String JSON_NEWS_SUMMARY = "summary";
    public static final String JSON_NEWS_PICTURE = "picture";
    public static final String JSON_NEWS_DATE = "date";
    public static final String JSON_NEWS_HREF = "href";
    static JSONObject papersToJson(List<Paper> papers) {
        JSONObject json = new JSONObject();
        JSONArray array = null;
        try {
            array = new JSONArray();
            JSONObject obj;
            for (Paper paper : papers) {
                obj = new JSONObject();
                obj.put(JSON_NEWS_TITLE, paper.mTitle);
                obj.put(JSON_NEWS_SUMMARY, paper.mSummary);
                obj.put(JSON_NEWS_DATE, paper.mDate);
                obj.put(JSON_NEWS_PICTURE, paper.mPicture);
                obj.put(JSON_NEWS_HREF, paper.mHref);
                array.put(obj);
            }
            json.put(JSON_NEWS_ARRAY, array);
        } catch (JSONException e) {
            error("papersToJson failed: " + MiscUtil.getStackTrace(e));
        }
        return json;
    }
    static List<Paper> parseJsonForPapers(String content) throws JSONException {
        if (content == null) return null;

        List<Paper> papers = new CopyOnWriteArrayList<Paper>();
        Paper paper;
        JSONObject json = new JSONObject(content);
        JSONArray news = json.getJSONArray(JSON_NEWS_ARRAY);
        int length = news.length();
        String title, summary, picture, date, href;
        for (int i = 0; i < length; i++) {
            JSONObject obj = news.getJSONObject(i);
            title = obj.getString(JSON_NEWS_TITLE);
            summary = obj.getString(JSON_NEWS_SUMMARY);
            picture = obj.getString(JSON_NEWS_PICTURE);
            date = obj.getString(JSON_NEWS_DATE);
            href = obj.getString(JSON_NEWS_HREF);
            paper = new Paper(title, summary, picture, date, href);
            papers.add(paper);
        }
        return papers;
    }

    static boolean writeJsonToFile(JSONObject json, String path) {
        boolean ret = false;
        try {
            ret = MiscUtil.writeToFile(path, json.toString());
        } catch (IOException e) {
            error("write json to file: " + MiscUtil.getStackTrace(e));
        }
        return ret;
    }


    public static final String getImagePath(String url) {
        return getWebImagesDir() + MiscUtil.toMD5(url);
    }
    public static final Bitmap getImageFromFile(String url) {
        return BitmapUtil.getBitmapFromFile(getImagePath(url));
    }
    public static final void saveBitmapToFile(Bitmap bm, String path) {
        try {
            BitmapUtil.saveBitmapToFile(bm, path);
        } catch (IOException e) {
            error("save bitmap to file: " + e);
        }
    }

    private class ClearCacheTask extends TimerTask {
        @Override
        public void run() {
            long usagedSize = MiscUtil.getDirUsagedBytes(APP_DIR);
            while (usagedSize > MAX_CACHE_SIZE) {
                // clear 1/4
                // images
                File[] files = new File(getWebImagesDir()).listFiles();
                int size = files.length;
                int count = size >> 2;
                if (size > 1) {
                    MiscUtil.sortFileByLastModified(files);
                    for (int i = 0;i < count; i++) {
                        files[size - 1 - i].delete();
                    }
                }

                // articles
                files = new File(getWebArticleDir()).listFiles();
                size = files.length;
                count = size >> 2;
                if (size > 1) {
                    MiscUtil.sortFileByLastModified(files);
                    for (int i = 0;i < count; i++) {
                        files[size - 1 - i].delete();
                    }
                }
                usagedSize = MiscUtil.getDirUsagedBytes(APP_DIR);
            }
            scheduleClearCacheTask();
        }
    }
}
