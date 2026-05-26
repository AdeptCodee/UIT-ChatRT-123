package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FriendRequestsResponse {
    @SerializedName("sent")
    private List<FriendRequest> sent;

    @SerializedName("received")
    private List<FriendRequest> received;

    public List<FriendRequest> getSent() { return sent; }
    public List<FriendRequest> getReceived() { return received; }
}