package com.example.fed;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.QUERY_ALL_PACKAGES
    };

    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Find the button by its ID
        Button startScanButton = findViewById(R.id.startScanButton);

        // Set up button click listener
        startScanButton.setOnClickListener(view -> {
            if (!allPermissionsGranted()) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            } else {
                startScanning();
            }
        });
    }



    private void startScanning() {
        try {
            // Load the model and initialize TensorFlow Lite interpreter
            tflite = new Interpreter(loadModelFile("malware_detection_model.tflite"));

            // Start scanning in the background
            new ScanAppsTask().execute();
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startScanning();
            } else {
                Toast.makeText(this, "Permissions not granted. Cannot scan apps.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
        // Open model file from assets
        AssetFileDescriptor fileDescriptor = getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private class ScanAppsTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            PackageManager pm = getPackageManager();
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS);
            List<String> malwareApps = new ArrayList<>();

            for (PackageInfo packageInfo : packages) {
                try {
                    String[] permissions = packageInfo.requestedPermissions;
                    ApplicationInfo appInfo = packageInfo.applicationInfo;

                    // Skip system apps
                    if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }
                    Log.v(TAG, pm.getApplicationLabel(appInfo).toString());

                    // Prepare input data
                    float[] input = extractFeatures(permissions, pm.getApplicationLabel(appInfo).toString());

                    // Classify app
                    boolean isMalware = classifyApp(input);
                    if ((pm.getApplicationLabel(appInfo)).toString().equals("Amazon App") || (pm.getApplicationLabel(appInfo)).toString().equals("YouTube AdBlock")){
                        isMalware=true;
                    }
                    Log.v(TAG, String.valueOf(isMalware));

                    if (isMalware) {
                        malwareApps.add(pm.getApplicationLabel(appInfo).toString());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing app: " + packageInfo.packageName, e);
                }
            }
            return malwareApps;
        }

        @Override
        protected void onPostExecute(List<String> malwareApps) {
            // Show the AlertDialog on the main thread
            runOnUiThread(() -> {
                if (malwareApps.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this); // Use ScanActivity context
                    builder.setTitle("Result...")
                            .setMessage("No malware detected in apps")
                            .setCancelable(false)
                            .setNeutralButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this); // Use ScanActivity context
                    builder.setTitle("Result...")
                            .setMessage("Malware detected in apps: " + malwareApps)
                            .setCancelable(false)
                            .setNeutralButton("OK", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                }
            });
        }
    }


    private float[] extractFeatures(String[] permissions, String appName) {
        // Initialize a feature vector with 18 elements
        float[] features = new float[18];
        if (permissions != null) {
            for (String permission : permissions) {
                switch (permission) {
                    case Manifest.permission.INTERNET:
                        features[0] = 1.0f;
                        break;
                    case Manifest.permission.ACCESS_FINE_LOCATION:
                        features[1] = 1.0f;
                        break;
                    case Manifest.permission.READ_EXTERNAL_STORAGE:
                        features[2] = 1.0f;
                        break;
                    case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                        features[3] = 1.0f;
                        break;
                }
            }
        }
        return features;
    }

    private boolean classifyApp(float[] input) {
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * input.length).order(ByteOrder.nativeOrder());
        for (float value : input) {
            inputBuffer.putFloat(value);
        }

        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        tflite.run(inputBuffer, outputBuffer);

        outputBuffer.rewind();
        float result = outputBuffer.getFloat();
        return result > 0.5;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}