package com.evergreen.treetop.ui.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UserPickerAdapter extends Adapter<UserPickerAdapter.UserPickerHolder> {


    private List<String> m_selectedOnInit;

    private final Activity m_context;
    private final List<User> m_data;
    private final List<User> m_selected = new ArrayList<>();



    public UserPickerAdapter(Activity context) {
        m_context = context;
        m_data = new ArrayList<>();
    }

    public void init(Unit withUnit, List<String> selectedOnInitIds) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        m_selectedOnInit = selectedOnInitIds;
        m_data.addAll(withUnit.getMembers());
        m_context.runOnUiThread(this::notifyDataSetChanged);
    }

    @NonNull
    @Override
    public UserPickerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserPickerHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull UserPickerHolder holder, int position) {

        User itemUser = m_data.get(position);

        holder.getTitleView().setText(itemUser.getName());

        holder.itemView.setOnClickListener(v ->
                holder.toggleSelect(itemUser)
        );

        if (m_selectedOnInit.contains(m_data.get(position).getId())) {
            holder.setSelected(true, itemUser);
        }
    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }

    public List<User> getSelected() {
        return m_selected;
    }

    public class UserPickerHolder extends ViewHolder {

        private TextView m_textTitle;
        private ImageView m_imgIcon;

        private boolean m_selected;

        public UserPickerHolder(@NonNull ViewGroup parent) {
            super(LayoutInflater.from(m_context).inflate(R.layout.listrow_user_picker, parent, false));
            m_textTitle = itemView.findViewById(R.id.tm_user_picker_text_title);
            m_imgIcon = itemView.findViewById(R.id.tm_user_picker_img_select_icon);
            m_selected = false;
        }

        private void toggleSelect(User withUser) {
            setSelected(!m_selected, withUser);
        }

        private void setSelected(boolean value, User withUser) {
            m_selected = value;

            if (m_selected) {
                select(withUser);
            } else {
                unselect(withUser);
            }
        }

        private void select(User withUser) {
            m_imgIcon.setImageDrawable(ContextCompat.getDrawable(m_context, R.drawable.ic_selected));
            UserPickerAdapter.this.m_selected.add(withUser);
        }

        private void unselect(User withUser) {
            m_imgIcon.setImageDrawable(ContextCompat.getDrawable(m_context, R.drawable.ic_unselected));
            UserPickerAdapter.this.m_selected.remove(withUser);
        }

        public TextView getTitleView() {
            return m_textTitle;
        }
    }
}
