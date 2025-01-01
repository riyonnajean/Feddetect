//package com.example.fed;
//
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import org.tensorflow.lite.Interpreter;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.io.OutputStream;
//import java.nio.MappedByteBuffer;
//import java.nio.channels.FileChannel;
//
//
//public class ServerConnectionActivity extends AppCompatActivity {
//    private static final String TAG = "ServerConnectionActivity";
//    private static final String UPLOAD_URL = "https://yourserver.com/upload_model"; // Replace with actual URL
//
//    private Interpreter tflite;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_scan);
//
//        // Start uploading the model to the server
//        new UploadModelTask().execute(); // <-- Starts the upload task when activity is created
//    }
//
//    // AsyncTask to upload the model to the server
//    private class UploadModelTask extends AsyncTask<Void, Void, Boolean> {
//        @Override
//        protected Boolean doInBackground(Void... voids) {
//            return uploadModel();
//        }
//
//        @Override
//        protected void onPostExecute(Boolean result) {
//            if (result) {
//                Toast.makeText(ServerConnectionActivity.this, "Model uploaded successfully!", Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(ServerConnectionActivity.this, "Failed to upload the model.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    // Method to upload the model to the server
//    private boolean uploadModel() {
//        HttpURLConnection urlConnection = null;
//        FileInputStream fileInputStream = null;
//        OutputStream outputStream = null;
//        try {
//            File modelFile = new File(getFilesDir(), "malware_detection_model_updated.tflite");
//
//            // Prepare connection to the server
//            URL url = new URL(UPLOAD_URL);
//            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setDoOutput(true);
//            urlConnection.setRequestMethod("POST");
//            urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
//
//            // Send the file content
//            fileInputStream = new FileInputStream(modelFile);
//            outputStream = urlConnection.getOutputStream();
//
//            byte[] buffer = new byte[1024];
//            int length;
//            while ((length = fileInputStream.read(buffer)) != -1) {
//                outputStream.write(buffer, 0, length);
//            }
//
//            outputStream.flush();
//
//            // Get the response code to check if upload was successful
//            int responseCode = urlConnection.getResponseCode();
//            return responseCode == HttpURLConnection.HTTP_OK;
//        } catch (IOException e) {
//            Log.e(TAG, "Error uploading model", e);
//            return false;
//        } finally {
//            try {
//                if (fileInputStream != null) {
//                    fileInputStream.close();
//                }
//                if (outputStream != null) {
//                    outputStream.close();
//                }
//                if (urlConnection != null) {
//                    urlConnection.disconnect();
//                }
//            } catch (IOException e) {
//                Log.e(TAG, "Error closing streams", e);
//            }
//        }
//    }
//
//    // Method to load the updated model after downloading it (as in the previous example)
//    private Interpreter loadUpdatedModel() throws IOException {
//        return new Interpreter(loadModelFile("malware_detection_model_updated.tflite"));
//    }
//
//    // Helper method to load model from assets or internal storage
//    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
//        FileInputStream inputStream = openFileInput(modelName);
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = 0;
//        long declaredLength = fileChannel.size();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (tflite != null) {
//            tflite.close(); // Close the interpreter when done
//        }
//    }
//}