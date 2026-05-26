package com.example.chatrt.models;

import com.google.gson.annotations.SerializedName;

/**
 * Class này mô phỏng lại model USERS.js từ Backend.
 * Nó dùng để chứa thông tin người dùng như tên, email, ảnh đại diện...
 */
public class User {

    // @SerializedName("_id") giúp Gson hiểu rằng key "_id" từ MongoDB
    // sẽ được gán vào biến "id" này trong Java.
    @SerializedName("_id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("bio")
    private String bio;

    @SerializedName("phone")
    private String phone;

    @SerializedName("createdAt")
    private String createdAt;

    // --- Thông tin ngân hàng (do backend trả về) ---
    @SerializedName("accountNo")
    private String accountNo;

    @SerializedName("accountName")
    private String accountName;

    @SerializedName("acqId")
    private String acqId;

    // --- Các hàm Getter và Setter ---
    // (Dùng để lấy dữ liệu ra hoặc gán dữ liệu vào các biến ở trên)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    // --- Getters/Setters for bank info ---
    public String getAccountNo() { return accountNo; }
    public void setAccountNo(String accountNo) { this.accountNo = accountNo; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAcqId() { return acqId; }
    public void setAcqId(String acqId) { this.acqId = acqId; }
}