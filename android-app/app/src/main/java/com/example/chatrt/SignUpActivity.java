package com.example.chatrt;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.models.AuthResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignUpActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện
    private EditText etFirstName, etLastName, etUsername, etEmail, etPassword;
    private Button btnSignUp;
    private TextView tvGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kết nối file Java này với file giao diện XML
        setContentView(R.layout.activity_signup);

        // 1. Ánh xạ các View từ XML sang Java
        initViews();

        // 2. Xử lý khi nhấn nút Đăng ký
        btnSignUp.setOnClickListener(v -> {
            performSignUp();
        });

        // 3. Xử lý khi nhấn "Đã có tài khoản? Đăng nhập"
        tvGoToLogin.setOnClickListener(v -> {
            finish(); // Đóng màn hình này để quay lại màn hình trước đó (Login)
        });
    }

    private void initViews() {
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
    }

    private void performSignUp() {
        // Lấy dữ liệu người dùng nhập vào
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Kiểm tra cơ bản (Validation)
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo một Map chứa dữ liệu để gửi lên Server (giống như object trong JavaScript)
        Map<String, String> data = new HashMap<>();
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("username", username);
        data.put("email", email);
        data.put("password", password);

        // Gọi API bằng Retrofit
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.signUp(data).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful()) {
                    // Đăng ký thành công
                    Toast.makeText(SignUpActivity.this, "Đăng ký thành công! Hãy đăng nhập.", Toast.LENGTH_LONG).show();
                    finish(); // Quay lại màn hình Login
                } else {
                    // Đăng ký thất bại (ví dụ: trùng username)
                    Toast.makeText(SignUpActivity.this, "Lỗi: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                // Lỗi kết nối mạng hoặc lỗi Server
                Toast.makeText(SignUpActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}