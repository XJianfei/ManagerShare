package com.peter.parttime.managershare;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
    private int distance = 100;
    private int velocity = 200;

    private GestureDetector mGestureDetector;

    public SwipeGestureListener(Context context, int distance, int velocity) {
        super();
        this.distance = distance;
        this.velocity = velocity;

        mGestureDetector = new GestureDetector(context, this);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        if (e1.getX() - e2.getX() > distance
                && Math.abs(velocityX) > velocity) {
            swipeLeft();
        }
        if (e2.getX() - e1.getX() > distance
                && Math.abs(velocityX) > velocity) {
            swipeRight();
        }
        return false;
    }

    public boolean swipeLeft() {
        return false;
    }

    public boolean swipeRight() {
        return false;
    }
}
