package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;

public class SearchResponse {
    @SerializedName("user")
    private User user;

    public User getUser() { return user; }
}