package com.peter.parttime.managershare;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.util.LruCache;
import android.view.Window;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import peter.parttime.utils.BitmapUtil;


public class WebArticleActivity extends Activity {

    public static final String EXTRA_URL = "extra_url";

    private TextView mArticleContentTextView = null;
    private String mArticleContent = "";
    private String mArticleTitle = "";
    private String mArticleLead = "";
    private String mArticleMeta = "";
    private String mPath = "";

    private LruCache<String, Bitmap> mMemoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.web_article);
        mArticleContentTextView = (TextView) findViewById(R.id.content);
        mPath = getIntent().getStringExtra(EXTRA_URL);
        ManagerShareActivity.info("show: " + mPath);
        if (mPath == null) {
            mArticleContentTextView.setText(R.string.invalid_url);
            return;
        }

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

    private Document getWebDocument(String url) throws IOException {
        ManagerShareActivity.dbg("http:" + url);
        Document doc = Jsoup.connect(url).get();
        return doc;
    }

    private static final int MSG_GET_WEB_CONTENT_DONE = 0;
    private static final int MSG_GET_WEB_CONTENT_FAILED = 1;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_WEB_CONTENT_DONE:
                    //mArticleContentTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
                    mArticleContentTextView.setText(
                            Html.fromHtml(
                                    "<html><head>" +
                                            "<title >" +
                                            "<strong><font color=\"#00000000\" >" +
                                            mArticleTitle + "</strong>" +
                                            "</title></head><br/><br/>" +
                                            "<body>" +
                                            "<font color=\"#00000000\">" +
                                            "<p><small>" + mArticleMeta + "</small></p>" +
                                            "<p>" + mArticleLead + "</p>" +
                                            mArticleContent +
                                            "</body></html>", new URLImageParser(), null));
                    break;
                case MSG_GET_WEB_CONTENT_FAILED:
                    mArticleContentTextView.setText(R.string.invalid_url);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private Runnable mGetArticalRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = getWebDocument(mPath);
                mArticleTitle = doc.select("h1").first().text();
                mArticleContent = doc.select(".article > p").outerHtml();
                mArticleLead = doc.select(".article .post_lead_r").first().text();
                mArticleMeta = doc.select(".post_meta").text();
                ManagerShareActivity.error("lead: " + mArticleLead);
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_DONE);
            } catch (IOException e) {
                ManagerShareActivity.error("Can't connect to " + mPath);
                mHandler.sendEmptyMessage(MSG_GET_WEB_CONTENT_FAILED);
            }
        }
    };

    private class URLImageParser implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            URLDrawable urlDrawable = new URLDrawable(WebArticleActivity.this);
            ManagerShareActivity.dbg("getDrawable: " + source);
            if (mMemoryCache.get(source) != null) {
                urlDrawable.bitmap = mMemoryCache.get(source);
            } else {
                AsyncImageGetter at = new AsyncImageGetter(urlDrawable);
                Drawable drawable = WebArticleActivity.this.getDrawable(R.drawable.blank);
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
            mArticleContentTextView.setText(mArticleContentTextView.getText());
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