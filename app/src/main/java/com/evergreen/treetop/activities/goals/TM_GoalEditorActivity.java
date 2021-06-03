package com.evergreen.treetop.activities.goals;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.activities.units.TM_UnitPickerActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.utils.DBGoal;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.architecture.utils.TaskUtils;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.views.spinner.BaseSpinner;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TM_GoalEditorActivity extends AppCompatActivity {

    private EditText m_editTitle;
    private EditText m_editDescription;
    private TextView m_textUnit;
    private BaseSpinner m_spinPriority;


    private String m_id;
    boolean m_new;
    private Goal m_goalToDisplay;
    private Unit m_pickedUnit;

    private static final List<String> s_editingIds = new ArrayList<>();

    public static final String GOAL_ID_EXTRA_KEY = "goal-id";
    public static final String RESULT_GOAL_EXTRA_KEY = "result-goal";

    ActivityResultLauncher<Intent> m_unitPicker = registerForActivityResult(
            new StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    m_pickedUnit = Unit.of((DBUnit) result.getData().getSerializableExtra(TM_UnitPickerActivity.RESULT_PICKED_EXTRA_KEY));
                    m_textUnit.setText(m_pickedUnit.getName());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_editor_tm);

        m_id = getIntent().getStringExtra(GOAL_ID_EXTRA_KEY);

        m_editTitle = findViewById(R.id.tm_goal_editor_edit_title);
        m_editDescription = findViewById(R.id.tm_goal_editor_edit_description);
        m_textUnit = findViewById(R.id.tm_goal_editor_text_unit);
        m_spinPriority = findViewById(R.id.tm_goal_editor_spin_priority);

        m_spinPriority.loadOptions(new String[]{"", "A", "B", "C", "D", "E"});
        m_textUnit.setOnClickListener(v -> m_unitPicker.launch(
                new Intent(this, TM_UnitPickerActivity.class)
                .putExtra(TM_UnitPickerActivity.USER_LEADING_FILTER_EXTRA_KEY, true)
        ));

        if (m_id != null) {

            m_new = false;
            UIUtils.showLoading(this, R.id.tm_goal_editor_constr_form);

            new Thread(() -> {
                showGoal(m_id);
                runOnUiThread(() -> UIUtils.hideLoading(this, R.id.tm_goal_editor_constr_form));
            }).start();

        } else {
            m_id = GoalDB.getInstance().newDoc().getId();
            m_new = true;
        }

    }

    @Override
    protected void onDestroy() {

        if (!m_new) {
            s_editingIds.remove(m_id);
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_goals_editor_options, menu);
        getMenuInflater().inflate(R.menu.menu_goals_navigation_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.tm_goal_options_meni_dashboard) {
            startActivity(new Intent(this, TM_DashboardActivity.class));
        } else if (itemId == R.id.tm_goal_options_meni_submit) {
            if (canSubmit()) {
                submit();
            } else {
                Toast.makeText(this, "Please fill out all fields before submitting", Toast.LENGTH_SHORT).show();
            }
        } else if (itemId == R.id.tm_goal_options_meni_view) {
            startActivity(
                    new Intent(this, TM_GoalViewActivity.class)
                    .putExtra(TM_GoalViewActivity.GOAL_ID_EXTRA_KEY, m_id)
            );
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        TaskUtils.discardDialouge(this);
    }

    private void submit() {
        DBGoal result = DBGoal.of(getGoal());

        new Thread(() -> {
            Looper.prepare();
            try {
                GoalDB.getInstance().create(getGoal());
                Log.i("DB_EVENT", "Submitted " + result.toString());
                setResult(Activity.RESULT_OK, new Intent().putExtra(RESULT_GOAL_EXTRA_KEY, result));
                runOnUiThread(this::finish);

            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled submission of goal" + result.toString());

            } catch (ExecutionException e) {
                Toast.makeText(this, "Failed to submit goal: Database Error", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Attempted to submit " + result.toString() + ", but failed:\n"
                        + ExceptionUtils.getStackTrace(e));
            }
        }).start();
    }

    private boolean canSubmit() {
        return
                getTitle() != ""
                && m_pickedUnit != null
                && getPriority() != -1;
    }

    private Goal getGoal() {
        Goal res = new Goal(
                getPriority(),
                m_id,
                getTitleText(),
                getDescription(),
                getUnitId()
        );

        if (m_goalToDisplay != null) {
            m_goalToDisplay.getChildrenIds().forEach(res::addChildById);
        }

        return res;
    }


    private String getTitleText() {
        return m_editTitle.getText().toString();
    }

    private int getPriority() {
        return TaskUtils.priorityNum((String)m_spinPriority.getSelectedItem());
    }

    private String getDescription() {
        return m_editDescription.getText().toString();
    }

    private String getUnitId() {
        return m_pickedUnit.getId();
    }


    private void showGoal(Goal goal) {
        m_spinPriority.setSelection(goal.getPriority() + 1);
        m_editTitle.setText(goal.getTitle());
        m_editDescription.setText(goal.getDescription());


        new Thread( () -> {
            Looper.prepare();
            try {
                String unitName = goal.getUnit().getName();
                runOnUiThread(() -> m_textUnit.setText(unitName));
            } catch (InterruptedException | ExecutionException e) {
                Toast.makeText(this, "Failed to retrieve goal unit", Toast.LENGTH_LONG).show();
                Log.w("DB_ERROR", "Tried to retrieve unit for task " + goal.toString()
                        + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Error: failed to identify task unit", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrieve unit of " + goal.toString() + ", but it specifies a unit that does not exist: " + ExceptionUtils.getStackTrace(e));
            }
        }).start();

    }

    private void showGoal(String id) {
        try {
            Looper.prepare();
            m_goalToDisplay = GoalDB.getInstance().awaitGoal(id);
            s_editingIds.add(m_id);
            m_pickedUnit = m_goalToDisplay.getUnit();
            runOnUiThread(() -> showGoal(m_goalToDisplay));
        } catch (ExecutionException e) {
            Toast.makeText(this, "could not retrieve goal, aborting", Toast.LENGTH_LONG).show();
            cancel();
        } catch (InterruptedException e) {
            cancel();
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to edit goal from id " + m_id + ", but there is no such document!");
            Toast.makeText(this, "Could not find the given goal. Aborting.", Toast.LENGTH_SHORT).show();
            cancel();
        }

    }

    private void cancel() {

        if (getCallingActivity() != null) {
            setResult(Activity.RESULT_CANCELED);
        }

        finish();
    }

    public static List<String> getEditingIds() {
        return s_editingIds;
    }
}