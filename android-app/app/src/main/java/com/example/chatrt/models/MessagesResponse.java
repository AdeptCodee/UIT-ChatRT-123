package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessagesResponse {
    @SerializedName("messages")
    private List<Message> messages;

    @SerializedName("nextCursor")
    private String nextCursor;

    public List<Message> getMessages() { return messages; }
    public String getNextCursor() { return nextCursor; }
}