package com.evergreen.treetop.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.R;

public class UnitEditListAdapter extends UnitListAdapter {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_UNIT = 1;

    // DUMB BAND-AID: whenever Trying to bind the header directly at onCreate of parent activity
    // resulted in NullPointerException: It was ran before the header was inflated.
    private boolean m_headerCreated = false;
    private HeaderHolder m_header;
    private View.OnClickListener m_headerListener;

    public UnitEditListAdapter(Context context) {
        super(context);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            HeaderHolder holder = new HeaderHolder(parent);
            m_headerCreated = true;

            if (m_headerListener != null) {
                holder.bind(m_headerListener);
            }

            m_header = m_header;
            return holder;
        }

        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (getItemViewType(position) == VIEW_TYPE_UNIT) {
            super.onBindViewHolder(holder, position - 1);
        }

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_UNIT;
        }
    }

    public static class HeaderHolder extends RecyclerView.ViewHolder {

        public HeaderHolder(ViewGroup parent) {

            super(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listrow_recycler_add_header, parent, false)
            );

        }

        public void bind(View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
        }
    }


    public void bindHeader(View.OnClickListener listener) {
        m_headerListener = listener;

        if (m_header != null) {
            m_header.bind(listener);
        }
    }


}
