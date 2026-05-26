package com.example.chatrt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatrt.models.Reminder;
import com.example.chatrt.models.User;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {
    private List<Reminder> reminderList;
    private String myId;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ReminderAdapter(List<Reminder> reminderList, String myId) {
        this.reminderList = reminderList;
        this.myId = myId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reminder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);

        String creatorId = reminder.getCreatorId();
        User creator = reminder.getCreatorUser();
        User partner = reminder.getPartnerUser();

        User otherUser;
        String titlePrefix;
        String titleSuffix;

        if (creatorId != null && creatorId.equals(myId)) {
            // "Bạn đã nhắc nhở {username}"
            otherUser = partner;
            titlePrefix = "Bạn đã nhắc nhở ";
            titleSuffix = (otherUser != null && otherUser.getDisplayName() != null) ? otherUser.getDisplayName() : "người dùng";
        } else {
            // "{username} đã nhắc nhở bạn"
            otherUser = creator;
            String name = (otherUser != null && otherUser.getDisplayName() != null) ? otherUser.getDisplayName() : "Người dùng";
            titlePrefix = name + " ";
            titleSuffix = "đã nhắc nhở bạn";
        }

        holder.tvTitle.setText(titlePrefix + titleSuffix);
        holder.tvContent.setText(": " + reminder.getContent());
        holder.tvDate.setText("vào " + dateFormat.format(reminder.getDueDate()));

        // Xử lý Avatar của người đối diện
        if (otherUser != null) {
            String avatarUrl = otherUser.getAvatarUrl();
            String displayName = otherUser.getDisplayName();

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                holder.ivAvatar.setVisibility(View.VISIBLE);
                holder.tvDefaultAvatar.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.edit_text_bg)
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setVisibility(View.GONE);
                holder.tvDefaultAvatar.setVisibility(View.VISIBLE);
                String firstLetter = (displayName != null && !displayName.isEmpty()) 
                        ? displayName.substring(0, 1).toUpperCase() : "?";
                holder.tvDefaultAvatar.setText(firstLetter);
            }
        } else {
            holder.ivAvatar.setVisibility(View.GONE);
            holder.tvDefaultAvatar.setVisibility(View.VISIBLE);
            holder.tvDefaultAvatar.setText("?");
        }
    }

    @Override
    public int getItemCount() { return reminderList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvDate, tvDefaultAvatar;
        CircleImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvContent = itemView.findViewById(R.id.tvReminderContent);
            tvDate = itemView.findViewById(R.id.tvReminderDate);
            ivAvatar = itemView.findViewById(R.id.ivReminderAvatar);
            tvDefaultAvatar = itemView.findViewById(R.id.tvReminderDefaultAvatar);
        }
    }
}
