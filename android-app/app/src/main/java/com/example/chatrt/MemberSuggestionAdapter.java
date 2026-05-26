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

public class MemberSuggestionAdapter extends RecyclerView.Adapter<MemberSuggestionAdapter.ViewHolder> {
    private List<User> suggestions;
    private OnMemberClickListener listener;

    public interface OnMemberClickListener {
        void onMemberClick(User user);
    }

    public MemberSuggestionAdapter(List<User> suggestions, OnMemberClickListener listener) {
        this.suggestions = suggestions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = suggestions.get(position);
        holder.tvName.setText(user.getDisplayName());

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

        holder.itemView.setOnClickListener(v -> listener.onMemberClick(user));
    }

    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    @Override
    public int getItemCount() { return suggestions.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvDefaultAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvDefaultAvatar = itemView.findViewById(R.id.tvDefaultAvatar);
        }
    }
}
