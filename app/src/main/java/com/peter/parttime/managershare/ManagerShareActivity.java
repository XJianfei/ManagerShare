package com.peter.parttime.managershare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;

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


public class ManagerShareActivity extends Activity {
    public static final String TAG = "WebCrawler";

    private RecyclerView mRecyclerView;
    private PaperAdapter mPaperAdapter;
    private List<Paper> mPapers = new CopyOnWriteArrayList<Paper>();

    private ThumbnailDownloader<ImageView> mThumbnailDownloader;

    private final static String html = "http://www.managershare.com/";

    public static void dbg(String msg) {
        Log.d(TAG, "" + msg);
    }
    public static void error(String msg) {
        Log.e(TAG, "" + msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.clearQueue();
    }

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
                dbg("set image bitmap done");
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mPaperAdapter = new PaperAdapter(this, mPapers, mThumbnailDownloader);

        mRecyclerView.setAdapter(mPaperAdapter);

        new Thread(mRunnable).start();

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mPaperAdapter.notifyDataSetChanged();
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Document doc = Jsoup.connect(html).get();
                Elements papers = doc.select(".post_list li");
                for (Element paper: papers) {
                    String title = paper.select("h3").text();
                    String summary = paper.getElementsByClass("post_summary").text();
                    String imgSrc = paper.select(".lazy").first().attr("data-original");
                    dbg("article: " + title +
                            summary +
                            " @" + imgSrc);
                    mPapers.add(new Paper(title,
                            summary,
                            imgSrc));
                }
                mHandler.sendEmptyMessage(0);
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

        public Paper (String title, String summary, String picture) {
            mTitle = title;
            mSummary = summary;
            mPicture = picture;
        }

        public int getImageResourceId( Context context ) {
            try {
                return context.getResources().getIdentifier(this.mPicture, "drawable",
                        context.getPackageName());

            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }
}
