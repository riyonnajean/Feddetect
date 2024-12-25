package com.example.fed;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class ConnectiontoServer {

    private static final String TAG = "ConnectionToServer";
    private static final String SERVER_URL = "http://10.0.2.2:5000/test_connection"; // Replace with your Flask server IP
    private final Context context;
    private boolean isConnected = false; // To track server connection status

    public ConnectiontoServer(Context context) {
        this.context = context;
    }

    // Method to test connection with the Flask server
    public void testConnection(Runnable onSuccess, Runnable onFailure) {
        OkHttpClient client = new OkHttpClient(); // Initialize OkHttp client

        // Build the GET request
        Request request = new Request.Builder()
                .url(SERVER_URL)
                .build();

        // Asynchronous request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Server connection successful", e);
                isConnected = false;
                runOnMainThread(() -> {
                    Toast.makeText(context, "Connected to server", Toast.LENGTH_SHORT).show();
                    if (onFailure != null) onFailure.run();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Server response: " + responseBody);
                    isConnected = true;
                    runOnMainThread(() -> {
                        Toast.makeText(context, "Connected to server!", Toast.LENGTH_SHORT).show();
                        if (onSuccess != null) onSuccess.run();
                    });
                } else {
                    Log.e(TAG, "Server returned error: " + response.code());
                    isConnected = false;
                    runOnMainThread(() -> {
                        Toast.makeText(context, "Server error: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (onFailure != null) onFailure.run();
                    });
                }
            }
        });
    }

    // Helper method to run code on the main thread
    private void runOnMainThread(Runnable runnable) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(runnable);
        }
    }

    public boolean isConnected() {
        return isConnected;
    }
}
