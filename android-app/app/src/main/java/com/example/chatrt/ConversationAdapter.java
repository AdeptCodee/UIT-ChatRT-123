package com.example.chatrt;

import android.content.res.ColorStateList;import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatrt.api.SocketManager;
import com.example.chatrt.models.Conversation;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;
import java.util.Map;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private List<Conversation> conversations;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversations, OnItemClickListener listener) {
        this.conversations = conversations;
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
        Conversation convo = conversations.get(position);
        com.example.chatrt.api.TokenManager tokenManager = new com.example.chatrt.api.TokenManager(holder.itemView.getContext());
        String myId = tokenManager.getUserId();
        SocketManager socketManager = SocketManager.getInstance(holder.itemView.getContext());

        // Reset hiển thị cơ bản
        holder.ivAvatar.setVisibility(View.GONE);
        holder.tvDefaultAvatar.setVisibility(View.GONE);
        holder.layoutGroup.setVisibility(View.GONE);
        holder.viewStatusDot.setVisibility(View.GONE);

        // --- 1. XỬ LÝ UNREAD COUNT (Logic từ web) ---
        int unreadCount = 0;
        if (convo.getUnreadCounts() != null && myId != null) {
            Integer count = convo.getUnreadCounts().get(myId);
            if (count != null) unreadCount = count;
        }

        if (unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(unreadCount));
            // Làm đậm tin nhắn cuối nếu chưa đọc
            holder.tvLastMsg.setTypeface(null, Typeface.BOLD);
            holder.tvLastMsg.setTextColor(0xFF111827); // Màu đen đậm
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
            holder.tvLastMsg.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMsg.setTextColor(0xFF6B7280); // Màu xám nhạt
        }

        // --- 2. XỬ LÝ AVATAR & TÊN ---
        if (convo.getType().equals("group")) {
            holder.tvName.setText(convo.getGroup() != null ? convo.getGroup().getName() : "Nhóm");
            holder.layoutGroup.setVisibility(View.VISIBLE);
            setupGroupAvatar(holder.layoutGroup, convo.getParticipants());
        } else {
            if (convo.getParticipants() != null) {
                for (Conversation.Participant p : convo.getParticipants()) {
                    if (!p.getId().equals(myId)) {
                        holder.tvName.setText(p.getDisplayName());
                        displaySingleAvatar(holder, p.getDisplayName(), p.getAvatarUrl());

                        // Dấu chấm online real-time
                        holder.viewStatusDot.setVisibility(View.VISIBLE);
                        boolean isOnline = socketManager.isUserOnline(p.getId());
                        holder.viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(isOnline ? 0xFF10B981 : 0xFFD1D5DB));
                        break;
                    }
                }
            }
        }

        // Tin nhắn cuối
        if (convo.getLastMessage() != null) {
            holder.tvLastMsg.setText(convo.getLastMessage().getContent());
        } else {
            holder.tvLastMsg.setText("Chưa có tin nhắn");
        }

        holder.itemView.setOnClickListener(v -> listener.onItemClick(convo));
    }

    private void displaySingleAvatar(ViewHolder holder, String name, String url) {
        if (url != null && !url.isEmpty()) {
            holder.ivAvatar.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext()).load(url).into(holder.ivAvatar);
        } else {
            holder.tvDefaultAvatar.setVisibility(View.VISIBLE);
            holder.tvDefaultAvatar.setText(getFirstLetter(name));
        }
    }

    private void setupGroupAvatar(LinearLayout layout, List<Conversation.Participant> list) {
        layout.removeAllViews();
        if (list == null) return;
        int count = Math.min(list.size(), 3);
        float density = layout.getContext().getResources().getDisplayMetrics().density;
        int size = (int) (24 * density);
        int overlap = (int) (-8 * density);

        for (int i = 0; i < count; i++) {
            Conversation.Participant p = list.get(i);
            View avatarView;
            if (p.getAvatarUrl() != null && !p.getAvatarUrl().isEmpty()) {
                CircleImageView iv = new CircleImageView(layout.getContext());
                iv.setBorderWidth((int)(1.5 * density));
                iv.setBorderColor(0xFFFFFFFF);
                Glide.with(layout.getContext()).load(p.getAvatarUrl()).into(iv);
                avatarView = iv;
            } else {
                TextView tv = new TextView(layout.getContext());
                tv.setBackgroundResource(R.drawable.status_bg);
                tv.setBackgroundTintList(ColorStateList.valueOf(0xFF7C3AED));
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setTextColor(0xFFFFFFFF);
                tv.setTextSize(10);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                tv.setText(getFirstLetter(p.getDisplayName()));
                tv.setPadding(2, 2, 2, 2);
                avatarView = tv;
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) params.setMargins(overlap, 0, 0, 0);
            avatarView.setLayoutParams(params);
            layout.addView(avatarView);
        }
        if (list.size() > 3) {
            TextView tvMore = new TextView(layout.getContext());
            tvMore.setText("...");
            tvMore.setTextColor(0xFF6B7280);
            tvMore.setPadding((int)(4 * density), 0, 0, 0);
            layout.addView(tvMore);
        }
    }

    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    public int getItemCount() { return conversations.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvLastMsg, tvDefaultAvatar, tvUnreadCount;
        LinearLayout layoutGroup;
        View viewStatusDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivConvoAvatar);
            tvName = itemView.findViewById(R.id.tvConvoName);
            tvLastMsg = itemView.findViewById(R.id.tvLastMessage);
            tvDefaultAvatar = itemView.findViewById(R.id.tvDefaultAvatar);
            layoutGroup = itemView.findViewById(R.id.layoutGroupAvatar);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
        }
    }
}