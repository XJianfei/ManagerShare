package com.peter.parttime.managershare;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.LruCache;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import peter.parttime.utils.BitmapUtil;
import peter.parttime.utils.MiscUtil;


public class WebArticleActivity extends Activity {

    public static final String EXTRA_URL = "extra_url";

    private TextView mArticleContentTextView = null;
    private Article mArticle;
    /*
    private String mArticleContent = "";
    private String mArticleTitle = "";
    private String mArticleLead = "";
    private String mArticleMeta = "";
    */
    private String mPath = "";

    private LruCache<String, Bitmap> mMemoryCache;

    private static final int SWIPE_RIGHT_DISTANCE = 300;
    private static final int SWIPE_RIGHT_VELOCITY = 100;

    private static final int MAX_ARTICLE_CACHE_COUNT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window = getWindow();
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
        setContentView(R.layout.web_article);
        mArticleContentTextView = (TextView) findViewById(R.id.content);
        mArticleContentTextView.setFitsSystemWindows(true);
        mPath = getIntent().getStringExtra(EXTRA_URL);
        ManagerShareActivity.info("show: " + mPath);
        if (mPath == null) {
            mArticleContentTextView.setText(R.string.invalid_url);
            return;
        }

        mHandler = new UIHandler(this);

        ArticleScrollView v = (ArticleScrollView) findViewById(R.id.article);
        v.setOnSwipeListener(new ArticleScrollView.OnSwipeListener() {
            @Override
            public void onSwipeLeft() {
                finish();
                overridePendingTransition(R.anim.activity_left_in, R.anim.activity_right_out);
            }

            @Override
            public void onSwipeRight() {
            }
        });

        int maxMemory = (int)Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 2;
        ManagerShareActivity.info("Web article cache size: " + mCacheSize);

        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                super.entryRemoved(evicted, key, oldValue, newValue);
                if (evicted && oldValue != null && !oldValue.isRecycled()) {
                    oldValue.recycle();
                    oldValue = null;
                }
            }
        };

        new Thread(mGetArticalRunnable).start();
    }

    private String getDocumentCacheName(String url) {
        String cache = MiscUtil.toMD5(url);
        return cache;
    }


    private Document getWebDocument(String url) throws IOException {
        Connection conn = Jsoup.connect(url);
        conn.header("User-Aagent", ManagerShareActivity.USER_AGENT);
        Document doc = conn.get();
        return doc;
    }

    private static final int MSG_GET_WEB_CONTENT_DONE = 0;
    private static final int MSG_GET_WEB_CONTENT_FAILED = 1;
    private static final int MSG_SET_TEXT_SELECTABLE = 2;

    private static final int SET_TEXT_SELECTABLE_TIME = 1;
    private class UIHandler extends Handler {
        private WeakReference<WebArticleActivity> activity;
        public UIHandler(WebArticleActivity activity) {
            this.activity = new WeakReference<WebArticleActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            WebArticleActivity a = activity.get();
            if (a == null) return;
            switch (msg.what) {
                case MSG_GET_WEB_CONTENT_DONE:
                    //mArticleContentTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
                    a.mArticleContentTextView.setText(
                            Html.fromHtml(
                                    "<html><head>" +
                                            "<title >" +
                                            "<strong><font color=\"#00000000\" >" +
                                            a.mArticle.title + "</strong>" +
                                            "</title></head><br/><br/>" +
                                            "<body>" +
                                            "<font color=\"#00000000\">" +
                                            "<p><small>" + a.mArticle.meta + "</small></p>" +
                                            "<p>" + a.mArticle.lead + "</p>" +
                                            a.mArticle.content +
                                            "</body></html>", new URLImageParser(a), null));
                    break;
                case MSG_GET_WEB_CONTENT_FAILED:
                    a.mArticleContentTextView.setText(R.string.no_availed_network);
                    break;
                case MSG_SET_TEXT_SELECTABLE:
                    a.mArticleContentTextView.setTextIsSelectable(true);
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }

    }
    private Handler mHandler;

    private Article obtainArticle(String url) throws IOException {
        Article article;
        // from local
        String cache = MiscUtil.toMD5(url);
        ManagerShareActivity.dbg("url: " + url + " cache:" + cache);
        cache = ManagerShareActivity.getWebCacheDir() + cache;
        if (cache != null) {
            try {
                article = (Article) MiscUtil.restoreSerializable(cache);
                if (article != null) return article;
                // if is null, then delete it
                new File(cache).delete();
            } catch (ClassCastException e) {
                ManagerShareActivity.error("obtain article from local error");
            }
        }

        // from internet
        Document doc = getWebDocument(url);
        article = new Article();
        article.title = doc.select("h1").first().text();
        article.content = doc.select(".article > p").outerHtml();
        article.lead = doc.select(".article .post_lead_r").first().text();
        article.meta = doc.select(".post_meta").text();
        article.path = url;
        if (cache != null) {
            File dir = new File(cache).getParentFile();
            File[] files = dir.listFiles();
            long oldest = Long.MAX_VALUE;
            long time;
            File old = null;
            if (files.length >= MAX_ARTICLE_CACHE_COUNT) {
                for (File file : files) {
                    time = file.lastModified();
                    if (oldest > time) {
                        oldest = time;
                        old = file;
                    }
                }
                if (old != null) {
                    old.delete();
                }
            }
            MiscUtil.storeSerializable(article, cache);
        }
        return article;
    }

    private Runnable mGetArticalRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                mArticle = obtainArticle(mPath);
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_DONE);
            } catch (IOException e) {
                ManagerShareActivity.error("Can't connect to " + mPath);
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_FAILED);
            }
        }
    };

    private class URLImageParser implements Html.ImageGetter {
        private WebArticleActivity activity;
        public URLImageParser(WebArticleActivity activity) {
            this.activity = activity;
        }
        @Override
        public Drawable getDrawable(String source) {
            URLDrawable urlDrawable = new URLDrawable(activity);
            if (source.startsWith("/")) {
                try {
                    URL url = new URL(mPath);
                    source = "http://" + url.getHost() + source;
                } catch (MalformedURLException e) {
                }
            }
            ManagerShareActivity.dbg("getDrawable: " + source);
            if (activity.mMemoryCache.get(source) != null) {
                urlDrawable.bitmap = activity.mMemoryCache.get(source);
            } else {
                AsyncImageGetter at = new AsyncImageGetter(urlDrawable);
                Drawable drawable = activity.getDrawable(R.drawable.blank);
                urlDrawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

                at.execute(source);
            }
            return urlDrawable;
        }
    }

    private class AsyncImageGetter extends AsyncTask<String, Void, Bitmap> {
        URLDrawable urlDrawable;

        public AsyncImageGetter(URLDrawable d) {
            urlDrawable = d;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            urlDrawable.bitmap = bitmap;

            if (bitmap != null)
                urlDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            // set isSelectable false, or textview will scroll to top.
            mArticleContentTextView.setTextIsSelectable(false);
            mArticleContentTextView.setText(mArticleContentTextView.getText());
            mHandler.sendEmptyMessageDelayed(MSG_SET_TEXT_SELECTABLE, SET_TEXT_SELECTABLE_TIME);

        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return fetchDrawable(params[0]);
        }

        public Bitmap fetchDrawable(String urlString) {
            Bitmap b = null;
            try {
                byte[] bitmapBytes = ManagerShareActivity.getUrlBytes(urlString);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                int maxWidth = mArticleContentTextView.getWidth() - mArticleContentTextView.getPaddingLeft() - mArticleContentTextView.getPaddingRight();
                if (bitmap.getWidth() > maxWidth) {
                    b = BitmapUtil.scaleWithWidth(bitmap, maxWidth);
                    bitmap.recycle();
                } else {
                    b = bitmap;
                }
                mMemoryCache.put(urlString, b);

            } catch (Exception e) {
                ManagerShareActivity.error("image: " + urlString);
                Log.e(ManagerShareActivity.TAG, "web", e);
            }

            return b;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMemoryCache.evictAll();
    }
}
