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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final WeakReference<MainActivity> selfRef = new WeakReference<>(this);

    private FirebaseFirestore firestore;
    private Button documentGetButtonView;
    private Button collectionGetButtonView;
    private Button createDocumentsButtonView;
    private TextView textView;
    private boolean getInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e("zzyzx", "MainActivity.onCreate() start");
        super.onCreate(savedInstanceState);
        FirebaseFirestore.setLoggingEnabled(true);
        firestore = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_main);
        documentGetButtonView = requireViewById(R.id.document_get_button);
        documentGetButtonView.setOnClickListener(this);
        collectionGetButtonView = requireViewById(R.id.collection_get_button);
        collectionGetButtonView.setOnClickListener(this);
        createDocumentsButtonView = requireViewById(R.id.documents_create_button);
        createDocumentsButtonView.setOnClickListener(this);
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

    @Override
    public void onClick(View view) {
        if (view == documentGetButtonView) {
            onDocumentGetButtonClick();
        } else if (view == collectionGetButtonView) {
            onCollectionGetButtonClick();
        } else if (view == createDocumentsButtonView) {
            onCreateDocumentsButtonClick();
        } else {
            throw new IllegalArgumentException("unknown view: " + view);
        }
    }

    void onDocumentGetButtonClick() {
        if (getInProgress) {
            return;
        }
        firestore.document("/AndroidIssue2311/doc001").get().addOnCompleteListener(new DocumentGetOnCompleteListener(selfRef));
        textView.setText("Retrieving document...");
        getInProgress = true;
    }

    void onDocumentGetComplete(Task<DocumentSnapshot> task) {
        getInProgress = false;
        Exception exception = task.getException();
        DocumentSnapshot result = task.getResult();
        if (result != null) {
            if (!result.exists()) {
                textView.setText("Document " + result.getReference().getPath() + " does not exist");
            } else {
                textView.setText("Successfully retrieved " + result.getReference().getPath() + "; date=" + result.get("date"));
            }
        } else {
            textView.setText("Failed to retrieve document: " + exception);
        }
    }

    void onCollectionGetButtonClick() {
        if (getInProgress) {
            return;
        }
        firestore.collection("/AndroidIssue2311").whereGreaterThanOrEqualTo("index", 0).get().addOnCompleteListener(new CollectionGetOnCompleteListener(selfRef));
        textView.setText("Retrieving collection...");
        getInProgress = true;
    }

    void onCollectionGetComplete(Task<QuerySnapshot> task) {
        getInProgress = false;
        Exception exception = task.getException();
        QuerySnapshot result = task.getResult();
        if (result != null) {
            textView.setText("Successfully retrieved " + result.size() + " documents");
        } else {
            textView.setText("Failed to retrieve collection: " + exception);
        }
    }

    void onCreateDocumentsButtonClick() {
        if (getInProgress) {
            return;
        }
        int numDocumentsToCreate = 50;
        textView.setText("Creating " + numDocumentsToCreate + " documents...");
        CollectionReference collection = firestore.collection("/AndroidIssue2311");
        DocumentCreateOnCompleteListener completionListener = new DocumentCreateOnCompleteListener(selfRef, numDocumentsToCreate);
        for (int i=0; i<numDocumentsToCreate; i++) {
            String nameIndex = Integer.toString(i);
            while (nameIndex.length() < 3) {
                nameIndex = "0" + nameIndex;
            }
            DocumentReference doc = collection.document("doc" + nameIndex);

            HashMap<String, Object> data = new HashMap<>();
            data.put("index", i);
            data.put("date", new Date().toString());
            doc.set(data).addOnCompleteListener(completionListener);
        }
        getInProgress = true;
    }

    void onDocumentCreateComplete(int successCount, int failCount, int expectedCount, String errorMessage) {
        if (errorMessage != null) {
            Log.e("zzyzx", "Failed to create document: " + errorMessage);
        }
        if (successCount + failCount == expectedCount) {
            getInProgress = false;
            if (failCount == 0) {
                textView.setText("Document creation complete: " + successCount + " documents created");
            } else {
                textView.setText("Document creation complete: " + successCount + " documents created, " + failCount + " documents FAILED to be created");
            }
        } else if (failCount == 0) {
            textView.setText(successCount + " documents created");
        } else if (successCount == 0) {
            textView.setText(failCount + " documents FAILED to be created");
        } else {
            textView.setText(successCount + " documents created, " + failCount + " documents FAILED to be created");
        }
    }

    private static class DocumentGetOnCompleteListener implements OnCompleteListener<DocumentSnapshot> {

        private final WeakReference<MainActivity> activityRef;

        DocumentGetOnCompleteListener(WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                activity.onDocumentGetComplete(task);
            }
        }
    }

    private static class CollectionGetOnCompleteListener implements OnCompleteListener<QuerySnapshot> {

        private final WeakReference<MainActivity> activityRef;

        CollectionGetOnCompleteListener(WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void onComplete(@NonNull Task<QuerySnapshot> task) {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                activity.onCollectionGetComplete(task);
            }
        }
    }

    private static class DocumentCreateOnCompleteListener implements OnCompleteListener<Void> {

        private final WeakReference<MainActivity> activityRef;
        private final int numDocumentsToCreate;
        private int successCount = 0;
        private int failCount = 0;

        DocumentCreateOnCompleteListener(WeakReference<MainActivity> activityRef, int numDocumentsToCreate) {
            this.activityRef = activityRef;
            this.numDocumentsToCreate = numDocumentsToCreate;
        }

        @Override
        public void onComplete(@NonNull Task<Void> task) {
            MainActivity activity = activityRef.get();
            if (activity != null) {
                String errorMessage;
                if (task.isSuccessful()) {
                    successCount++;
                    errorMessage = null;
                } else {
                    failCount++;
                    Exception exception = task.getException();
                    errorMessage = (exception == null) ? null : exception.toString();
                }
                activity.onDocumentCreateComplete(successCount, failCount, numDocumentsToCreate, errorMessage);
            }
        }
    }
}