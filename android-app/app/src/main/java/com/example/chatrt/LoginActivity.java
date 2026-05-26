package com.example.chatrt;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.api.TokenManager;
import com.example.chatrt.models.AuthResponse;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private TextView tvUsernameError, tvPasswordError, tvGoToSignUp;
    private Button btnLogin;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        initViews();

        btnLogin.setOnClickListener(v -> performLogin());
        tvGoToSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        tvUsernameError = findViewById(R.id.tvUsernameError);
        tvPasswordError = findViewById(R.id.tvPasswordError);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToSignUp = findViewById(R.id.tvGoToSignUp);
    }

    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        tvUsernameError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);

        boolean isValid = true;
        if (username.length() < 3) { tvUsernameError.setVisibility(View.VISIBLE); isValid = false; }
        if (password.length() < 6) { tvPasswordError.setVisibility(View.VISIBLE); isValid = false; }
        if (!isValid) return;

        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.signIn(credentials).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String accessToken = response.body().getAccessToken();

                    // 1. Lưu Access Token
                    tokenManager.saveAccessToken(accessToken);

                    // 2. GIẢI MÃ TOKEN ĐỂ LẤY USER ID (BƯỚC QUAN TRỌNG NHẤT)
                    String userId = extractUserIdFromToken(accessToken);
                    if (userId != null) {
                        tokenManager.saveUserId(userId);
                        Log.d("LoginActivity", "Đã lưu ID từ Token: " + userId);
                    }

                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm giải mã JWT Token (giống như cách bạn làm ở Web)
    private String extractUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            // Lấy phần giữa (Payload) của Token
            byte[] bytes = Base64.decode(parts[1], Base64.URL_SAFE);
            String payload = new String(bytes, "UTF-8");

            // Dùng GSON để đọc JSON
            JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
            return json.get("userId").getAsString(); // Key "userId" khớp với Backend của bạn
        } catch (Exception e) {
            Log.e("LoginActivity", "Lỗi giải mã token: " + e.getMessage());
            return null;
        }
    }
}
