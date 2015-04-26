package com.peter.parttime.managershare;

import android.app.Activity;
import android.content.Context;
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
import android.view.WindowManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class ManagerShareActivity extends Activity {

    private RecyclerView mRecyclerView;
    private PaperAdapter mPaperAdapter;
    private List<Paper> mPapers = new CopyOnWriteArrayList<Paper>();

    private final static String html = "http://www.managershare.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_manager_share);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mPaperAdapter = new PaperAdapter(this, mPapers);

        mRecyclerView.setAdapter(mPaperAdapter);
        new Thread(mRunnable).start();
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
            Log.d("123", "Go");
            try {
                Document doc = Jsoup.connect(html).get();
                Elements papers = doc.select(".post_list li");
                for (Element paper: papers) {
                    Log.d("123", "-:" + paper.select("h3").text() +
                     paper.getElementsByClass("post_summary").text());
                    mPapers.add(new Paper(paper.select("h3").text(),
                     paper.getElementsByClass("post_summary").text(), ""));
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
