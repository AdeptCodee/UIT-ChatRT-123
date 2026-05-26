package com.example.chatrt.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ConversationsResponse {
    @SerializedName("conversations")
    private List<Conversation> conversations;

    public List<Conversation> getConversations() {
        return conversations;
    }
}