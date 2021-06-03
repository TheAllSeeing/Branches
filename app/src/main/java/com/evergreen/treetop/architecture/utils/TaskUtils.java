package com.evergreen.treetop.architecture.utils;

import android.app.Activity;

import androidx.appcompat.app.AlertDialog;

import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.data.User;

import java.time.LocalDate;
import java.time.Period;

public class TaskUtils {
    private TaskUtils() {}

    public static String priorityChar(int priority) {

        switch (priority) {
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            case 3:
                return "D";
            case 4:
                return "E";
            default:
                return null;
        }
    }


    public static int priorityNum(String charc) {
        switch (charc) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            case "D":
                return 3;
            case "E":
                return 4;
            default:
                return -1;
        }
    }



    public static Goal dummyGoal(String id) {

        Goal goal = new Goal(0, id, "Test Goal " + id, "this is a test goal", "TestUnit");
        goal.addChildById("hello");
        goal.addChildById("test");
        goal.addChildById("Long one");
        goal.addChildById("and");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");
        goal.addChildById("see?");

        return goal;
    }

    public static AppTask dummyTask(String id) {

        Goal goal = new Goal(0, id + "-parent", "Test Goal", "this is a test goal", "TestUnit");
        AppTask task =
                new AppTask(0,
                        id,
                        "Test Task: " + id,
                        "this is a test task",
                        "TestUnit",
                        "test-id",
                        LocalDate.now().plus(Period.of(0, 0, 1)),
                        LocalDate.now().plus(Period.of(0, 0, 3)),
                        "Test assigner",
                        "test-goal",
                        "test-root"
                );
        task.setCompleted(task.getId().length() > 5);
        task.addChildById("hello");
        task.addChildById("test");
        task.addChildById("Long one");
        task.addChildById("and");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");
        task.addChildById("see?");

        task.addAssignee(new User("ID1", "Test Assignee 1"));
        task.addAssignee(new User("ID2", "Test Assignee 2"));

        return task;
    }


    public static void discardDialouge(Activity context) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setMessage("Discard Changes?");
        alertBuilder.setPositiveButton("Yes", (dialog, which) -> {
            context.setResult(Activity.RESULT_CANCELED);
            context.finish();
        });
        alertBuilder.setNegativeButton("No", null);
        alertBuilder.create().show();
    }





}
