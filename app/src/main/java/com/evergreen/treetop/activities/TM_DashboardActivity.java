package com.evergreen.treetop.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.goals.TM_GoalEditorActivity;
import com.evergreen.treetop.activities.notes.TM_NotesActivity;
import com.evergreen.treetop.activities.units.TM_UnitPickerActivity;
import com.evergreen.treetop.activities.units.TM_UnitViewActivity;
import com.evergreen.treetop.activities.users.TM_SignUpActivity;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.ui.adapters.TaskGridAdapter;
import com.evergreen.treetop.ui.views.recycler.GoalBoardListRecycler;
import com.evergreen.treetop.ui.views.recycler.TaskGridRecycler;

public class TM_DashboardActivity extends AppCompatActivity {

    private TaskGridRecycler m_gridTasks;
    private GoalBoardListRecycler m_listGoals;

    private static TM_DashboardActivity runningInstance = null;
    private ActivityResultLauncher<Intent> m_unitListLauncher = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {

                    Unit picked = Unit.of(
                            (DBUnit)result.getData()
                                .getSerializableExtra(TM_UnitPickerActivity.RESULT_PICKED_EXTRA_KEY));

                    startActivity(
                            new Intent(this, TM_UnitViewActivity.class)
                                    .putExtra(TM_UnitViewActivity.UNIT_ID_EXTRA_KEY, picked.getId())
                    );
                }
            }
    );

    public static final String FORBID_BACK_EXTRA_KEY = "forbid-back";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dasboard_tm);

        m_gridTasks = findViewById(R.id.tm_dashboard_recycler_task_grid);
        m_listGoals = findViewById(R.id.tm_dashboard_recycler_goal_list);


        Button buttonUnitFilter = findViewById(R.id.tm_dashboard_button_filter_units);
        setUnitFilterText(buttonUnitFilter);
        buttonUnitFilter.setOnClickListener(v -> cycleUnitFilter((Button)v));

        Button buttonAssignedFilter = findViewById(R.id.tm_dashboard_button_filter_assigned);
        setAssignedFilterText(buttonAssignedFilter);
        buttonAssignedFilter.setOnClickListener(v -> cycleAssignedFilter((Button)v));

        Button buttonCompleteFilter = findViewById(R.id.tm_dashboard_button_filter_complete);
        setCompleteFilterText(buttonCompleteFilter);
        buttonCompleteFilter.setOnClickListener(v -> cycleCompleteFilter((Button)v));

    }

    @Override
    protected void onResume() {
        runningInstance = this;
        m_listGoals.getAdapter().refresh();
        m_gridTasks.getAdapter().refresh();
        super.onResume();
    }

    @Override
    protected void onPause() {
        runningInstance = null;
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (!getIntent().getBooleanExtra(FORBID_BACK_EXTRA_KEY, false)) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard_options, menu);


            // FIXME this is to handle weird, random crushes
            if (UserDB.getInstance().getCurrentUser() == null) {
                Log.w("UI_ERROR", "Went to dashboard, but user was null!");
                startActivity(new Intent(this, TM_SignUpActivity.class));
                return false;
            }

            if (UserDB.getInstance().getCurrentUser().getLeadingIds().size() == 0) {
                menu.removeItem(R.id.tm_dashboard_options_meni_new_goal);
            }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.tm_dashboard_options_meni_new_goal) {
            startActivity(new Intent(this, TM_GoalEditorActivity.class));
        } else if (itemId == R.id.tm_dashboard_options_meni_unit_view) {
            m_unitListLauncher.launch(new Intent(this, TM_UnitPickerActivity.class));
        } else if (itemId == R.id.tm_dashboard_options_meni_log_out) {
            UserDB.getInstance().logout(this);
        } else if (itemId == R.id.tm_dashboard_options_meni_notes_list) {
            startActivity(new Intent(this, TM_NotesActivity.class));
        }

        return true;
    }

    private void setUnitFilterText(Button cyclerButton) {
        switch (TaskGridAdapter.getUnitFilter()) {
            case TaskGridAdapter.UNIT_FILTER_NONE:
                cyclerButton.setText("All");
                break;
            case TaskGridAdapter.UNIT_FILTER_IN:
                cyclerButton.setText("In");
                break;
            case TaskGridAdapter.UNIT_FILTER_LEADING:
                cyclerButton.setText("Leading");
                break;
        }
    }

    private void cycleUnitFilter(Button cyclerButton) {
        m_gridTasks.getAdapter().cycleUnitFilter();
        m_listGoals.getAdapter().cycleUnitFilter();
        setUnitFilterText(cyclerButton);
    }

    private void setAssignedFilterText(Button cyclerButton) {
        switch (TaskGridAdapter.getAssignedFilter()) {
            case TaskGridAdapter.ASSIGN_FILTER_NONE:
                cyclerButton.setText("All");
                break;
            case TaskGridAdapter.ASSIGN_FILTER_ASSIGNER:
                cyclerButton.setText("Assigner");
                break;
            case TaskGridAdapter.ASSIGN_FILTER_ASSIGNEE:
                cyclerButton.setText("Assignee");
                break;
        }
    }

    private void cycleAssignedFilter(Button cyclerButton) {
        m_gridTasks.getAdapter().cycleAssignedFilter();
        setAssignedFilterText(cyclerButton);
    }

    private void setCompleteFilterText(Button cyclerButton) {
        switch (TaskGridAdapter.getCompletedFilter()) {
            case TaskGridAdapter.COMPLETE_FILTER_NONE:
                cyclerButton.setText("All");
                break;
            case TaskGridAdapter.COMPLETE_FILTER_INCOMPLETE:
                cyclerButton.setText("Incomplete");
                break;
            case TaskGridAdapter.COMPLETE_FILTER_COMPLETE:
                cyclerButton.setText("Complete");
                break;
        }
    }

    private void cycleCompleteFilter(Button cyclerButton) {
        m_gridTasks.getAdapter().cycleCompleteFilter();
        m_listGoals.getAdapter().cycleCompleteFilter();
        setCompleteFilterText(cyclerButton);
    }



    public static TM_DashboardActivity getRunningInstance() {
        return runningInstance;
    }
}