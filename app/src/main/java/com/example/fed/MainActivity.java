package com.example.fed;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TFLiteMalwareDetector malwareDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply system bar insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // UI components
        EditText inputFeatures = findViewById(R.id.input_features);
        Button predictButton = findViewById(R.id.predict_button);
        TextView predictionResult = findViewById(R.id.prediction_result);

        // Load the TFLite model
        try {
            malwareDetector = new TFLiteMalwareDetector(this);
        } catch (IOException e) {
            e.printStackTrace();
            predictionResult.setText("Error loading model.");
            return;
        }

        // Set up button click listener for prediction
        predictButton.setOnClickListener(v -> {
            try {
                // Get and parse user input
                String[] inputStrings = inputFeatures.getText().toString().split(",");
                float[] inputValues = new float[inputStrings.length];
                for (int i = 0; i < inputStrings.length; i++) {
                    inputValues[i] = Float.parseFloat(inputStrings[i].trim());
                }

                // Perform prediction
                float prediction = malwareDetector.predict(inputValues);

                // Update UI with prediction result
                predictionResult.setText(prediction > 0.5 ? "Malware Detected" : "App is Safe");
            } catch (Exception e) {
                e.printStackTrace();
                predictionResult.setText("Invalid input. Please enter valid features.");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the TFLite model if loaded
        if (malwareDetector != null) {
            malwareDetector.close();
        }
    }
}