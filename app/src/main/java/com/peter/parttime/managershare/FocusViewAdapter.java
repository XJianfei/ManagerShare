package com.peter.parttime.managershare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import peter.parttime.utils.MiscUtil;

public class FocusViewAdapter extends PagerAdapter{
    private List<ManagerShareActivity.Paper> papers = new ArrayList<>();
    private List<VM> mViews = new ArrayList<>();
    private Queue<VM> mQueue = new LinkedList<>();
    private Activity mActivity;
    private class VM {
        View v;
        ManagerShareActivity.Paper paper;
        Bitmap bm;
    }

    private View.OnClickListener itemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
                int position = 0;
                int size = mViews.size();
                for (; position < size; position++) {
                    if (v == mViews.get(position).v) {
                        break;
                    }
                }
                ManagerShareActivity.dbg("Click: " + position + " @ " + mViews.get(position).paper.mHref);
                if (position >= 0 && mViews.size() > position) {
                    ManagerShareActivity.switchToArticle(mActivity,
                            mViews.get(position).paper.mHref,
                            mViews.get(position).paper.mPicture,
                            v.findViewById(R.id.image),
                            v.findViewById(R.id.title));
                }
        }
    };

    public void updateDatas() {
        if (papers.size() != 0) {
            for (ManagerShareActivity.Paper paper : papers) {
                ViewGroup vg = (ViewGroup) LayoutInflater.from(mActivity).inflate(R.layout.focus_view,  null, false);
                TextView tv = (TextView) vg.findViewById(R.id.title);
                tv.setText(paper.mTitle);
                ImageView v = (ImageView) vg.findViewById(R.id.image);
                v.setImageResource(R.drawable.p1);
                vg.setOnClickListener(itemClickListener);
                VM vm = new VM();
                vm.v = vg;
                vm.bm = null;
                vm.paper = paper;
                mViews.add(vm);
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                int size = papers.size();
                for (int position = 0; position < size; position++) {
                    try {
                        ManagerShareActivity.Paper paper = papers.get(position);
                        String url = paper.mPicture;
                        ManagerShareActivity.dbg("#" + position + " url " + url);
                        Bitmap bm = ManagerShareActivity.getImageFromFile(url);
                        if (bm == null) {
                            byte[] bitmapBytes = ManagerShareActivity.getUrlBytes(url);
                            bm = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                            ManagerShareActivity.saveBitmapToFile(bm, ManagerShareActivity.getImagePath(url));
                        }
                        ViewGroup vg = (ViewGroup) mViews.get(position).v;
                        ImageView iv = (ImageView) vg.findViewById(R.id.image);
                        VM vm = new VM();
                        vm.v = vg;
                        vm.bm = bm;
                        synchronized (mQueue) {
                            mQueue.add(vm);
                            if (iv != null) {
                                Handler handler = iv.getHandler();
                                if (handler != null) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            synchronized (mQueue) {
                                                while (mQueue.size() > 0) {
                                                    VM vm = mQueue.remove();
                                                    ((ImageView) vm.v.findViewById(R.id.image)).setImageBitmap(vm.bm);
                                                }
                                            }
                                        }
                                    });
                                } else {
                                    //((ImageView) vm.v).setImageBitmap(vm.bm);
                                    ((ImageView) vm.v.findViewById(R.id.image)).setImageBitmap(vm.bm);
                                }
                            }
                        }
                    } catch (IOException e) {
                        ManagerShareActivity.error(MiscUtil.getStackTrace(e));
                    }
                }
            }
        }).start();
    }


    public FocusViewAdapter(Activity a, List<ManagerShareActivity.Paper> papers) {
        super();
        this.papers = papers;
        mActivity = a;
        updateDatas();
    }

    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
    @Override
    public void destroyItem(ViewGroup container, int position,
                            Object object) {
        // TODO Auto-generated method stub
        container.removeView(mViews.get(position).v);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        // TODO Auto-generated method stub
        container.addView(mViews.get(position).v);
        return mViews.get(position).v;
    }

    @Override
    public int getItemPosition(Object object) {
        return mViews.indexOf(object);
    }
}
