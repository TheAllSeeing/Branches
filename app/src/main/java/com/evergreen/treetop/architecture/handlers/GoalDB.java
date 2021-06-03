package com.evergreen.treetop.architecture.handlers;

import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.data.AppTask;
import com.evergreen.treetop.architecture.data.Goal;
import com.evergreen.treetop.architecture.utils.DBGoal;
import com.evergreen.treetop.architecture.utils.DBGoal.GoalDBKey;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class GoalDB {

    private GoalDB() {}
    private static final GoalDB m_instance = new GoalDB();
    public static GoalDB getInstance() {
        return m_instance;
    }

    private CollectionReference m_goals = FirebaseFirestore.getInstance().collection("goals");

    public CollectionReference getRef() {
        return m_goals;
    }

    public List<Goal> getAll() throws ExecutionException, InterruptedException {
        return  Tasks.await(getRef().get())
                .getDocuments().stream().map(doc -> Goal.of(doc.toObject(DBGoal.class)))
                .collect(Collectors.toList());
    }

    public DocumentReference newDoc() {
        return m_goals.document();
    }

    public Task<DocumentSnapshot> requestGoal(String id) {
        return  m_goals.document(id).get();
    }

    public DocumentReference getGoalRef(String id) {
        return m_goals.document(id);
    }

    public Query tasksWhereEqual(GoalDBKey key, Object value) {
        return m_goals.whereEqualTo(key.getKey(), value);
    }

    public void create(Goal goal) throws ExecutionException, InterruptedException {
        Tasks.await(getGoalRef(goal.getId()).set(DBGoal.of(goal)));
    }

    public Task<Void> update(String id, GoalDBKey key, Object value) {
        return getGoalRef(id).update(key.getKey(), value);
    }

    public Task<Void> delete(Goal goal) throws InterruptedException, ExecutionException, NoSuchDocumentException {

        for (AppTask subtask : goal.getChildren()) {
            TaskDB.getInstance().delete(subtask);
        }

        return getGoalRef(goal.getId()).delete();
    }

    public Goal awaitGoal(String id) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        DBGoal dbGoal = Tasks.await(m_goals.document(id).get()).toObject(DBGoal.class);

        if (dbGoal == null) {
            throw new NoSuchDocumentException("Tried to retrieve task by id " + id
                    + ", but there was no such document!");
        }

        return Goal.of(dbGoal);
    }
}
