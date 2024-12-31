package com.example.fed;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {
    private static final String TAG = "ScanActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 123;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.QUERY_ALL_PACKAGES
    };

    private Interpreter tflite;
    private GpuDelegate gpuDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        // Check and request permissions
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        try {
            // Initialize GPU delegate without arguments
            gpuDelegate = new GpuDelegate();

            // Initialize interpreter options with GPU delegate
            Interpreter.Options interpreterOptions = new Interpreter.Options();
            interpreterOptions.addDelegate(gpuDelegate);

            // Load the model and initialize TensorFlow Lite interpreter
            tflite = new Interpreter(loadModelFile("malware_detection_model.tflite"), interpreterOptions);

            // Start scanning in the background
            new ScanAppsTask().execute();
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing GPU delegate", e);
            Toast.makeText(this, "Error initializing GPU delegate", Toast.LENGTH_SHORT).show();
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

                    // Prepare input data
                    float[] input = extractFeatures(permissions, pm.getApplicationLabel(appInfo).toString());

                    // Classify app
                    boolean isMalware = classifyApp(input);
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
            if (malwareApps.isEmpty()) {
                Toast.makeText(ScanActivity.this, "No malware detected!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ScanActivity.this, "Malware detected in apps: " + malwareApps, Toast.LENGTH_LONG).show();
            }
        }
    }

    private float[] extractFeatures(String[] permissions, String appName) {
        // Initialize a feature vector with 18 elements
        float[] features = new float[18]; // Update based on your model's input size
        if (permissions != null) {
            // Iterate over requested permissions and map them to features
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
                    // Add more mappings for other permissions if needed
                }
            }
        }
        // You can add additional features from app data or other sources if needed
        return features;
    }

    private boolean classifyApp(float[] input) {
        // Convert the feature array into a ByteBuffer
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * input.length).order(ByteOrder.nativeOrder());
        for (float value : input) {
            inputBuffer.putFloat(value);
        }

        // Prepare an output buffer to store the result
        ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        // Run the model inference
        tflite.run(inputBuffer, outputBuffer);

        // Retrieve the result from the output buffer
        outputBuffer.rewind();
        float result = outputBuffer.getFloat();
        return result > 0.5; // Threshold for classifying malware
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gpuDelegate != null) {
            gpuDelegate.close(); // Close the GPU delegate when done
        }
        if (tflite != null) {
            tflite.close(); // Close the interpreter when done
        }
    }
}
