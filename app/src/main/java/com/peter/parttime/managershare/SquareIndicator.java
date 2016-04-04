package com.peter.parttime.managershare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import org.apache.http.MethodNotSupportedException;

public class SquareIndicator extends View {
    private int foregroundColor = 0xff00b2ff;
    private int backgroundColor = 0x00000000;
    public int showSize = 4;
    public int position = 0;
    public SquareIndicator(Context context) {
        super(context);
    }

    public SquareIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SquareIndicator(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPosition(int position) {
        this.position = position;
        this.invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(backgroundColor);
        if (showSize <= 0)
            return;
        paint.setColor(foregroundColor);
        canvas.drawRect(position * getWidth() / 4, 0,
                (position + 1) * getWidth() / showSize, getHeight(), paint);
    }
}
