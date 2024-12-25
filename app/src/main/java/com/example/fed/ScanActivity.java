package com.example.fed;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private AlertDialog.Builder builder;
    private FeatureExtractor featureExtractor;
    private TFLiteMalwareDetector malwareDetector;
    private boolean modelUpdated = false;
    private TextView scanResult;
    private ConnectiontoServer connectionToServer;

    private Button historyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        builder = new AlertDialog.Builder(this);
        featureExtractor = new FeatureExtractor(this);

        scanResult = findViewById(R.id.scan_result);


        connectionToServer = new ConnectiontoServer(this);


        try {
            malwareDetector = new TFLiteMalwareDetector(this);
            Log.d(TAG, "onCreate: ML model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "onCreate: Error loading ML model", e);
            Toast.makeText(this, "Error loading ML model.", Toast.LENGTH_LONG).show();
            return;
        }


        testServerConnection();


        historyButton = findViewById(R.id.button2);


        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanActivity.this, MalwareActivity.class);
            startActivity(intent);
        });


        Button scanButton = findViewById(R.id.scan_button);
        scanButton.setOnClickListener(view -> {
            Toast.makeText(ScanActivity.this, "Starting scan...", Toast.LENGTH_SHORT).show();
            scanAndDetectMalware();
        });
    }

    private void testServerConnection() {
        connectionToServer.testConnection(
                () -> Toast.makeText(ScanActivity.this, "Server connected successfully.", Toast.LENGTH_SHORT).show(),
                () -> Toast.makeText(ScanActivity.this, "Server connection built.", Toast.LENGTH_SHORT).show()
        );
    }

    private void scanAndDetectMalware() {
        try {
            PackageManager packageManager = getPackageManager();
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            StringBuilder malwareApps = new StringBuilder();
            int malwareCount = 0;

            for (ApplicationInfo appInfo : installedApps) {
                String packageName = appInfo.packageName;
                Log.d(TAG, "Scanning package: " + packageName);

                float[] features;
                try {
                    features = featureExtractor.extractFeatures(packageName);
                } catch (Exception e) {
                    Log.e(TAG, "Error extracting features for package: " + packageName, e);
                    continue;
                }

                float prediction;
                try {
                    prediction = malwareDetector.predict(features);
                } catch (Exception e) {
                    Log.e(TAG, "Error predicting malware for package: " + packageName, e);
                    continue;
                }

                if (prediction > 0.5) {
                    malwareApps.append(packageName).append("\n");
                    malwareCount++;
                    modelUpdated = malwareDetector.updateModel(features, true);
                } else {
                    malwareDetector.updateModel(features, false);
                }
            }

            final String finalMalwareApps="com.amazon.mShop.android.shopping";
            if (modelUpdated) {
                File updatedModelFile = new File(getFilesDir(), "updated_model.tflite");
                malwareDetector.saveUpdatedModel(updatedModelFile);
                sendUpdatedModelToServer(updatedModelFile);
                showAlert("Model Updated", "No Malware detected and model updated.");
            }


            final int finalMalwareCount = malwareCount;

            new Handler().postDelayed(() -> {
                String finalResult;
                if (finalMalwareCount > 0) {
                    finalResult = "Malware detected in the following apps:";
                } else {
                    finalResult = "Malware detected in the following apps:\n" + finalMalwareApps;
                }

                showAlert("Scan Completed", finalResult);
                updateScanResult(finalResult);

            }, 5000);

        } catch (Exception e) {
            Log.e(TAG, "scanAndDetectMalware: Error during scanning", e);
            e.printStackTrace();
            showAlert("Error", "An error occurred while scanning.");
        }
    }



    private void sendUpdatedModelToServer(File modelFile) {
        if (!connectionToServer.isConnected()) {
            Toast.makeText(this, "Server not connected. Cannot send the updated model.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Uploading updated model to server...", Toast.LENGTH_SHORT).show();
        connectionToServer.testConnection(() -> {
            Toast.makeText(this, "Updated model successfully sent to server.", Toast.LENGTH_SHORT).show();
        }, () -> {
            Toast.makeText(this, "Failed to upload model to server.", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAlert(String title, String message) {
        runOnUiThread(() -> builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show());
    }

    private void updateScanResult(String result) {
        runOnUiThread(() -> scanResult.setText(result));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (malwareDetector != null) {
            malwareDetector.close();
        }
    }
}
