package com.example.chatrt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatrt.models.FriendRequest;
import com.example.chatrt.models.User;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
    private List<FriendRequest> list;
    private boolean isReceivedTab;
    private OnRequestListener listener;

    public interface OnRequestListener {
        void onAccept(String id);
        void onDecline(String id);
    }

    public RequestAdapter(List<FriendRequest> list, boolean isReceivedTab, OnRequestListener listener) {
        this.list = list;
        this.isReceivedTab = isReceivedTab;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FriendRequest req = list.get(position);
        User user = isReceivedTab ? req.getFromUser() : req.getToUser();

        if (user != null) {
            holder.tvName.setText(user.getDisplayName());
            holder.tvUsername.setText("@" + user.getUsername());
            
            // Xử lý hiển thị Avatar hoặc Chữ cái mặc định
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                holder.ivAvatar.setVisibility(View.VISIBLE);
                holder.tvDefaultAvatar.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(user.getAvatarUrl())
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setVisibility(View.GONE);
                holder.tvDefaultAvatar.setVisibility(View.VISIBLE);
                holder.tvDefaultAvatar.setText(getFirstLetter(user.getDisplayName()));
            }
        }

        if (isReceivedTab) {
            holder.layoutActions.setVisibility(View.VISIBLE);
            holder.tvWaiting.setVisibility(View.GONE);
            holder.btnAccept.setOnClickListener(v -> listener.onAccept(req.getId()));
            holder.btnDecline.setOnClickListener(v -> listener.onDecline(req.getId()));
        } else {
            holder.layoutActions.setVisibility(View.GONE);
            holder.tvWaiting.setVisibility(View.VISIBLE);
        }
    }

    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvUsername, tvWaiting, tvDefaultAvatar;
        View layoutActions;
        Button btnAccept, btnDecline;

        ViewHolder(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivReqAvatar);
            tvDefaultAvatar = v.findViewById(R.id.tvDefaultReqAvatar);
            tvName = v.findViewById(R.id.tvReqName);
            tvUsername = v.findViewById(R.id.tvReqUsername);
            layoutActions = v.findViewById(R.id.layoutActions);
            tvWaiting = v.findViewById(R.id.tvWaiting);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnDecline = v.findViewById(R.id.btnDecline);
        }
    }
}