package com.peter.parttime.managershare;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThumbnailDownloader<Token> extends HandlerThread {

    private static final int MSG_DOWNLOAD = 0;
    private Handler mHandler;
    private Handler mResponseHandler;
    private Listener mListener;
    private Map<Token, String> requesetMap = Collections.
            synchronizedMap(new HashMap<Token, String>());

    private LruCache<String, Bitmap> mMemoryCache;

    public ThumbnailDownloader(Handler handler) {
        super(ManagerShareActivity.TAG);
        mResponseHandler = handler;

        int maxMemory = (int)Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(mCacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

    }

    public interface Listener<Token> {
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener(Listener<Token> listener) {
        mListener = listener;
    }

    @Override
    public void onLooperPrepared() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == MSG_DOWNLOAD) {
                    Token token = (Token) message.obj;
                    try {
                        handleRequest(token);
                    } catch (IOException e) {
                        Log.e(ManagerShareActivity.TAG, "loop", e);
                    }
                }
            }
        };
    }

    private void handleRequest(final Token token) throws IOException {
        final  String url = requesetMap.get(token);
        if (url == null)
            return;

        String key = (String) ((ImageView)token).getTag();
        byte[] bitmapBytes = ManagerShareActivity.getUrlBytes(url);
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        mMemoryCache.put(key, bitmap);
        mResponseHandler.post(new Runnable() {
            @Override
            public void run() {
                if (requesetMap.get(token) != url)
                    return;
                requesetMap.remove(token);
                mListener.onThumbnailDownloaded(token, bitmap);
            }
        });
    }

    public void clearQueue() {
        mHandler.removeMessages(MSG_DOWNLOAD);
        requesetMap.clear();
        mMemoryCache.evictAll();
    }

    public void queueThumbnail(Token token, String url) {
        requesetMap.put(token, url);
        Message msg = mHandler.obtainMessage(MSG_DOWNLOAD, token);
        msg.sendToTarget();
    }

    public Bitmap getCacheImage(String key) {
        Bitmap bitmap = mMemoryCache.get(key);
        return bitmap;
    }

}
