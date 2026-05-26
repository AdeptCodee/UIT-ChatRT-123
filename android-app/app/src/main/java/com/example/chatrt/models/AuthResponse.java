package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("user")
    private User user;

    public String getAccessToken() { return accessToken; }
    public User getUser() { return user; }
}