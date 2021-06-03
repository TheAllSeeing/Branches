package com.evergreen.treetop.activities.users;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.handlers.UnitDB;
import com.evergreen.treetop.architecture.utils.DBUser;
import com.evergreen.treetop.architecture.utils.UIUtils;
import com.evergreen.treetop.ui.adapters.UserPickerAdapter;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TM_UserPickerActivity extends AppCompatActivity {

    private final static int SUBMIT_BUTTON_ID = 0;

    public final static String UNIT_ID_EXTRA_KEY = "unit-id";
    public final static String INIT_SELECTED_EXTRA_KEY = "init-selected";
    public final static String RESULT_SELECTED_EXTRA_KEY = "selected-users";

    private RecyclerView m_listUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_picker_tm);

        String unitId = getIntent().getStringExtra(UNIT_ID_EXTRA_KEY);
        List<String> initSelected = getIntent().getStringArrayListExtra(INIT_SELECTED_EXTRA_KEY);

        if (initSelected == null) {
            initSelected = new ArrayList<>();
        }

        if (unitId == null) {
            Toast.makeText(this, "Unit id not given. Aborting", Toast.LENGTH_SHORT).show();
            Log.w("UI_ERROR", "Started UserPickerActivity with no unit given");
            finish();
            return;
        }

        m_listUsers = findViewById(R.id.tm_user_picker_recycler_picker);
        UserPickerAdapter adapter = new UserPickerAdapter(this);

        m_listUsers.setAdapter(adapter);
        m_listUsers.setLayoutManager(new LinearLayoutManager(this));


        // Dumb lambda workaround. z
        final List<String> finalInitSelected = initSelected;

        new Thread(() -> {
            UIUtils.showLoading(this);
            try {
                adapter.init(UnitDB.getInstance().awaitUnit(unitId), finalInitSelected);
                runOnUiThread(() -> UIUtils.hideLoading(this, m_listUsers.getId()));
            } catch (ExecutionException e) {
                Toast.makeText(this, "Could not retrieve unit users. Aborting.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Failed to retrieve unit for UnitPicker, or the unit's members:\n" + ExceptionUtils.getStackTrace(e));
                finish();
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled retrieval of unit for UnitPicker, or of the unit's members:\n" + ExceptionUtils.getStackTrace(e));
            } catch (NoSuchDocumentException e) {
                Toast.makeText(this, "Could not locate unit users. Aborting.", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Failed to identify unit for UnitPicker, or the unit's members:\n" + ExceptionUtils.getStackTrace(e));
                finish();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, SUBMIT_BUTTON_ID, Menu.NONE, "Submit")
        .setIcon(R.drawable.ic_confirm)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        List<DBUser> res = ((UserPickerAdapter)m_listUsers.getAdapter())
                .getSelected()
                .stream()
                .map(DBUser::of)
                .collect(Collectors.toList());

        setResult(
                RESULT_OK,
                new Intent().putExtra(
                        RESULT_SELECTED_EXTRA_KEY,
                        new ArrayList<>(res)
                )
        );

        finish();

        return true;

    }

}