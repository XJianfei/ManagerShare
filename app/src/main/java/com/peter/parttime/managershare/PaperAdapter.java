package com.peter.parttime.managershare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.ViewHolder> {

    private List<ManagerShareActivity.Paper> mPapers;
    private Context mContext;
    private ThumbnailDownloader<ImageView> mThumbnailDownloader;
    public PaperAdapter(Context context, List<ManagerShareActivity.Paper> papers,
                ThumbnailDownloader<ImageView> loader) {
        mContext = context;
        mPapers = papers;
        mThumbnailDownloader = loader;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ManagerShareActivity.Paper p = mPapers.get(position);
        holder.mTitleTextView.setText(p.mTitle);
        holder.mSummaryTextView.setText(p.mSummary);
        holder.mImageView.setImageDrawable(mContext.getDrawable(R.drawable.p1));
        holder.mImageView.setTag(p.mPicture);

        Bitmap bitmap = null;
        ManagerShareActivity.dbg("url: " + p.mPicture);

        if ((bitmap = mThumbnailDownloader.getCacheImage(p.mPicture)) != null) {
            holder.mImageView.setImageBitmap(bitmap);
        } else {
            mThumbnailDownloader.queueThumbnail(holder.mImageView, p.mPicture);
        }
    }

    @Override
    public int getItemCount() {
        return mPapers == null ? 0 : mPapers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleTextView;
        public TextView mSummaryTextView;
        public ImageView mImageView;
        public Drawable mPicture;
        public ViewHolder(View v) {
            super(v);
            mTitleTextView = (TextView) v.findViewById(R.id.title);
            mSummaryTextView = (TextView) v.findViewById(R.id.summary);
            mImageView = (ImageView) v.findViewById(R.id.pic);
            mPicture = null;
        }
    }
}
