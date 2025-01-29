package com.example.datn_tuan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        // Khởi tạo Firebase trước khi sử dụng bất kỳ tính năng nào
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");

            // Cấu hình App Check
            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
            Log.d(TAG, "App Check initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: ", e);
        }

        mAuth = FirebaseAuth.getInstance();

        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.btn_login);
        TextView createAccountTextView = findViewById(R.id.createAccountTextView);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            Log.d(TAG, "Attempting login with email: " + email);
            // Không nên log password trong production

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading indicator
            // TODO: Thêm ProgressDialog hoặc ProgressBar ở đây

            // Authenticate user
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        Log.d(TAG, "signInWithEmail:complete");

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = task.getException() != null ?
                                    task.getException().getMessage() :
                                    "Đăng nhập thất bại";
                            Toast.makeText(this, "Đăng nhập thất bại: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }

                        // Hide loading indicator
                        // TODO: Ẩn ProgressDialog hoặc ProgressBar ở đây
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Login error: ", e);
                        Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        createAccountTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in: " + mAuth.getCurrentUser().getEmail());
            // Người dùng đã đăng nhập, chuyển đến MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}