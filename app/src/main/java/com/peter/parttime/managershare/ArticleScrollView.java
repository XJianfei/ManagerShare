package com.peter.parttime.managershare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ArticleScrollView extends ScrollView {
    public ArticleScrollView(Context context) {
        super(context);
    }

    public ArticleScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArticleScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ArticleScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    private OnSwipeListener listener = null;

    private int distance = 100;
    private static final int distanceY = 200;

    interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    public void setOnSwipeListener(OnSwipeListener l) { listener = l; }

    private float mLastMotionEventX = INVALID_EVENT;
    private float mLastMotionEventY = INVALID_EVENT;
    private static final float INVALID_EVENT = -11111;

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (listener != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mLastMotionEventX = ev.getX();
                    mLastMotionEventY = ev.getY();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (Math.abs(mLastMotionEventY - ev.getY()) > distanceY) {
                        break;
                    }
                    if (ev.getX() - mLastMotionEventX > distance) {
                        listener.onSwipeLeft();
                    }
                    if (mLastMotionEventX - ev.getX() > distance) {
                        listener.onSwipeRight();
                    }
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (listener != null) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (Math.abs(mLastMotionEventY - ev.getY()) > distanceY) {
                        break;
                    }
                    if (ev.getX() - mLastMotionEventX > distance) {
                        listener.onSwipeLeft();
                    }
                    if (mLastMotionEventX - ev.getX() > distance) {
                        listener.onSwipeRight();
                    }
                    break;
            }
        }
        return super.onTouchEvent(ev);
    }
}
