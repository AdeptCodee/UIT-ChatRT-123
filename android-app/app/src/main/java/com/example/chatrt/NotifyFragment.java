package com.example.chatrt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatrt.api.*;
import com.example.chatrt.models.*;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotifyFragment extends Fragment {
    private RecyclerView rvRequests;
    private TextView tvEmptyState;
    private RequestAdapter adapter;
    private List<FriendRequest> receivedList = new ArrayList<>();
    private List<FriendRequest> sentList = new ArrayList<>();
    private boolean isReceivedTab = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notify, container, false);

        rvRequests = view.findViewById(R.id.rvRequests);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        setupTabs(view);
        fetchRequests();

        return view;
    }

    private void setupTabs(View v) {
        TextView btnReceived = v.findViewById(R.id.tabReceived);
        TextView btnSent = v.findViewById(R.id.tabSent);

        btnReceived.setOnClickListener(view -> {
            isReceivedTab = true;
            updateTabUI(btnReceived, btnSent);
            updateList();
        });

        btnSent.setOnClickListener(view -> {
            isReceivedTab = false;
            updateTabUI(btnSent, btnReceived);
            updateList();
        });
    }

    private void updateTabUI(TextView active, TextView inactive) {
        active.setBackgroundResource(R.drawable.status_bg);
        active.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        active.setTextColor(getResources().getColor(R.color.primary_purple));

        inactive.setBackground(null);
        inactive.setTextColor(0xFF6B7280);
    }

    private void fetchRequests() {
        ApiClient.getClient(getContext()).create(ApiService.class).getAllFriendRequests()
                .enqueue(new Callback<FriendRequestsResponse>() {
                    @Override
                    public void onResponse(Call<FriendRequestsResponse> call, Response<FriendRequestsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            receivedList.clear(); receivedList.addAll(response.body().getReceived());
                            sentList.clear(); sentList.addAll(response.body().getSent());
                            updateList();
                        }
                    }
                    @Override public void onFailure(Call<FriendRequestsResponse> call, Throwable t) {}
                });
    }

    private void updateList() {
        List<FriendRequest> currentList = isReceivedTab ? receivedList : sentList;
        
        if (currentList.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            if (isReceivedTab) {
                tvEmptyState.setText("Bạn chưa có lời mời kết bạn nào.");
            } else {
                tvEmptyState.setText("Bạn chưa gửi lời mời kết bạn nào.");
            }
        } else {
            rvRequests.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }

        adapter = new RequestAdapter(currentList, isReceivedTab, new RequestAdapter.OnRequestListener() {
            @Override public void onAccept(String id) { handleAction(id, true); }
            @Override public void onDecline(String id) { handleAction(id, false); }
        });
        rvRequests.setAdapter(adapter);
    }

    private void handleAction(String id, boolean accept) {
        ApiService api = ApiClient.getClient(getContext()).create(ApiService.class);
        Call<Void> call = accept ? api.acceptRequest(id) : api.declineRequest(id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) fetchRequests(); // Refresh danh sách
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }
}