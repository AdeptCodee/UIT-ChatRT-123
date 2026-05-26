package com.example.chatrt.models;

import com.google.gson.annotations.SerializedName;

/**
 * Class này mô phỏng lại Friend.js.
 * Dùng để quản lý mối quan hệ bạn bè giữa 2 người dùng.
 */
public class Friend {

    @SerializedName("_id")
    private String id;

    // ID của người bạn thứ nhất
    @SerializedName("userA")
    private String userA;

    // ID của người bạn thứ hai
    @SerializedName("userB")
    private String userB;

    @SerializedName("createdAt")
    private String createdAt;

    // --- Các hàm Getter và Setter ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserA() { return userA; }
    public void setUserA(String userA) { this.userA = userA; }

    public String getUserB() { return userB; }
    public void setUserB(String userB) { this.userB = userB; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}