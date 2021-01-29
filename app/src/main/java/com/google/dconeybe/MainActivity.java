package com.google.dconeybe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private final WeakReference<MainActivity> selfRef = new WeakReference<>(this);

    private FirebaseFirestore firestore;
    private Button buttonView;
    private TextView textView;
    private boolean getInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("zzyzx", "MainActivity.onCreate() start");
        super.onCreate(savedInstanceState);
        FirebaseFirestore.setLoggingEnabled(true);
        firestore = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_main);
        buttonView = requireViewById(R.id.button);
        buttonView.setOnClickListener(new ButtonClickListener());
        textView = requireViewById(R.id.text);
        Log.e("zzyzx", "MainActivity.onCreate() done");
    }

    @Override
    protected void onDestroy() {
        Log.e("zzyzx", "MainActivity.onDestroy() start");
        super.onDestroy();
        selfRef.clear();
        Log.e("zzyzx", "MainActivity.onDestroy() done");
    }

    void onButtonClick() {
        if (getInProgress) {
            return;
        }
        firestore.document("/AndroidIssue2311/doc001").get().addOnCompleteListener(new FirestoreGetOnCompleteListener(selfRef));
        textView.setText("Retrieving document...");
        getInProgress = true;
    }

    void onFirestoreGetComplete(Task<DocumentSnapshot> task) {
        getInProgress = false;
        Exception exception = task.getException();
        DocumentSnapshot result = task.getResult();
        if (result != null) {
            textView.setText("Successfully retrieved " + result.getReference().getPath() + "; date=" + result.get("date"));
        } else {
            textView.setText("Failed to retrieve document: " + exception);
        }
    }

    private final class ButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            onButtonClick();
        }
    }

    private static class FirestoreGetOnCompleteListener implements OnCompleteListener<DocumentSnapshot> {

        private final WeakReference<MainActivity> activityRef;

        FirestoreGetOnCompleteListener(WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            Log.e("zzyzx", "FirestoreGetOnCompleteListener.onComplete() task=" + task);
            MainActivity activity = activityRef.get();
            if (activity != null) {
                activity.onFirestoreGetComplete(task);
            }
        }
    }
}