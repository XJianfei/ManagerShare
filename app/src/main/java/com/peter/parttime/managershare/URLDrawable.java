package com.peter.parttime.managershare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class URLDrawable extends BitmapDrawable {
    protected Drawable drawable;

    private Rect rect;
    public URLDrawable(Context context, int width, int height) {
        rect = new Rect(0, 0, width, height);
        drawable = context.getDrawable(R.drawable.p1);
        setBounds(rect);
        drawable.setBounds(rect);
    }

    @Override
    public void draw(Canvas canvas) {
        if (drawable != null) {
            drawable.draw(canvas);
        }
    }
}
