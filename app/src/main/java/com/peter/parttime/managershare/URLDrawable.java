package com.peter.parttime.managershare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class URLDrawable extends BitmapDrawable {
    protected Bitmap bitmap = null;
    private Context mContext;
    private Drawable drawable = null;

    public URLDrawable(Context context) {
        mContext = context;
        drawable = mContext.getDrawable(R.drawable.blank);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas) {
        if (bitmap != null && !bitmap.isRecycled()) {
            canvas.drawBitmap(bitmap, 0, 0, getPaint());
        } else {
            drawable.draw(canvas);
        }
    }
}
