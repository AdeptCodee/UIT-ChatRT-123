package com.example.chatrt.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Class này mô phỏng lại Conversation.js.
 * Quản lý thông tin cuộc trò chuyện (Chat đơn hoặc Chat nhóm).
 */
public class Conversation {

    @SerializedName("_id")
    private String id;

    @SerializedName("type")
    private String type; // "direct" (cá nhân) hoặc "group" (nhóm)

    @SerializedName("participants")
    private List<Participant> participants;

    @SerializedName("group")
    private GroupInfo group;

    @SerializedName("lastMessage")
    private LastMessage lastMessage;

    @SerializedName("unreadCounts")
    private Map<String, Integer> unreadCounts; // ID người dùng -> Số tin chưa đọc

    @SerializedName("updatedAt")
    private String updatedAt;

    // --- Các Class phụ bên trong (Nested Classes) ---

    public static class Participant {
        // Cập nhật: Server có thể trả về _id hoặc userId tùy trường hợp (Socket vs API)
        @SerializedName(value = "_id", alternate = {"userId"})
        private String id;

        @SerializedName("displayName")
        private String displayName;

        @SerializedName("avatarUrl")
        private String avatarUrl;

        @SerializedName("joinedAt")
        private String joinedAt;

        // Getters
        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
    }

    public static class GroupInfo {
        @SerializedName("name")
        private String name;

        @SerializedName("createdBy")
        private String createdBy;

        public String getName() { return name; }
    }

    public static class LastMessage {
        @SerializedName("_id")
        private String id;

        @SerializedName("content")
        private String content;

        /**
         * FIX TRIỆT ĐỂ: Dùng JsonElement để GSON không bị crash khi gặp String thay vì Object.
         * Lỗi "Expected BEGIN_OBJECT but was STRING" xảy ra khi field này được định nghĩa là 1 Class.
         */
        @SerializedName("senderId")
        private JsonElement senderId;

        @SerializedName("createdAt")
        private String createdAt;

        public String getContent() { return content; }

        public String getSenderId() {
            if (senderId == null || senderId.isJsonNull()) return null;
            if (senderId.isJsonPrimitive()) return senderId.getAsString();
            if (senderId.isJsonObject()) {
                JsonElement idElem = senderId.getAsJsonObject().get("_id");
                return idElem != null ? idElem.getAsString() : null;
            }
            return null;
        }
    }

    // --- Các hàm Getter và Setter chính ---

    public String getId() { return id; }
    public String getType() { return type; }
    public List<Participant> getParticipants() { return participants; }
    public GroupInfo getGroup() { return group; }
    public LastMessage getLastMessage() { return lastMessage; }
    public Map<String, Integer> getUnreadCounts() { return unreadCounts; }
    public String getUpdatedAt() { return updatedAt; }

    // --- ĐÃ BỔ SUNG: Các hàm Setter để cập nhật Real-time ---

    public void setLastMessage(LastMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setUnreadCounts(Map<String, Integer> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }

    public void setParticipants(List<Participant> participants) {
        this.participants = participants;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setGroup(GroupInfo group) {
        this.group = group;
    }
}