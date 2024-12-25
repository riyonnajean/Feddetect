package com.example.fed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LogoActivity extends AppCompatActivity {
    EditText edUsername, edPassword;
    Button btn;
    TextView tv, tv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        // Making the activity full screen (edge-to-edge)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Initialize views
        edUsername = findViewById(R.id.editTextLoginName);
        edPassword = findViewById(R.id.editTextLoginPassword);
        btn = findViewById(R.id.buttonLogin);
        tv = findViewById(R.id.textView3);
        tv2 = findViewById(R.id.textViewa);

        // Handle login button click
        btn.setOnClickListener(view -> {
            String username = edUsername.getText().toString();
            String password = edPassword.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please Enter All Details!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Login Success", Toast.LENGTH_SHORT).show();
            }
        });

        // Redirect to Homepage when login button is clicked
        btn.setOnClickListener(view -> startActivity(new Intent(LogoActivity.this, ScanActivity.class)));

        // Redirect to ScanActivity when the second textView (tv2) is clicked
        tv2.setOnClickListener(view -> startActivity(new Intent(LogoActivity.this, ScanActivity.class)));

        // Apply window insets to handle system UI like status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
