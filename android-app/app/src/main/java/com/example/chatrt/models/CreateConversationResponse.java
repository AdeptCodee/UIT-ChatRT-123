package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;

public class CreateConversationResponse {
    @SerializedName("conversation")
    private Conversation conversation;
    public Conversation getConversation() { return conversation; }
}