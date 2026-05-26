package com.example.chatrt.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("_id")
    private String id;

    @SerializedName("conversationId")
    private String conversationId;

    @SerializedName("senderId")
    private JsonElement senderId; // Dùng JsonElement để cân cả String ID và Object

    @SerializedName("content")
    private String content;

    @SerializedName("imgUrl")
    private String imgUrl;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }

    public String getConversationId() { return conversationId; }

    // Hàm helper lấy String ID an toàn
    public String getSenderId() {
        if (senderId == null || senderId.isJsonNull()) return null;
        if (senderId.isJsonPrimitive()) return senderId.getAsString();
        if (senderId.isJsonObject()) {
            JsonElement idElem = senderId.getAsJsonObject().get("_id");
            if (idElem == null) idElem = senderId.getAsJsonObject().get("userId");
            return idElem != null ? idElem.getAsString() : null;
        }
        return null;
    }

    public String getContent() { return content; }
    public String getImgUrl() { return imgUrl; }
    public String getCreatedAt() { return createdAt; }
}
