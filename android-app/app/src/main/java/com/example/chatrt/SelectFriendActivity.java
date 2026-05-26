package com.example.chatrt;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.models.CreateConversationResponse;
import com.example.chatrt.models.FriendsResponse;
import com.example.chatrt.models.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectFriendActivity extends AppCompatActivity {
    private RecyclerView rvFriends;
    private FriendAdapter adapter;
    private List<User> friendList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        rvFriends = findViewById(R.id.rvFriends);
        if (findViewById(R.id.btnBack) != null) {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        }

        rvFriends.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FriendAdapter(friendList, user -> startChatWithUser(user));
        rvFriends.setAdapter(adapter);

        fetchFriends();
    }

    private void fetchFriends() {
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.getFriendList().enqueue(new Callback<FriendsResponse>() {
            @Override
            public void onResponse(Call<FriendsResponse> call, Response<FriendsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    friendList.clear();
                    friendList.addAll(response.body().getFriends());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(SelectFriendActivity.this, "Không thể tải bạn bè", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<FriendsResponse> call, Throwable t) {
                Toast.makeText(SelectFriendActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startChatWithUser(User user) {
        Map<String, Object> data = new HashMap<>();
        data.put("type", "direct");
        data.put("memberIds", Collections.singletonList(user.getId()));

        ApiService service = ApiClient.getClient(this).create(ApiService.class);
        service.createConversation(data).enqueue(new Callback<CreateConversationResponse>() {
            @Override
            public void onResponse(Call<CreateConversationResponse> call, Response<CreateConversationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Lấy conversation từ trong cái vỏ response
                    String convoId = response.body().getConversation().getId();

                    Intent intent = new Intent(SelectFriendActivity.this, ChatActivity.class);
                    intent.putExtra("CONVERSATION_ID", convoId);
                    intent.putExtra("CHAT_NAME", user.getDisplayName());
                    intent.putExtra("AVATAR_URL", user.getAvatarUrl());
                    startActivity(intent);
                    finish();
                }
            }
            @Override
            public void onFailure(Call<CreateConversationResponse> call, Throwable t) {
                Toast.makeText(SelectFriendActivity.this, "Lỗi tạo chat", Toast.LENGTH_SHORT).show();
            }
        });
    }
}