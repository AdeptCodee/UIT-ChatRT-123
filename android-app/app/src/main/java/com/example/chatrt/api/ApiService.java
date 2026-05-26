package com.example.chatrt.api;

import com.example.chatrt.models.*;import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ==========================================
    // 1. AUTH SERVICE
    // ==========================================
    @POST("auth/signin")
    Call<AuthResponse> signIn(@Body Map<String, String> credentials);

    @POST("auth/signup")
    Call<AuthResponse> signUp(@Body Map<String, String> userData);

    @POST("auth/signout")
    Call<Void> signOut();

    @POST("auth/refresh")
    Call<AuthResponse> refreshToken();

    @GET("users/me")
    Call<SearchResponse> fetchMe();


    // ==========================================
    // 2. CHAT SERVICE
    // ==========================================
    @GET("conversations")
    Call<ConversationsResponse> getConversations();

    @GET("conversations/{id}/messages")
    Call<MessagesResponse> getMessages(
            @Path("id") String conversationId,
            @Query("limit") int limit,
            @Query("cursor") String cursor
    );

    @POST("conversations")
    Call<CreateConversationResponse> createConversation(@Body Map<String, Object> data);

    @PATCH("conversations/{id}/seen")
    Call<Void> markAsSeen(@Path("id") String conversationId);

    // Gửi tin nhắn cá nhân
    @Multipart
    @POST("messages/direct")
    Call<SendMessageResponse> sendDirectMessage(
            @Part("recipientId") RequestBody recipientId,
            @Part("content") RequestBody content,
            @Part("conversationId") RequestBody conversationId,
            @Part MultipartBody.Part image
    );

    // Gửi tin nhắn nhóm
    @Multipart
    @POST("messages/group")
    Call<SendMessageResponse> sendGroupMessage(
            @Part("conversationId") RequestBody conversationId,
            @Part("content") RequestBody content,
            @Part MultipartBody.Part image
    );


    // ==========================================
    // 3. FRIEND SERVICE
    // ==========================================
    @GET("friends")
    Call<FriendsResponse> getFriendList();

    @GET("users/search")
    Call<SearchResponse> searchUser(@Query("username") String username);

    @GET("friends/requests")
    Call<FriendRequestsResponse> getAllFriendRequests();

    @POST("friends/requests")
    Call<Void> sendFriendRequest(@Body Map<String, String> data);

    @POST("friends/requests/{id}/accept")
    Call<Void> acceptRequest(@Path("id") String requestId);

    @POST("friends/requests/{id}/decline")
    Call<Void> declineRequest(@Path("id") String requestId);


    // ==========================================
    // 4. USER SERVICE
    // ==========================================
    @Multipart
    @POST("users/uploadAvatar")
    Call<Map<String, String>> uploadAvatar(@Part MultipartBody.Part avatar);

    // ==========================================
    // 5. REMINDER SERVICE
    // ==========================================
    @POST("reminders")
    Call<Reminder> createReminder(@Body Map<String, Object> data);

    @GET("reminders")
    Call<List<Reminder>> getMyReminders();
    @PUT("payments/bank-setup")
    Call<com.google.gson.JsonObject> setupBank(@Body java.util.Map<String, String> body);

    @POST("payments/qr")
    Call<com.google.gson.JsonObject> generateQr(@Body java.util.Map<String, Object> body);
}
