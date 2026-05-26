package com.example.chatrt.models;
import com.google.gson.annotations.SerializedName;

public class SendMessageResponse {
    @SerializedName("message")
    private Message message;
    public Message getMessage() { return message; }
}