package com.evergreen.treetop.activities.units;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.architecture.utils.DBUnit;
import com.evergreen.treetop.ui.adapters.UnitPickerAdapter;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;

public class TM_UnitPickerActivity extends AppCompatActivity {

    public static final String ROOT_UNIT_EXTRA_KEY = "root-unit";
    public static final String USER_IN_FILTER_EXTRA_KEY = "user-in";
    public static final String USER_LEADING_FILTER_EXTRA_KEY = "user-leading";
    public static final String RESULT_PICKED_EXTRA_KEY = "picked-unit";

    private Menu m_menuOptions;

    private static final int ADD_UNIT_MENI_ID = 0;
    RecyclerView m_list;

    private final ActivityResultLauncher<Intent> m_newLauncher = registerForActivityResult(
            new StartActivityForResult(),
            result -> getAdapter().newUnitCallback(result)
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_picker_tm);

        m_list = findViewById(R.id.tm_unit_picker_recycler_picker);

        boolean filterForUser = getIntent().getBooleanExtra(USER_IN_FILTER_EXTRA_KEY, false);
        boolean leadingOnly = getIntent().getBooleanExtra(USER_LEADING_FILTER_EXTRA_KEY, false);
        Serializable dbUnit = getIntent().getSerializableExtra(ROOT_UNIT_EXTRA_KEY);


        new Thread(() -> {
            Looper.prepare();
            try {

                boolean usingUser = filterForUser || leadingOnly;

                User user = usingUser? UserDB.getInstance().getCurrentUser() : null;
                Unit unit = dbUnit != null? Unit.of((DBUnit)dbUnit) : null;

                UnitPickerAdapter adapter = new UnitPickerAdapter(this, user, leadingOnly,  unit);
                runOnUiThread(() -> {
                    m_list.setAdapter(adapter);
                    m_list.setLayoutManager(new LinearLayoutManager(this));
                });
            } catch (ExecutionException e) {
                Toast.makeText(this, "Could not retrieve units, aborting.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Tried to retrive units for picking, but failed:\n" + ExceptionUtils.getStackTrace(e));
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled retrieving units for picking, but failed:\n" + ExceptionUtils.getStackTrace(e));
            }
        }).start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {


        m_menuOptions = menu;

        // If no filters are given, meaning we're coming from dashboard,
        // Add option to add new unit.
        if (!getIntent().getBooleanExtra(USER_IN_FILTER_EXTRA_KEY, false)
                &&  getIntent().getSerializableExtra(USER_IN_FILTER_EXTRA_KEY) == null) {
            MenuItem item = menu.add("New Unit");
            item.setIcon(R.drawable.ic_add);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        m_newLauncher.launch(getAdapter().newUnit());
        return true;
    }

    @Override
    public void onBackPressed() {
        ((UnitPickerAdapter)m_list.getAdapter()).onBackPressed();
    }

    public UnitPickerAdapter getAdapter() {
        return (UnitPickerAdapter)m_list.getAdapter();
    }

    public Menu getOptionsMenu() {
        return m_menuOptions;
    }
}