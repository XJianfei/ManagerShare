package com.peter.parttime.managershare;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

public class ArticleTextView extends TextView{
    private OnSwipeListener listener = null;

    private int distance = 100;

    interface OnSwipeListener {
        void onSwipeLeft();
        void onSwipeRight();
    }

    public void setOnSwipeListener(OnSwipeListener l) { listener = l; }

    public ArticleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ArticleTextView(Context context) {
        super(context);
    }

    private float mLastMotionEventX = -1;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener != null) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                mLastMotionEventX = event.getX();
            } else if (action == MotionEvent.ACTION_CANCEL) {
                if (event.getX() - mLastMotionEventX > distance) {
                    listener.onSwipeLeft();
                }
                if (mLastMotionEventX - event.getX() > distance) {
                    listener.onSwipeRight();
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
