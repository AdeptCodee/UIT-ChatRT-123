package com.example.chatrt;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatrt.api.ApiClient;
import com.example.chatrt.api.ApiService;
import com.example.chatrt.api.TokenManager;
import com.example.chatrt.models.Reminder;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaceholderFragment extends Fragment {
    private RecyclerView rvReminders;
    private TextView tvEmpty;
    private ReminderAdapter adapter;
    private List<Reminder> reminderList = new ArrayList<>();
    private String myId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        rvReminders = view.findViewById(R.id.rvReminders);
        tvEmpty = view.findViewById(R.id.tvEmptyReminders);
        
        myId = new TokenManager(requireContext()).getUserId();
        
        setupRecyclerView();
        fetchReminders();
        
        return view;
    }

    private void setupRecyclerView() {
        adapter = new ReminderAdapter(reminderList, myId);
        rvReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvReminders.setAdapter(adapter);
    }

    private void fetchReminders() {
        ApiService api = ApiClient.getClient(requireContext()).create(ApiService.class);
        api.getMyReminders().enqueue(new Callback<List<Reminder>>() {
            @Override
            public void onResponse(Call<List<Reminder>> call, Response<List<Reminder>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reminderList.clear();
                    reminderList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    
                    tvEmpty.setVisibility(reminderList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<Reminder>> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Không thể tải danh sách hẹn", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchReminders();
    }
}
