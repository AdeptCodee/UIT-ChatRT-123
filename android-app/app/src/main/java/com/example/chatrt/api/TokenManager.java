package com.example.chatrt.api;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "ChatRT_Prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id"; // Khóa để lưu ID của chính bạn

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public TokenManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Hàm mới để lưu cả Token và ID người dùng khi đăng nhập thành công
    public void saveAuthData(String accessToken, String refreshToken, String userId) {
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public void saveAccessToken(String token) {
        editor.putString(KEY_ACCESS_TOKEN, token).apply();
    }

    public void saveUserId(String userId) {
        editor.putString(KEY_USER_ID, userId).apply();
    }

    public String getAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
    }

    // Hàm mới để lấy ID của chính mình (dùng để phân biệt "người kia" trong chat)
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public void clear() {
        // Dùng commit() thay vì apply() để đảm bảo xóa dữ liệu ĐỒNG BỘ ngay lập tức khi logout
        editor.clear().commit();
    }
}