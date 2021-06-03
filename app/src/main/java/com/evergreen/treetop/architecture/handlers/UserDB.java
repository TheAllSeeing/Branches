package com.evergreen.treetop.architecture.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.evergreen.treetop.activities.users.TM_SignUpActivity;
import com.evergreen.treetop.architecture.Exceptions.NoSuchDocumentException;
import com.evergreen.treetop.architecture.LoggingUtils;
import com.evergreen.treetop.architecture.data.Unit;
import com.evergreen.treetop.architecture.data.User;
import com.evergreen.treetop.architecture.utils.DBUnit.UnitDBKey;
import com.evergreen.treetop.architecture.utils.DBUser;
import com.evergreen.treetop.architecture.utils.DBUser.UserDBKey;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class UserDB {


    private UserDB() {}
    private User m_user;

    private static final UserDB m_instance = new UserDB();
    public static UserDB getInstance() {
        return m_instance;
    }

    public void cacheCurrent() throws InterruptedException, ExecutionException, NoSuchDocumentException {
        try {
            m_user = awaitUser(getCurrentUserId());
        } catch (NoSuchDocumentException e) {
            FirebaseAuth.getInstance().getCurrentUser().delete();
            throw new NoSuchDocumentException(e.getMessage());
        }
    }

    private CollectionReference m_users = FirebaseFirestore.getInstance().collection("users");

    public CollectionReference getRef() {
        return m_users;
    }

    public DocumentReference newDoc() {
        return m_users.document();
    }

    public DocumentReference getUserRef(String id) {
        return m_users.document(id);
    }

    public void joinUnit(Unit unit, boolean leading) throws InterruptedException, ExecutionException, NoSuchDocumentException {

        update(getCurrentUserId(), UserDBKey.UNIT_IDS, FieldValue.arrayUnion(unit.getId()));
        UnitDB.getInstance().update(unit.getId(), UnitDBKey.MEMBER_IDS, FieldValue.arrayUnion(getCurrentUserId()));

        if (leading) {
            update(getCurrentUserId(), UserDBKey.LEADING_IDS, FieldValue.arrayUnion(unit.getId()));
        }

        if (!unit.isRootUnit()) {
            joinUnit(unit.getParent());
        }


        m_user.addUnit(unit.getId(), leading);
    }

    public void joinUnit(Unit unit) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        joinUnit(unit, false);
    }

    public void leaveUnit(Unit unit, String userId) throws ExecutionException, InterruptedException {

        update(userId, UserDBKey.UNIT_IDS, FieldValue.arrayRemove(unit.getId()));
        UnitDB.getInstance().update(unit.getId(), UnitDBKey.MEMBER_IDS, FieldValue.arrayRemove(userId));

        for (Unit child : unit.getChildren()) {
            if (m_user.isIn(child.getId())) {
                leaveUnit(child);
            }
        }

    }

    public void leaveUnit(Unit unit) throws ExecutionException, InterruptedException {
        leaveUnit(unit, getCurrentUserId());
        m_user.removeUnit(unit.getId());
    }

    public void update(String id, UserDBKey field, Object value) throws ExecutionException, InterruptedException {
        Tasks.await(getUserRef(id).update(field.getKey(), value));
    }

    public void update(String id, UserDBKey field, FieldValue value) throws ExecutionException, InterruptedException {
        Tasks.await(getUserRef(id).update(field.getKey(), value));
    }

    public User awaitUser(String id) throws ExecutionException, InterruptedException, NoSuchDocumentException {
        DBUser res = Tasks.await(m_users.document(id).get()).toObject(DBUser.class);

        if (res == null) {
            throw new NoSuchDocumentException("Tried to retrieve user by id " + id
                    + ", but not such user exists!");
        }

        return User.of(res);
    }

    public User getUserByName(String name) throws ExecutionException, InterruptedException {
        return Tasks.await(m_users.whereEqualTo("title", name).get()).getDocuments().get(0).toObject(User.class);
    }

    public User getCurrentUser() {
        return m_user;
    }

    public void registerCurrent() throws ExecutionException, InterruptedException {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        Tasks.await(getUserRef(current.getUid()).set(new DBUser(current.getUid(), current.getDisplayName())));
        m_user = new User(current.getUid(), current.getDisplayName());
    }

    public String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public List<User> getUnitUsers(Unit unitId) throws ExecutionException, InterruptedException {
        return Tasks.await(m_users.whereArrayContains(UserDBKey.UNIT_IDS.getKey(), unitId).get())
                .getDocuments().stream()
                .map(doc -> User.of(doc.toObject(DBUser.class)))
                .collect(Collectors.toList());
    }

    public void logout(Context context) {
        AuthUI.getInstance().signOut(context)
        .addOnSuccessListener(aVoid -> {
            context.startActivity(new Intent(context, TM_SignUpActivity.class));
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Failed to sign out: database error", Toast.LENGTH_SHORT).show();
            Log.w("DB_ERROR",
                    "Failed to sign out ("
                    + LoggingUtils.stringify(getCurrentUser()) + "):\n"
                    + ExceptionUtils.getStackTrace(e));
        });
    }
}
