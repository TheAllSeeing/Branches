package com.evergreen.treetop.activities.units;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.handlers.UnitDB;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.architecture.utils.UIUtils;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TM_UnitEditorActivity extends AppCompatActivity {

    private EditText m_editTitle;
    private EditText m_editDescription;

    private String m_id;
    private String m_parentId;
    private Unit m_givenUnit;

    private boolean m_new;

    private static final List<String> s_editingIds = new ArrayList<>();

    public static final String PARENT_ID_EXTRA_KEY = "parent-id";
    public static final String UNIT_ID_EXTRA_KEY = "unit-id";
    public static final String RESULT_UNIT_EXTRA_KEY = "result-unit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_editor_tm);

        m_editTitle = findViewById(R.id.tm_unit_editor_edit_title);
        m_editDescription = findViewById(R.id.tm_unit_editor_edit_description);

        m_parentId = getIntent().getStringExtra(PARENT_ID_EXTRA_KEY);
        m_id = getIntent().getStringExtra(UNIT_ID_EXTRA_KEY);

        if (m_id == null) {
            m_id = UnitDB.getInstance().newDoc().getId();
            m_new = true;
        } else {
            m_new = false;
            initEdit();
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
         getMenuInflater().inflate(R.menu.menu_units_editor_options, menu);
         return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.tm_units_options_meni_submit) {
            trySubmit();
        } else if (itemId == R.id.tm_units_options_meni_view) {
            startActivity(
                    new Intent(this, TM_UnitViewActivity.class)
                    .putExtra(TM_UnitViewActivity.UNIT_ID_EXTRA_KEY, m_id)
            );
        }

        return true;
    }

    private void trySubmit() {
        if (canSubmit()) {
            submit();
        } else {
            Toast.makeText(this, "Please fill out unit name before submitting", Toast.LENGTH_LONG).show();
            Log.i("TASK_FAIL", "User tried to submit unit without filling details");
        }
    }

    private boolean canSubmit() {
        return !m_editTitle.getText().toString().equals("");
    }

    private void submit() {

       new Thread(() -> {
           Looper.prepare();
           Unit result = getUnit();

           try {

               if (m_new) {
                   UnitDB.getInstance().create(result);
               } else {
                   UnitDB.getInstance().update(result);
               }

               Log.i("DB_EVENT", "Submitted " + result.toString());
               setResult(Activity.RESULT_OK,
                       new Intent().putExtra(RESULT_UNIT_EXTRA_KEY, DBUnit.of(result))
               );

               finish();
           } catch (InterruptedException e) {
               Log.w("DB_ERROR", "Cancelled submission of unit" + result.toString());
           } catch (ExecutionException e) {
               Log.w("DB_ERROR", "Attempted to submit " + result.toString() + ", but failed:\n"
                       + ExceptionUtils.getStackTrace(e));
               Toast.makeText(this, "Failed to submit unit: Database Error", Toast.LENGTH_SHORT).show();
           } catch (NoSuchDocumentException e) {
               Toast.makeText(this, "Could not identify unit parent; aborting.", Toast.LENGTH_SHORT).show();
               Log.w("DB_ERROR", "Tried to submit a unit, but it specified a non-existent parent: "
               + ExceptionUtils.getStackTrace(e));
               finish();
           }

       }).start();

    }

    private void initEdit() {
        UIUtils.showLoading(this, R.id.tm_unit_editor_constr_layout);
        new Thread(() -> {
            Looper.prepare();
            initUnit(m_id);
            runOnUiThread(() -> {
                showUnit(m_givenUnit);
                UIUtils.hideLoading(this, R.id.tm_unit_editor_constr_layout);
            });
        }).start();
    }

    private void initUnit(String id) {
        try {
            m_givenUnit = UnitDB.getInstance().awaitUnit(id);
            s_editingIds.add(id);
        } catch (ExecutionException e) {
            Log.w("DB_ERROR", "Tried to edit unit from id " + m_id + ", but failed:\n" + ExceptionUtils.getStackTrace(e));
            Toast.makeText(this, "could not retrieve unit, aborting", Toast.LENGTH_LONG).show();
            cancel();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Tried to edit unit from id " + m_id + ", but the action was cancelled.");
            cancel();
        } catch (NoSuchDocumentException e) {
            Log.w("DB_ERROR", "Tried to edit unit from id " + m_id + ", but there is no such document!");
            Toast.makeText(this, "Could not find the given unit. Aborting.", Toast.LENGTH_SHORT).show();
            cancel();
        }

    }

    private void showUnit(Unit unit) {
        m_editTitle.setText(unit.getName());
        m_editDescription.setText(unit.getDescription());
    }


    private Unit getUnit() {
        Unit res = new Unit(
                m_id,
                getName(),
                getDescription(),
                m_new ? UserDB.getInstance().getCurrentUserId() : m_givenUnit.getLeaderId(),
                m_parentId
        );

        getChildrenIds().forEach(res::addChild);
        return res;
    }

    private String getName() {
        return m_editTitle.getText().toString();
    }

    private String getDescription() {
        return m_editDescription.getText().toString();
    }

    private List<String> getChildrenIds() {
        return m_new? new ArrayList<>() : m_givenUnit.getChildrenIds();
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