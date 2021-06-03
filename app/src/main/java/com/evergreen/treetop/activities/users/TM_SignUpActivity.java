package com.evergreen.treetop.activities.users;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.evergreen.treetop.R;
import com.evergreen.treetop.activities.TM_DashboardActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.handlers.UserDB;
import com.evergreen.treetop.test.TestActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class TM_SignUpActivity extends AppCompatActivity {

    private static final Intent m_signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().setRequireName(true).build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            ))
            .setIsSmartLockEnabled(false)
            .setLogo(R.drawable.ic_tree)
            .setTheme(R.style.Theme_Treetop)
            .build();

    private final ActivityResultLauncher<Intent> m_signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), this::handleSignIn);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (TestActivity.TEST) {
            startActivity(new Intent(this, TestActivity.class));
            return;
        }

        Log.d("SIGN_UP", "onCreate");

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            Log.v("DB_EVENT", "Already logged in: " + LoggingUtils.stringify(user));
            new Thread(() -> {
                Looper.prepare();
                tryConnect(user);
            }).start();
        } else {
            Log.d("DB_EVENT", "User is not logged in.");
            launchSignIn();
        }


    }

    private void tryRegister(FirebaseUser user) {
        try {
            UserDB.getInstance().registerCurrent();
            Log.i("DB_EVENT", "New User Registered: User " + user.getUid() + " (" + user.getDisplayName() + ")" );
            launchHomepage();
        } catch (ExecutionException e) {
            Toast.makeText(this, "Failed to register; Database error", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Could not register user " + LoggingUtils.stringify(user) + ":\n" + ExceptionUtils.getStackTrace(e));
            user.delete();
            launchSignIn();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Cancelled registration of " + LoggingUtils.stringify(user) + ":\n" + ExceptionUtils.getStackTrace(e));
            user.delete();
            launchSignIn();
        }
    }

    private void tryConnect(FirebaseUser user) {

        try {
            UserDB.getInstance().cacheCurrent();
            Log.i("DB_EVENT", "Cached user: " + LoggingUtils.stringify(user) );
            launchHomepage();
        } catch (ExecutionException e) {
            Toast.makeText(this, "Failed to cache user; Database error", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", "Could not cache user " + LoggingUtils.stringify(user) + ":\n" + ExceptionUtils.getStackTrace(e));
            launchSignIn();
        } catch (InterruptedException e) {
            Log.w("DB_ERROR", "Cancelled cache of " + LoggingUtils.stringify(user) + ":\n" + ExceptionUtils.getStackTrace(e));
            launchSignIn();
        } catch (NoSuchDocumentException e) {
            Toast.makeText(this,"User does not exist on the database. Aborting.", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR", LoggingUtils.stringify(user) + " exists on " +
                    "FirebaseAuth, but isn't on CloudFirestore!\n" + ExceptionUtils.getStackTrace(e));
            launchSignIn();

        }
    }


    private void handleSignIn(ActivityResult result) {

        IdpResponse resultData = IdpResponse.fromResultIntent(result.getData());

        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                new Thread(() -> {
                    Looper.prepare();
                    if (resultData.isNewUser()) {
                        tryRegister(user);
                    } else {
                        tryConnect(user);
                    }
                }).start();


        } else {
            FirebaseUiException error = resultData.getError();


            if (error != null) {
                Log.w("DB_ERROR", "Logging failed: " + error.getMessage() + "\n" + ExceptionUtils.getStackTrace(error));
                Toast.makeText(
                        this,
                        "Log in failure (error code " + resultData.getError().getErrorCode() + ")",
                        Toast.LENGTH_SHORT
                ).show();
            } else {
                Log.w("DB_ERROR", "Logging failed: result code " + result.getResultCode());

                Toast.makeText(
                        this,
                        "Log in failed (error code " + result.getResultCode() + ")",
                        Toast.LENGTH_SHORT
                ).show();

                launchSignIn();
            }

        }

    }

    private void launchSignIn() {
            m_signInLauncher.launch(m_signInIntent);
                Log.d("UI_EVENT|DB_EVENT", "Redirected to sign in/sign up activity");
    }

    private void launchHomepage() {
        startActivity(new Intent(this, TM_DashboardActivity.class)
                        .putExtra(TM_DashboardActivity.FORBID_BACK_EXTRA_KEY, true)
        );
    }
}