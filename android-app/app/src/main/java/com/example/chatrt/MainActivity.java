package com.example.chatrt;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.api.SocketManager;
import com.example.chatrt.api.TokenManager;
import com.example.chatrt.models.SearchResponse;
import com.example.chatrt.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "LogoutFlow";
    private TokenManager tokenManager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tokenManager = new TokenManager(this);

        if (tokenManager.getAccessToken() == null) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);
        
        bottomNav = findViewById(R.id.bottom_navigation);
        
        fetchMyInfo();
        SocketManager.getInstance(this).connect();

        bottomNav.setSelectedItemId(R.id.nav_chat);
        loadFragment(new ChatFragment());

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            } else if (id == R.id.nav_notify) {
                selectedFragment = new NotifyFragment();
            } else if (id == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (id == R.id.nav_placeholder) {
                selectedFragment = new PlaceholderFragment();
            } else if (id == R.id.nav_logout) {
                performLogout();
                return true;
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }
            return true;
        });
    }

    private void fetchMyInfo() {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.fetchMe().enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                    User me = response.body().getUser();
                    tokenManager.saveUserId(me.getId());

                    // Cập nhật Avatar và Username lên thanh điều hướng
                    updateBottomNavigation(me);
                }
            }
            @Override public void onFailure(Call<SearchResponse> call, Throwable t) {}
        });
    }

    /**
     * Cập nhật Avatar và Username trên Bottom Navigation.
     * ProfileFragment sẽ gọi hàm này sau khi upload ảnh thành công.
     */
    public void updateBottomNavigation(User me) {
        if (bottomNav == null) return;
        
        MenuItem profileItem = bottomNav.getMenu().findItem(R.id.nav_profile);
        if (profileItem == null) return;

        // 1. Cập nhật tên hiển thị thành username
        profileItem.setTitle(me.getUsername());

        // 2. Tải ảnh đại diện và đặt làm icon
        bottomNav.setItemIconTintList(null); // Tắt Tint để ảnh đại diện hiển thị đúng màu gốc

        if (me.getAvatarUrl() != null && !me.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(me.getAvatarUrl())
                    .circleCrop()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            profileItem.setIcon(new BitmapDrawable(getResources(), resource));
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            profileItem.setIcon(placeholder);
                        }
                    });
        } else {
            // Hiển thị chữ cái đầu nếu không có avatar
            String firstLetter = me.getUsername().substring(0, 1).toUpperCase();
            Bitmap bitmap = createLetterBitmap(firstLetter);
            profileItem.setIcon(new BitmapDrawable(getResources(), bitmap));
        }
    }

    private Bitmap createLetterBitmap(String letter) {
        int size = 96; // Kích thước icon
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Vẽ hình nền tròn tím
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#7C3AED")); // Màu tím giống profile banner
        backgroundPaint.setAntiAlias(true);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, backgroundPaint);

        // Vẽ chữ cái
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(size / 2f);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Căn giữa chữ cái theo chiều dọc
        Rect bounds = new Rect();
        textPaint.getTextBounds(letter, 0, letter.length(), bounds);
        float y = (size / 2f) - bounds.centerY();

        canvas.drawText(letter, size / 2f, y, textPaint);

        return bitmap;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void performLogout() {
        Log.d(TAG, "1. Bắt đầu quá trình Logout");
        SocketManager.getInstance(this).disconnect();
        ApiClient.resetClient();

        ApiClient.getClient(this).create(ApiService.class).signOut().enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                completeLogout();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                completeLogout();
            }
        });
    }

    private void completeLogout() {
        tokenManager.clear();
        goToLogin();
    }

    private void goToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
