package com.peter.parttime.managershare;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

                    new Thread(mGetNextPageRunnable).start();
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

        new Thread(mGetNextPageRunnable).start();

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
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

        }
    };

    @Override
    public void onRefresh() {
        new Thread(mUpdateHomePageRunnable).start();
    }

    private static final int MSG_LOAD_NEXT_PAGE_DONE = 0;
    private static final int MSG_UPDATE_HOME_PAGE_DONE = 1;
    private static final int MSG_SHOW_LAST_CONTENTS_HINT = 2;

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

                default:
                    break;
            }
        }
    };

    private int mCurrentPage = 0;
    private Document getWebDocument(int page) throws IOException {
        String url = html;
        if (page != 0)
            url += "/?&page=" + page;
        dbg("http:" + url);
        Document doc = Jsoup.connect(url).get();
        return doc;
    }
    private Document getWebDocument() throws IOException {
        return getWebDocument(mCurrentPage);
    }
    private Runnable mUpdateHomePageRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = getWebDocument(0);
                Elements papers = doc.select(".post_list li");
                String lastPaper = mPapers.get(0).mTitle;
                List<Paper> news = new CopyOnWriteArrayList<Paper>();
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
                if (news.size() == 0) {
                    mHandler.sendEmptyMessage(MSG_SHOW_LAST_CONTENTS_HINT);
                    return;
                } else {
                    mPapers.addAll(0, news);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            mHandler.sendEmptyMessage(MSG_UPDATE_HOME_PAGE_DONE);
        }
    };
    private Runnable mGetNextPageRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = getWebDocument();
                Elements papers = doc.select(".post_list li");
                int lastCount = mPaperAdapter.getItemCount();
                for (Element paper: papers) {
                    String title = paper.select("h3").text();
                    String summary = paper.getElementsByClass("post_summary").text();
                    String imgSrc = paper.select(".lazy").first().attr("data-original");
                    String date = paper.select(".post_meta").text();
                    String href = paper.select("h3 a").attr("href");
                    dbg("Article: " + title + " href: " + href + " # " +
                            summary +
                            " @" + imgSrc);
                    mPapers.add(new Paper(title,
                            summary,
                            imgSrc,
                            date,
                            href));
                }
                Message msg = mHandler.obtainMessage(MSG_LOAD_NEXT_PAGE_DONE,
                        lastCount, mPaperAdapter.getItemCount() - 1);
                mHandler.sendMessage(msg);
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
