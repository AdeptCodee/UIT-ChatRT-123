package com.example.chatrt.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FriendsResponse {
    @SerializedName("friends")
    private List<User> friends;

    public List<User> getFriends() { return friends; }
}