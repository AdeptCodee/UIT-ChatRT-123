package com.example.chatrt.models;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class FriendRequest {
    @SerializedName("_id")
    private String id;

    // Dùng JsonElement để linh hoạt vì đôi khi là String ID, đôi khi là Object User
    @SerializedName("from")
    private JsonElement fromRaw;

    @SerializedName("to")
    private JsonElement toRaw;

    @SerializedName("message")
    private String message;

    @SerializedName("createdAt")
    private String createdAt;

    public String getId() { return id; }

    // Lấy thông tin người liên quan (nếu mình nhận thì lấy 'from', mình gửi thì lấy 'to')
    public User getFromUser() { return convertToUser(fromRaw); }
    public User getToUser() { return convertToUser(toRaw); }

    private User convertToUser(JsonElement element) {
        if (element == null || !element.isJsonObject()) return null;
        return new com.google.gson.Gson().fromJson(element, User.class);
    }
}