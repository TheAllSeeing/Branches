package com.evergreen.treetop.architecture.handlers;

import androidx.annotation.NonNull;

import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.utils.DBGoal.GoalDBKey;
import com.evergreen.treetop.architecture.utils.DBTask;
import com.evergreen.treetop.architecture.utils.DBTask.TaskDBKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class TaskDB {

    private TaskDB() {}
    private static final TaskDB m_instance = new TaskDB();
    public static TaskDB getInstance() {
        return m_instance;
    }

    private CollectionReference m_tasks = FirebaseFirestore.getInstance().collection("tasks");

    public CollectionReference getRef() {
        return m_tasks;
    }

    public DocumentReference getTaskRef(String id) {
        return m_tasks.document(id);
    }

    public DocumentReference newDoc() {
        return m_tasks.document();
    }

    public Void update(String id, TaskDBKey key, Object value) throws ExecutionException, InterruptedException {
        return Tasks.await(getTaskRef(id).update(key.getKey(), value));
    }

    public Task<Void> update(String id, TaskDBKey key, FieldValue value) {
        return getTaskRef(id).update(key.getKey(), value);
    }

    public void delete(AppTask task) throws InterruptedException, ExecutionException, NoSuchDocumentException {

        for (AppTask subtask : task.getChildren()) {
            delete(subtask);
        }

        Tasks.await(getTaskRef(task.getId()).delete());

        if (task.isRootTask()) {
            Tasks.await(GoalDB.getInstance().update(task.getParentId(), GoalDBKey.SUBTASK_IDS, FieldValue.arrayRemove(task.getId())));
        } else {
            Tasks.await(update(task.getParentId(), TaskDBKey.SUBTASK_IDS, FieldValue.arrayRemove(task.getId())));
        }
    }

    @NonNull
    public AppTask awaitTask(String id) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        DBTask dbTask = Tasks.await(m_tasks.document(id).get()).toObject(DBTask.class);

        if (dbTask == null) {
            throw new NoSuchDocumentException("Tried to retrieve task by id " + id
                    + ", but there was no such document!");
        }

        return AppTask.of(dbTask);
    }

    public List<AppTask> getRootTasks() throws ExecutionException, InterruptedException {
        return Tasks.await(m_tasks.whereEqualTo(TaskDBKey.IS_ROOT.getKey(), true).get())
                .getDocuments().stream().map(doc -> AppTask.of(doc.toObject(DBTask.class)))
                .collect(Collectors.toList());
    }

    public Task<DocumentSnapshot> requestTask(String id) {
        return  m_tasks.document(id).get();
    }

}
