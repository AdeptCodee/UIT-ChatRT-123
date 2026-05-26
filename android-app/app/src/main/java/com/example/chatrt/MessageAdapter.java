package com.example.chatrt;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chatrt.models.Conversation;
import com.example.chatrt.models.Message;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private List<Message> messages;
    private List<Conversation.Participant> participants;
    private String myId;

    public MessageAdapter(List<Message> messages, String myId, List<Conversation.Participant> participants) {
        this.messages = messages;
        this.myId = myId;
        this.participants = participants;
    }

    @Override
    public int getItemViewType(int position) {
        String msgSenderId = messages.get(position).getSenderId();
        if (msgSenderId != null && msgSenderId.equals(myId)) return TYPE_SENT;
        else return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);
        Message prevMessage = position > 0 ? messages.get(position - 1) : null;

        // 1. Logic Header thời gian (1 phút)
        long currentTs = parseIsoDate(message.getCreatedAt());
        long prevTs = prevMessage != null ? parseIsoDate(prevMessage.getCreatedAt()) : 0;
        boolean isShowTime = (position == 0) || (currentTs - prevTs > 60 * 1000);

        // 2. Logic Hiển thị Avatar (Group Break)
        boolean isGroupBreak = isShowTime || (prevMessage == null) || !message.getSenderId().equals(prevMessage.getSenderId());

        if (holder instanceof SentViewHolder) {
            SentViewHolder h = (SentViewHolder) holder;
            setupCommonViews(h.tvContent, h.ivImage, h.tvDateHeader, message, isShowTime);
        } else {
            ReceivedViewHolder h = (ReceivedViewHolder) holder;
            setupCommonViews(h.tvContent, h.ivImage, h.tvDateHeader, message, isShowTime);

            // --- FIX LỖI LOGCAT: KIỂM TRA NULL AN TOÀN ---
            if (h.layoutAvatar != null) {
                if (isGroupBreak) {
                    h.layoutAvatar.setVisibility(View.VISIBLE);

                    // Tìm participant để lấy tên và ảnh
                    Conversation.Participant sender = findParticipantById(message.getSenderId());

                    if (sender != null && sender.getAvatarUrl() != null && !sender.getAvatarUrl().isEmpty()) {
                        // CÓ ẢNH THẬT
                        if (h.ivAvatar != null) h.ivAvatar.setVisibility(View.VISIBLE);
                        if (h.tvDefaultAvatar != null) h.tvDefaultAvatar.setVisibility(View.GONE);
                        Glide.with(h.itemView.getContext())
                                .load(sender.getAvatarUrl())
                                .placeholder(R.drawable.edit_text_bg)
                                .into(h.ivAvatar);
                    } else {
                        // KHÔNG CÓ ẢNH -> HIỆN CHỮ CÁI ĐẦU
                        if (h.ivAvatar != null) h.ivAvatar.setVisibility(View.GONE);
                        if (h.tvDefaultAvatar != null) {
                            h.tvDefaultAvatar.setVisibility(View.VISIBLE);
                            String name = (sender != null) ? sender.getDisplayName() : "Unknown";
                            h.tvDefaultAvatar.setText(getFirstLetter(name));
                        }
                    }
                } else {
                    h.layoutAvatar.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void setupCommonViews(TextView tvContent, ImageView ivImage, TextView tvDateHeader, Message message, boolean isShowTime) {
        if (isShowTime && tvDateHeader != null) {
            tvDateHeader.setVisibility(View.VISIBLE);
            tvDateHeader.setText(formatDateHeader(message.getCreatedAt()));
        } else if (tvDateHeader != null) {
            tvDateHeader.setVisibility(View.GONE);
        }

        if (tvContent != null) {
            if (message.getContent() != null && !message.getContent().isEmpty()) {
                tvContent.setVisibility(View.VISIBLE);
                tvContent.setText(message.getContent());
            } else {
                tvContent.setVisibility(View.GONE);
            }
        }

        if (ivImage != null) {
            if (message.getImgUrl() != null && !message.getImgUrl().isEmpty()) {
                ivImage.setVisibility(View.VISIBLE);
                Glide.with(ivImage.getContext()).load(message.getImgUrl()).into(ivImage);
                if (tvContent != null && (message.getContent() == null || message.getContent().isEmpty())) {
                    tvContent.setPadding(0, 0, 0, 0);
                }
            } else {
                ivImage.setVisibility(View.GONE);
                if (tvContent != null) tvContent.setPadding(30, 25, 30, 25);
            }
        }
    }

    private Conversation.Participant findParticipantById(String userId) {
        if (participants == null || userId == null) return null;
        for (Conversation.Participant p : participants) {
            if (userId.equals(p.getId())) return p;
        }
        return null;
    }

    private String getFirstLetter(String name) {
        if (name == null || name.isEmpty()) return "?";
        return name.substring(0, 1).toUpperCase();
    }

    private long parseIsoDate(String isoDate) {
        if (isoDate == null) return 0;
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            return parser.parse(isoDate).getTime();
        } catch (Exception e) { return 0; }
    }

    private String formatDateHeader(String isoDate) {
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(isoDate);
            Calendar now = Calendar.getInstance();
            Calendar msgDate = Calendar.getInstance();
            msgDate.setTime(date);
            String timePart = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
            if (now.get(Calendar.DATE) == msgDate.get(Calendar.DATE)) return "Hôm nay " + timePart;
            return new SimpleDateFormat("dd/MM ", Locale.getDefault()).format(date) + timePart;
        } catch (Exception e) { return ""; }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDateHeader, tvTime; ImageView ivImage;
        SentViewHolder(View v) { super(v);
            tvContent = v.findViewById(R.id.tvMessageContent);
            ivImage = v.findViewById(R.id.ivMessageImage);
            tvDateHeader = v.findViewById(R.id.tvDateHeader);
            tvTime = v.findViewById(R.id.tvMessageTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent, tvDateHeader, tvTime, tvDefaultAvatar;
        ImageView ivImage;
        CircleImageView ivAvatar;
        View layoutAvatar;

        ReceivedViewHolder(View v) { super(v);
            tvContent = v.findViewById(R.id.tvMessageContent);
            ivImage = v.findViewById(R.id.ivMessageImage);
            tvDateHeader = v.findViewById(R.id.tvDateHeader);
            tvTime = v.findViewById(R.id.tvMessageTime);
            ivAvatar = v.findViewById(R.id.ivSenderAvatar);
            tvDefaultAvatar = v.findViewById(R.id.tvDefaultAvatar);
            layoutAvatar = v.findViewById(R.id.layoutAvatarContainer);
        }
    }
}