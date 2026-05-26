package com.example.chatrt.models;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;

public class Reminder implements Serializable {
    @SerializedName("_id")
    private String id;
    private String conversationId;
    @SerializedName("creatorId")
    private JsonElement creatorId; 
    @SerializedName("partnerId")
    private JsonElement partnerId;
    private String content;
    private Date dueDate;

    public String getId() { return id; }
    public String getContent() { return content; }
    public Date getDueDate() { return dueDate; }

    public String getCreatorId() { return getIdFromElem(creatorId); }
    public String getPartnerId() { return getIdFromElem(partnerId); }

    public User getCreatorUser() { return getUserFromElem(creatorId); }
    public User getPartnerUser() { return getUserFromElem(partnerId); }

    private String getIdFromElem(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsString();
        if (e.isJsonObject()) return e.getAsJsonObject().get("_id").getAsString();
        return null;
    }

    private User getUserFromElem(JsonElement element) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) return null;
        return new com.google.gson.Gson().fromJson(element, User.class);
    }
}
