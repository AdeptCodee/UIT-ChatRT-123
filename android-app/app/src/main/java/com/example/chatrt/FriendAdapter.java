package com.example.chatrt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatrt.models.User;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private List<User> friendList;
    private OnFriendClickListener listener;

    public interface OnFriendClickListener {
        void onFriendClick(User user);
    }

    public FriendAdapter(List<User> friendList, OnFriendClickListener listener) {
        this.friendList = friendList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friendList.get(position);
        holder.tvName.setText(user.getDisplayName());
        holder.tvUsername.setText("@" + user.getUsername());

        // FIX LỖI TÀNG HÌNH: Ẩn/Hiện đúng thành phần
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            holder.tvDefaultAvatar.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.edit_text_bg)
                    .into(holder.ivAvatar);
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvDefaultAvatar.setVisibility(View.VISIBLE);
            holder.tvDefaultAvatar.setText(getFirstLetter(user.getDisplayName()));
        }

        // Ẩn các phần không dùng ở màn hình này (Chấm online, Group)
        holder.viewStatusDot.setVisibility(View.GONE);
        holder.layoutGroup.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> listener.onFriendClick(user));
    }

    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    public int getItemCount() { return friendList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvUsername, tvDefaultAvatar;
        View viewStatusDot, layoutGroup;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivConvoAvatar);
            tvName = itemView.findViewById(R.id.tvConvoName);
            tvUsername = itemView.findViewById(R.id.tvLastMessage);
            tvDefaultAvatar = itemView.findViewById(R.id.tvDefaultAvatar);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            layoutGroup = itemView.findViewById(R.id.layoutGroupAvatar);
        }
    }
}