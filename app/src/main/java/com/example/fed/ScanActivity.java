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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
            copyModelToInternalStorage("malware_detection_model.tflite");
            tflite = new Interpreter(loadModelFile("malware_detection_model.tflite"));

            // Start scanning in the background
            new ScanAppsTask().execute();
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyModelToInternalStorage(String modelName) throws IOException {
        File outputFile = new File(getFilesDir(), modelName);

        if (!outputFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(getAssets().openFd(modelName).getFileDescriptor());
                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }
            Log.d(TAG, "Model copied to internal storage: " + outputFile.getAbsolutePath());
        } else {
            Log.d(TAG, "Model already exists in internal storage: " + outputFile.getAbsolutePath());
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
                            .setNeutralButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .create()
                            .show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScanActivity.this); // Use ScanActivity context
                    builder.setTitle("Result...")
                            .setMessage("Malware detected in apps: " + malwareApps)
                            .setCancelable(false)
                            .setNeutralButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                // Upload the model after user clicks "OK"
                                new UploadModelTask().execute();
                            })
                            .create()
                            .show();
                }
            });
        }

    }
    public class UploadModelTask extends AsyncTask<Void, Void, Boolean> {
        private static final String TAG = "UploadModelTask";
        private static final String SERVER_URL = "https://c01e-103-89-232-66.ngrok-free.app/upload";
        private final MediaType MEDIA_TYPE_TFLITE = MediaType.parse("application/octet-stream");
    @Override
    protected Boolean doInBackground(Void... voids) {
        File modelFile = new File(getFilesDir(), "malware_detection_model.tflite");

        if (!modelFile.exists()) {
            Log.e(TAG, "Model file does not exist");
            return false;
        }

        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(modelFile, MEDIA_TYPE_TFLITE);
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", modelFile.getName(), fileBody)
                .addFormDataPart("client_id", "example_client_id")
                .build();

        Request request = new Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            Log.e(TAG, "Error uploading model", e);
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
            Toast.makeText(ScanActivity.this, "Model uploaded successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ScanActivity.this, "Failed to upload model", Toast.LENGTH_SHORT).show();
        }
    }

    // Ensure getFilesDir() is accessible
    private File getFilesDir() {
        // Replace with appropriate context method if needed
        return ScanActivity.this.getFilesDir();
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