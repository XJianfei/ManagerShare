package com.peter.parttime.managershare;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.ViewHolder> {

    private List<ManagerShareActivity.Paper> mPapers;
    private Context mContext;
    public PaperAdapter(Context context, List<ManagerShareActivity.Paper> papers) {
        mContext = context;
        mPapers = papers;
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
    }

    @Override
    public int getItemCount() {
        return mPapers == null ? 0 : mPapers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitleTextView;
        public TextView mSummaryTextView;
        public ImageView mImageView;
        public ViewHolder(View v) {
            super(v);
            mTitleTextView = (TextView) v.findViewById(R.id.title);
            mSummaryTextView = (TextView) v.findViewById(R.id.summary);
            mImageView = (ImageView) v.findViewById(R.id.pic);
        }
    }
}
