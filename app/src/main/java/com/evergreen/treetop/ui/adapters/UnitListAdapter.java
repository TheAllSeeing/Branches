package com.evergreen.treetop.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.units.TM_UnitViewActivity;
import com.evergreen.treetop.architecture.data.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UnitListAdapter extends Adapter<ViewHolder> {

    private Context m_context;
    private Unit m_parent;
    private List<Unit> m_data;

    public UnitListAdapter(Context context) {
        m_context = context;
        m_data = new ArrayList<>();
    }

    public void init(Unit parent) throws ExecutionException, InterruptedException {
        m_parent = parent;
        refresh();
    }

    public void refresh() throws ExecutionException, InterruptedException {
        m_data = m_parent.getChildren();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(m_context).inflate(
                R.layout.listrow_unit_list,
                parent
        )) {};
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView textTitle = holder.itemView.findViewById(R.id.tm_unit_list_item_text_title);
        textTitle.setText(m_data.get(position).getName());

        OnClickListener action = v -> m_context.startActivity(
                new Intent(m_context, TM_UnitViewActivity.class)
                .putExtra(TM_UnitViewActivity.UNIT_ID_EXTRA_KEY, m_data.get(position).getId())
        );

        holder.itemView.setOnClickListener(action);
        textTitle.setOnClickListener(action);
    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }
}
