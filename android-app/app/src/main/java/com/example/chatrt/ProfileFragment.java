package com.example.chatrt;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.models.SearchResponse;
import com.example.chatrt.models.User;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private CircleImageView ivAvatar;
    private TextView tvDefaultAvatar;
    private TextView tvDisplayNameHeader, tvBioHeader;
    private EditText etDisplayName, etEmail, etUsername, etPhone, etBio;
    private FloatingActionButton btnChangeAvatar;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        uploadAvatar(selectedImageUri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvDefaultAvatar = view.findViewById(R.id.tvDefaultAvatar);
        tvDisplayNameHeader = view.findViewById(R.id.tvDisplayNameHeader);
        tvBioHeader = view.findViewById(R.id.tvBioHeader);
        etDisplayName = view.findViewById(R.id.etProfileDisplayName);
        etEmail = view.findViewById(R.id.etProfileEmail);
        etUsername = view.findViewById(R.id.etUsername);
        etPhone = view.findViewById(R.id.etPhone);
        etBio = view.findViewById(R.id.etBio);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);

        btnChangeAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        fetchUserData();

        return view;
    }

    private void fetchUserData() {
        if (getContext() == null) return;
        ApiService apiService = ApiClient.getClient(getContext()).create(ApiService.class);
        apiService.fetchMe().enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getUser();
                    if (user != null) {
                        updateUI(user);

                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).updateBottomNavigation(user);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<SearchResponse> call, Throwable t) {}
        });
    }

    private void updateUI(User user) {
        if (!isAdded()) return;

        String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) 
                ? user.getDisplayName() : user.getUsername();
        tvDisplayNameHeader.setText(displayName + " (@" + user.getUsername() + ")");
        tvBioHeader.setText(user.getBio() != null ? user.getBio() : ">>> Dòng giới thiệu mặc định!! <<<");

        etDisplayName.setText(user.getDisplayName());
        etEmail.setText(user.getEmail());
        etUsername.setText(user.getUsername());
        etPhone.setText(user.getPhone());
        etBio.setText(user.getBio());

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvDefaultAvatar.setVisibility(View.GONE);
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .centerCrop()
                    .into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvDefaultAvatar.setVisibility(View.VISIBLE);
            String firstLetter = user.getUsername().substring(0, 1).toUpperCase();
            tvDefaultAvatar.setText(firstLetter);
        }
    }

    private void uploadAvatar(Uri uri) {
        try {
            File file = uriToFile(uri);
            if (file == null) return;

            RequestBody requestFile = RequestBody.create(file, MediaType.parse(requireContext().getContentResolver().getType(uri)));
            // Sử dụng tên trường "file" để khớp với Backend logic
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            ApiService apiService = ApiClient.getClient(getContext()).create(ApiService.class);
            apiService.uploadAvatar(body).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(getContext(), "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();

                        // 1. Cập nhật ảnh ngay lập tức ở Header của Profile
                        String newAvatarUrl = response.body().get("avatarUrl");
                        if (newAvatarUrl != null) {
                            ivAvatar.setVisibility(View.VISIBLE);
                            tvDefaultAvatar.setVisibility(View.GONE);
                            Glide.with(ProfileFragment.this)
                                    .load(newAvatarUrl)
                                    .centerCrop()
                                    .into(ivAvatar);
                        }

                        // 2. Gọi lại fetchUserData để đồng bộ hóa mọi thứ (bao gồm Bottom Navigation)
                        fetchUserData();

                    } else {
                        Toast.makeText(getContext(), "Lỗi upload ảnh: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File uriToFile(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            File file = new File(requireContext().getCacheDir(), "temp_avatar_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}
