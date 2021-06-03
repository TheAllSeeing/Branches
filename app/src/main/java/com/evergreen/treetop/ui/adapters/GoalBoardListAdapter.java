package com.evergreen.treetop.ui.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.goals.TM_GoalViewActivity;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.adapters.GoalBoardListAdapter.GoalHolder;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GoalBoardListAdapter extends RecyclerView.Adapter<GoalHolder> {

    private List<Goal> m_data;
    private final Activity m_context;

    private static int s_unitFilter = 0;
    private static int s_assignedFilter = 0;
    private static int s_completedFilter = 0;


    public GoalBoardListAdapter(Activity context) {
        m_data = new ArrayList<>();
        m_context = context;
        Log.v("UI_EVENT", "Created a new GoalkList with values " + LoggingUtils.stringify(m_data));
    }


    @NonNull
    @Override
    public GoalHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.v("UI_EVENT", "Created new TaskHolder");
        return new GoalHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalHolder holder, int position) {
        holder.setContent(m_data.get(position));
        Log.v("UI_EVENT", "Bound new TaskHolder to " + m_data.get(position));
    }

    @Override
    public int getItemCount() {
        return m_data.size();
    }

    public class GoalHolder extends RecyclerView.ViewHolder {

        public GoalHolder(ViewGroup parent) {
            super(LayoutInflater.from(m_context)
                    .inflate(R.layout.listrow_recycler_goal_dashboard_list, parent, false)
            );
        }

        public void setContent(Goal goal) {
            geTitletView().setText(goal.getTitle());
            geTitletView().setOnClickListener(v -> m_context.startActivity(
                    new Intent(m_context, TM_GoalViewActivity.class)
                            .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, goal.getId())
            ));

//            new Thread(() -> {
                ProgressBar border = getBorder();
//
//                border.setMax(goal.getTaskCount());
//

                if (goal.isCompleted()) {
                    border.setProgressDrawable(ContextCompat.getDrawable(m_context, R.drawable.circle));
                } else {
                    border.setProgressDrawable(ContextCompat.getDrawable(m_context, R.drawable.ring));
                    border.setIndeterminateDrawable(ContextCompat.getDrawable(m_context, R.drawable.ring));;
                }
//
                UIUtils.setProgressColor(border, goal.getPriorityColor(m_context));
//
//                border.setMax(goal.getTaskCount());
//                UIUtils.setProgressColor(border, goal.getPriorityColor(m_context));
//                border.setMax(100);
//                border.setProgress(25);
//                try {
//                    int completeCount = goal.getCompletedCount();
//                    m_context.runOnUiThread(() -> {
//                        UIUtils.animateProgress(border, completeCount);
//                    });
//
//                } catch (ExecutionException | NoSuchDocumentException | InterruptedException ignored) { }
//            }).start();

        }

        public TextView geTitletView() {
            return (TextView) itemView.findViewById(R.id.tm_dashboard_goal_list_item_text_title);
        }

        public ProgressBar getBorder() {
            return (ProgressBar) itemView.findViewById(R.id.tm_dashboard_goal_list_item_prog_border);
        }

    }

    public void reset() throws ExecutionException, InterruptedException {
        m_data = GoalDB.getInstance().getAll()
                .stream().sorted(Goal.PRIORITY_COMPARATOR)
                 .collect(Collectors.toList());
    }

    public void cycleUnitFilter() {
        s_unitFilter = (s_unitFilter + 1) % 3;
        refresh();
    }

    public void cycleAssignedFilter() {
        s_assignedFilter = (s_assignedFilter + 1) % 2;
        refresh();
    }

    public void cycleCompleteFilter() {
        s_completedFilter = (s_completedFilter + 1) % 3;
        refresh();
    }

    public void refresh() {

        // FIXME when this fails the filters are still ncremented

        new Thread(() -> {
            Looper.prepare();
            try {
                reset();
                applyUnitFilter(s_unitFilter);
                applyCompletedFilter(s_completedFilter);
                sort();
                m_context.runOnUiThread(this::notifyDataSetChanged);
            } catch (ExecutionException e) {
                Toast.makeText(m_context, "Could not refresh tasks: Database error", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Could not refresh task grid:\n" + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled refresh of task grid:\n" + ExceptionUtils.getStackTrace(e));
            }
        }).start();
    }

    private void applyUnitFilter(int filter) {

        User currentUser = UserDB.getInstance().getCurrentUser();
        switch (filter) {
            case 1:
                filterForInUnit(currentUser);
                break;
            case 2:
                filterForLeading(currentUser);
                break;
        }

    }

    private void applyCompletedFilter(int filter) {
        switch (s_completedFilter) {
            case 1:
                filterForIncomplete();
                break;
            case 2:
                filterForComplete();
                break;
        }

    }

    private void filterForInUnit(User user) {
        m_data = m_data.stream().filter(task -> user.getUnitIds().contains(task.getUnitId())).collect(Collectors.toList());
    }

    private void filterForLeading(User user) {
        m_data = m_data.stream().filter(task -> user.getLeadingIds().contains(task.getUnitId())).collect(Collectors.toList());
    }

    private void filterForComplete() {
        m_data = m_data.stream().filter(Goal::isCompleted).collect(Collectors.toList());
    }

    private void filterForIncomplete() {
        m_data = m_data.stream().filter(goal -> !goal.isCompleted()).collect(Collectors.toList());
    }

    private synchronized void sort() {
        m_data.sort(Goal.PRIORITY_COMPARATOR);
    }


}
