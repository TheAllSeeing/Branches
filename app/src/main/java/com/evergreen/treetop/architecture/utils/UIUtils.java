package com.evergreen.treetop.architecture.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.evergreen.treetop.R;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.handlers.GoalDB;
import com.evergreen.treetop.architecture.handlers.TaskDB;
import com.evergreen.treetop.architecture.handlers.UnitDB;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.ExecutionException;

public class UIUtils {



    public static void setBackgroundColor(Context context, View view, int colorId) {
        Drawable background = view.getBackground();
        if (background instanceof ShapeDrawable) {
            // cast to 'ShapeDrawable'
            ShapeDrawable shapeDrawable = (ShapeDrawable) background;
            shapeDrawable.getPaint().setColor(ContextCompat.getColor(context, colorId));
        } else if (background instanceof GradientDrawable) {
            // cast to 'GradientDrawable'
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(ContextCompat.getColor(context, colorId));
        } else if (background instanceof ColorDrawable) {
            // alpha value may need to be set again after this call
            ColorDrawable colorDrawable = (ColorDrawable) background;
            colorDrawable.setColor(ContextCompat.getColor(context, colorId));
        }
    }


    /**
     * @param view         View to animate
     * @param toVisibility Visibility at the end of animation
     * @param toAlpha      Alpha at the end of animation
     * @param duration     Animation duration in ms
     */
    public static void animateView(final View view, final int toVisibility, float toAlpha, int duration) {
        float alpha = 0;

        if (toVisibility == View.VISIBLE) {
            alpha = toAlpha;
            view.setAlpha(alpha);
        }

        view.setVisibility(View.VISIBLE);
        view.animate()
                .setDuration(duration)
                .alpha(alpha)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(toVisibility);
                    }
                });
    }

    public static void showLoading(Activity context) {
        animateView(context.findViewById(R.id.loading_overlay), View.VISIBLE, 0.4f, 200);
    }

    public static void showLoading(Activity context, int mainViewId) {
        showLoading(context);
        animateView(context.findViewById(mainViewId), View.VISIBLE, 0.4f, 200);
    }

    public static void hideLoading(Activity context) {
        animateView(context.findViewById(R.id.loading_overlay), View.GONE, 0.4f, 200);
    }

    public static void hideLoading(Activity context, int mainViewId) {
        hideLoading(context);
        animateView(context.findViewById(mainViewId), View.VISIBLE, 1, 200);
    }

    public static void deleteTaskDialouge(Activity context, AppTask taskToDelete) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setMessage("Are you sure you want to delete the task?\n" +
                "This action is permanent and cannot be undone.");
        alertBuilder.setPositiveButton("Yes", (dialog, which) -> {
            new Thread(() -> {
                Looper.prepare();
                try {
                    TaskDB.getInstance().delete(taskToDelete);
                    context.runOnUiThread(context::finish);
                } catch (InterruptedException e) {
                    Log.w("DB_ERROR", "Cancelled deletion of " + taskToDelete.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
                } catch (ExecutionException | NoSuchDocumentException e) {
                    Toast.makeText(context, "Failed to delete task; database error", Toast.LENGTH_SHORT).show();
                    Log.w("DB_ERROR", "Failed to delete " + taskToDelete.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
                }
            }).start();
        });
        alertBuilder.setNegativeButton("No", null);
        alertBuilder.create().show();
    }


    public static void deleteGoalDialouge(Activity context, Goal goal) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setMessage("Are you sure you want to delete the task?\n" +
                "This action is permanent and cannot be undone.");
        alertBuilder.setPositiveButton("Yes", (dialog, which) -> {
            try {
                GoalDB.getInstance().delete(goal);
                context.finish();
            } catch (InterruptedException e) {
                Log.w("DB_ERROR", "Cancelled deletion of " + goal.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
            } catch (ExecutionException | NoSuchDocumentException e) {
                Toast.makeText(context, "Failed to delete task; database error", Toast.LENGTH_SHORT).show();
                Log.w("DB_ERROR", "Failed to delete " + goal.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
            }
        });
        alertBuilder.setNegativeButton("No", null);
        alertBuilder.create().show();
    }



    public static void deleteUnitDialouge(Activity context, Unit unit) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);

        alertBuilder.setMessage("Are you sure you want to delete the task?\n" +
                "This action is permanent and cannot be undone.\n\n" +
                "This will also delete any child units you have created, and leave all " +
                "of the unit's tasks without a unit."
        );

        alertBuilder.setPositiveButton("Yes", (dialog, which) -> {
            new Thread(() -> {
                Looper.prepare();
                try {
                    UnitDB.getInstance().delete(unit);
                    context.runOnUiThread(context::finish);
                } catch (InterruptedException e) {
                    Log.w("DB_ERROR", "Cancelled deletion of " + unit.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
                } catch (ExecutionException  e) {
                    Toast.makeText(context, "Failed to delete unit: database error", Toast.LENGTH_SHORT).show();
                    Log.w("DB_ERROR", "Failed to delete " + unit.toString() + ":\n" + ExceptionUtils.getStackTrace(e));
                }
            }).start();
        });
        alertBuilder.setNegativeButton("No", null);
        alertBuilder.create().show();
    }


    public static void setProgressColor(ProgressBar view, int color) {
        view.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
        view.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void setBackgroundColor(View view, int color) {
        view.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public static void animateProgress(ProgressBar view, int to) {
        ObjectAnimator animation = ObjectAnimator.ofInt(view, "progress", view.getProgress(), to); // see this max value coming back here, we animate towards that value
        animation.setDuration(5000); // in milliseconds
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }
}
