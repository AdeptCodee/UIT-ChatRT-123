package com.example.chatrt;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatrt.R;
import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.api.SocketManager;
import com.example.chatrt.api.TokenManager;
import com.example.chatrt.models.Conversation;
import com.example.chatrt.models.ConversationsResponse;
import com.example.chatrt.models.CreateConversationResponse;
import com.example.chatrt.models.FriendsResponse;
import com.example.chatrt.models.SearchResponse;
import com.example.chatrt.models.User;
import com.google.android.flexbox.FlexboxLayout;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";
    private RecyclerView rvGroupChats, rvDirectChats;
    private ConversationAdapter groupAdapter, directAdapter;
    private final List<Conversation> groupList = new ArrayList<>();
    private final List<Conversation> directList = new ArrayList<>();

    private SocketManager.OnOnlineUsersChangedListener onlineListener;
    private SocketManager.ConversationUpdateListener convoListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        
        rvGroupChats = view.findViewById(R.id.rvGroupChats);
        rvDirectChats = view.findViewById(R.id.rvDirectChats);

        setupRecyclerViews();

        View btnNewMessage = view.findViewById(R.id.btnNewMessage);
        if (btnNewMessage != null) {
            btnNewMessage.setOnClickListener(v ->
                    startActivity(new Intent(getContext(), SelectFriendActivity.class)));
        }

        // Nút thêm bạn bè
        View btnAddFriend = view.findViewById(R.id.btnAddFriend);
        if (btnAddFriend != null) {
            btnAddFriend.setOnClickListener(v -> showAddFriendDialog());
        }

        // Nút thêm nhóm chat
        View btnAddGroup = view.findViewById(R.id.btnAddGroup);
        if (btnAddGroup != null) {
            btnAddGroup.setOnClickListener(v -> showCreateGroupDialog());
        }

        // THIẾT LẬP SOCKET
        SocketManager sm = SocketManager.getInstance(getContext());
        sm.connect();

        onlineListener = onlineIds -> {
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    if (directAdapter != null) directAdapter.notifyDataSetChanged();
                    if (groupAdapter != null) groupAdapter.notifyDataSetChanged();
                });
            }
        };
        sm.addOnlineUsersListener(onlineListener);

        convoListener = data -> {
            Log.d(TAG, "Socket nhận update: " + data.toString());
            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    try {
                        JSONObject convoJson = data.getJSONObject("conversation");
                        if (data.has("unreadCounts")) {
                            convoJson.put("unreadCounts", data.get("unreadCounts"));
                        }
                        Conversation updatedConvo = new Gson().fromJson(convoJson.toString(), Conversation.class);
                        updateListWithNewConvo(updatedConvo);
                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi xử lý Socket update: " + e.getMessage());
                    }
                });
            }
        };
        sm.addConvoListener(convoListener);

        return view;
    }

    private void showCreateGroupDialog() {
        final Context context = getContext();
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_group, null);
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final EditText etGroupName = dialogView.findViewById(R.id.etGroupName);
        final EditText etSearchMember = dialogView.findViewById(R.id.etSearchMember);
        final RecyclerView rvSuggestions = dialogView.findViewById(R.id.rvSuggestions);
        final FlexboxLayout flexSelectedMembers = dialogView.findViewById(R.id.flexSelectedMembers);
        View btnCreateGroup = dialogView.findViewById(R.id.btnCreateGroup);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        final List<User> allFriends = new ArrayList<>();
        final List<User> selectedMembers = new ArrayList<>();
        final List<User> suggestionList = new ArrayList<>();

        // Sử dụng màng bọc để khởi tạo Adapter tránh lỗi lambda truy cập biến chưa xong
        final MemberSuggestionAdapter[] suggestionAdapterRef = {null};
        
        suggestionAdapterRef[0] = new MemberSuggestionAdapter(suggestionList, user -> {
            selectedMembers.add(user);
            addMemberChip(flexSelectedMembers, user, selectedMembers);
            etSearchMember.setText("");
            suggestionList.clear();
            if (suggestionAdapterRef[0] != null) {
                suggestionAdapterRef[0].notifyDataSetChanged();
            }
            rvSuggestions.setVisibility(View.GONE);
        });

        rvSuggestions.setLayoutManager(new LinearLayoutManager(context));
        rvSuggestions.setAdapter(suggestionAdapterRef[0]);

        // Lấy danh sách bạn bè
        ApiClient.getClient(context).create(ApiService.class).getFriendList()
                .enqueue(new Callback<FriendsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FriendsResponse> call, @NonNull Response<FriendsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            allFriends.clear();
                            allFriends.addAll(response.body().getFriends());
                        }
                    }
                    @Override 
                    public void onFailure(@NonNull Call<FriendsResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Lỗi lấy danh sách bạn bè: " + t.getMessage());
                    }
                });

        etSearchMember.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    suggestionList.clear();
                    if (suggestionAdapterRef[0] != null) suggestionAdapterRef[0].notifyDataSetChanged();
                    rvSuggestions.setVisibility(View.GONE);
                    return;
                }

                suggestionList.clear();
                for (User u : allFriends) {
                    String displayName = u.getDisplayName();
                    if (displayName != null && displayName.toLowerCase().contains(query)) {
                        boolean alreadySelected = false;
                        for (User sel : selectedMembers) {
                            if (sel.getId().equals(u.getId())) {
                                alreadySelected = true;
                                break;
                            }
                        }
                        if (!alreadySelected) {
                            suggestionList.add(u);
                        }
                    }
                }

                if (suggestionAdapterRef[0] != null) suggestionAdapterRef[0].notifyDataSetChanged();
                rvSuggestions.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnCreateGroup.setOnClickListener(v -> {
            String groupName = etGroupName.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedMembers.isEmpty()) {
                Toast.makeText(context, "Vui lòng chọn ít nhất 1 thành viên", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> memberIds = new ArrayList<>();
            for (User u : selectedMembers) {
                memberIds.add(u.getId());
            }

            Map<String, Object> body = new HashMap<>();
            body.put("type", "group");
            body.put("name", groupName);
            body.put("memberIds", memberIds);

            ApiClient.getClient(context).create(ApiService.class).createConversation(body)
                    .enqueue(new Callback<CreateConversationResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<CreateConversationResponse> call, @NonNull Response<CreateConversationResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                dialog.dismiss();
                                openChat(response.body().getConversation());
                            } else {
                                Toast.makeText(context, "Lỗi khi tạo nhóm", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<CreateConversationResponse> call, @NonNull Throwable t) {
                            Toast.makeText(context, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void addMemberChip(FlexboxLayout flexbox, User user, List<User> selectedMembers) {
        Context context = getContext();
        if (context == null) return;

        View chipView = LayoutInflater.from(context).inflate(R.layout.item_selected_member, flexbox, false);
        TextView tvName = chipView.findViewById(R.id.tvName);
        ImageView ivAvatar = chipView.findViewById(R.id.ivAvatar);
        TextView tvDefaultAvatar = chipView.findViewById(R.id.tvDefaultAvatar);
        ImageView btnRemove = chipView.findViewById(R.id.btnRemove);

        tvName.setText(user.getDisplayName());
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvDefaultAvatar.setVisibility(View.GONE);
            Glide.with(this).load(user.getAvatarUrl()).into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvDefaultAvatar.setVisibility(View.VISIBLE);
            String name = user.getDisplayName();
            String initial = (name != null && !name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            tvDefaultAvatar.setText(initial);
        }

        btnRemove.setOnClickListener(v -> {
            selectedMembers.remove(user);
            flexbox.removeView(chipView);
        });

        flexbox.addView(chipView);
    }

    private void showAddFriendDialog() {
        final Context context = getContext();
        if (context == null) return;

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_friend, null);
        final AlertDialog dialog = new AlertDialog.Builder(context).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final View layoutSearch = dialogView.findViewById(R.id.layoutSearch);
        final View layoutResult = dialogView.findViewById(R.id.layoutResult);
        final EditText etUsername = dialogView.findViewById(R.id.etUsername);
        final TextView tvError = dialogView.findViewById(R.id.tvError);
        final TextView tvFoundMessage = dialogView.findViewById(R.id.tvFoundMessage);
        final EditText etIntro = dialogView.findViewById(R.id.etIntro);

        final User[] foundUser = {null};

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (username.isEmpty()) return;

            tvError.setVisibility(View.GONE);
            ApiClient.getClient(context).create(ApiService.class).searchUser(username)
                    .enqueue(new Callback<SearchResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().getUser() != null) {
                                foundUser[0] = response.body().getUser();
                                tvFoundMessage.setText("Đã tìm thấy @" + foundUser[0].getUsername() + "!");
                                layoutSearch.setVisibility(View.GONE);
                                layoutResult.setVisibility(View.VISIBLE);
                            } else {
                                tvError.setText("Không tìm thấy @" + username);
                                tvError.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<SearchResponse> call, @NonNull Throwable t) {
                            tvError.setText("Lỗi kết nối máy chủ");
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
        });

        dialogView.findViewById(R.id.btnBack).setOnClickListener(v -> {
            layoutResult.setVisibility(View.GONE);
            layoutSearch.setVisibility(View.VISIBLE);
        });

        dialogView.findViewById(R.id.btnSendRequest).setOnClickListener(v -> {
            if (foundUser[0] == null) return;
            String message = etIntro.getText().toString().trim();
            if (message.isEmpty()) message = "Xin chào! - Có thể kết bạn được không?...";

            Map<String, String> data = new HashMap<>();
            data.put("to", foundUser[0].getId());
            data.put("message", message);

            ApiClient.getClient(context).create(ApiService.class).sendFriendRequest(data)
                    .enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(context, "Đã gửi lời mời kết bạn!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(context, "Không thể gửi yêu cầu", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Toast.makeText(context, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchConversations();
    }

    private void updateListWithNewConvo(Conversation updatedConvo) {
        Log.d(TAG, "Đang cập nhật hội thoại ID: " + updatedConvo.getId());
        Conversation oldConvo = null;
        int oldIndex = -1;
        List<Conversation> foundList = null;

        for (int i = 0; i < groupList.size(); i++) {
            if (groupList.get(i).getId().equals(updatedConvo.getId())) {
                oldConvo = groupList.get(i);
                oldIndex = i;
                foundList = groupList;
                break;
            }
        }

        if (oldConvo == null) {
            for (int i = 0; i < directList.size(); i++) {
                if (directList.get(i).getId().equals(updatedConvo.getId())) {
                    oldConvo = directList.get(i);
                    oldIndex = i;
                    foundList = directList;
                    break;
                }
            }
        }

        if (oldConvo != null) {
            oldConvo.setLastMessage(updatedConvo.getLastMessage());
            if (updatedConvo.getUnreadCounts() != null) {
                oldConvo.setUnreadCounts(updatedConvo.getUnreadCounts());
            }

            foundList.remove(oldIndex);
            foundList.add(0, oldConvo);

            if (foundList == groupList) {
                if (groupAdapter != null) groupAdapter.notifyDataSetChanged();
            } else {
                if (directAdapter != null) directAdapter.notifyDataSetChanged();
            }
        } else {
            fetchConversations();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SocketManager sm = SocketManager.getInstance(getContext());
        sm.removeOnlineUsersListener(onlineListener);
        sm.removeConvoListener(convoListener);
    }

    private void setupRecyclerViews() {
        Context context = getContext();
        if (context == null) return;

        rvGroupChats.setLayoutManager(new LinearLayoutManager(context));
        rvDirectChats.setLayoutManager(new LinearLayoutManager(context));
        
        groupAdapter = new ConversationAdapter(groupList, this::openChat);
        directAdapter = new ConversationAdapter(directList, this::openChat);
        
        rvGroupChats.setAdapter(groupAdapter);
        rvDirectChats.setAdapter(directAdapter);
    }

    private void fetchConversations() {
        Context context = getContext();
        if (context == null) return;

        ApiClient.getClient(context).create(ApiService.class).getConversations()
                .enqueue(new Callback<ConversationsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ConversationsResponse> call, @NonNull Response<ConversationsResponse> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null) {
                            groupList.clear(); 
                            directList.clear();
                            for (Conversation c : response.body().getConversations()) {
                                if ("group".equals(c.getType())) groupList.add(c);
                                else directList.add(c);
                            }
                            if (groupAdapter != null) groupAdapter.notifyDataSetChanged();
                            if (directAdapter != null) directAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override 
                    public void onFailure(@NonNull Call<ConversationsResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Lỗi fetch conversations: " + t.getMessage());
                    }
                });
    }

    private void openChat(Conversation convo) {
        Context context = getContext();
        if (context == null) return;

        Intent intent = new Intent(context, ChatActivity.class);
        intent.putExtra("CONVERSATION_ID", convo.getId());
        String name = ""; 
        String url = null;
        
        if ("group".equals(convo.getType())) {
            name = convo.getGroup() != null ? convo.getGroup().getName() : "Nhóm";
        } else {
            String myId = new TokenManager(context).getUserId();
            if (convo.getParticipants() != null) {
                for (Conversation.Participant p : convo.getParticipants()) {
                    if (!p.getId().equals(myId)) { 
                        name = p.getDisplayName(); 
                        url = p.getAvatarUrl(); 
                        break; 
                    }
                }
            }
        }
        intent.putExtra("CHAT_NAME", name);
        intent.putExtra("AVATAR_URL", url);
        startActivity(intent);
    }
}
