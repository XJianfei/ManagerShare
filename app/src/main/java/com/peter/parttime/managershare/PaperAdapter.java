package com.peter.parttime.managershare;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class PaperAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_EMPTY = 199;
    private static final int VIEW_TYPE_HEADER = 198;
    private static final int VIEW_TYPE_NORMAL = 197;
    private List<ManagerShareActivity.Paper> mPapers;
    private List<ManagerShareActivity.Paper> mFocus;
    private View mHeader = null;
    private FocusViewAdapter mFocusAdapter = null;
    private Context mContext;
    private ThumbnailDownloader<ImageView> mThumbnailDownloader;
    public PaperAdapter(Context context, List<ManagerShareActivity.Paper> papers,
                List<ManagerShareActivity.Paper> focus,
                ThumbnailDownloader<ImageView> loader) {
        mContext = context;
        mFocus = focus;
        mPapers = papers;
        mThumbnailDownloader = loader;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (isEmpty()) {
            View v2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty_view, parent, false);
            return new EmptyViewHolder(v2);
        }
        View v;
        switch (viewType) {
            case VIEW_TYPE_NORMAL:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_view, parent, false);
                return new ViewHolder(v, VIEW_TYPE_NORMAL);
            case VIEW_TYPE_HEADER:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.focus_layout, parent, false);
                return new ViewHolder(v, VIEW_TYPE_HEADER);
        }
        View v2 = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty_view, parent, false);
        return new EmptyViewHolder(v2);
    }

    private boolean isEmpty() {
        if (mPapers != null && mPapers.isEmpty()) {
            return true;
        }
        return false;
    }

    public void updateHeader() {
        if (mFocusAdapter != null) {
            mFocusAdapter.updateDatas();
            mFocusAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
        if (getItemViewType(position) == VIEW_TYPE_EMPTY) {
            return;
        }
        if (!(vh instanceof ViewHolder))
            return;
        ViewHolder holder = (ViewHolder)vh;
        if (holder.type == VIEW_TYPE_HEADER) {
            ViewPager vp = holder.vp;
            if (holder.pa == null) {
                PagerAdapter pa = new FocusViewAdapter((Activity)mContext, mFocus);
                mFocusAdapter = (FocusViewAdapter) pa;
                vp.setAdapter(pa);
                holder.pa = pa;
            }
            holder.pa.notifyDataSetChanged();
            return;
        }
        ManagerShareActivity.Paper p = mPapers.get(position);
        holder.mTitleTextView.setText(p.mTitle);
        holder.mDateTextView.setText(p.mDate);
        holder.mSummaryTextView.setText(p.mSummary);
        holder.mImageView.setImageDrawable(mContext.getDrawable(R.drawable.p1));
        holder.mImageView.setTag(p.mPicture);
        holder.mPaper = p;
        holder.mListener = mListener;

        Bitmap bitmap = null;

        if ((bitmap = mThumbnailDownloader.getCacheImage(p.mPicture)) != null) {
            holder.mImageView.setImageBitmap(bitmap);
        } else {
            mThumbnailDownloader.queueThumbnail(holder.mImageView, p.mPicture);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isEmpty()) return VIEW_TYPE_EMPTY;
        if (position == 0) return VIEW_TYPE_HEADER;
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public int getItemCount() {
        return isEmpty() ? 1 : mPapers.size() + 1;
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(View v) {
            super(v);
        }

    }
    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {
        public TextView mTitleTextView;
        public TextView mSummaryTextView;
        public TextView mDateTextView;
        public ImageView mImageView;
        public Drawable mPicture;
        public ViewPager vp;
        public PagerAdapter pa = null;
        public ManagerShareActivity.Paper mPaper;
        public int type = 0;

        private void init(View v) {
            mTitleTextView = (TextView) v.findViewById(R.id.title);
            mSummaryTextView = (TextView) v.findViewById(R.id.summary);
            mImageView = (ImageView) v.findViewById(R.id.pic);
            mDateTextView = (TextView) v.findViewById(R.id.date);
            mPicture = null;
            v.setClickable(true);
            v.setOnClickListener(this);
        }
        public ViewHolder(View v, int type) {
            super(v);
            this.type = type;
            switch (type) {
                case VIEW_TYPE_NORMAL:
                    init(v);
                    break;
                case VIEW_TYPE_HEADER:
                    vp = (ViewPager) v.findViewById(R.id.focus);
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            ManagerShareActivity.dbg("click: " + mTitleTextView.getText());
            if (mListener != null) {
                mListener.onItemClickListener(v, mPaper);
            }
        }
        public OnItemClickListener mListener = null;


    }


    private OnItemClickListener mListener = null;
    public void setOnItemClickListener(OnItemClickListener l) {
        mListener = l;
    }

    public interface OnItemClickListener {
        void onItemClickListener(View v, ManagerShareActivity.Paper p);
    }
}
