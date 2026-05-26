package com.example.chatrt;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatrt.api.*;
import com.example.chatrt.models.*;
import com.example.chatrt.utils.ReminderParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView rvMessages;
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private List<Conversation.Participant> currentParticipants = new ArrayList<>();

    private String conversationId, myId, chatName, avatarUrl, conversationType;
    private EditText etInput;
    private SocketManager socketManager;
    private Uri selectedImageUri = null;

    private SocketManager.OnOnlineUsersChangedListener onlineListener;
    private SocketManager.MessageListener messageListener;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Toast.makeText(this, "Đã chọn ảnh!", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Khôi phục lấy dữ liệu từ Intent
        conversationId = getIntent().getStringExtra("CONVERSATION_ID");
        chatName = getIntent().getStringExtra("CHAT_NAME");
        avatarUrl = getIntent().getStringExtra("AVATAR_URL");

        myId = new TokenManager(this).getUserId();
        socketManager = SocketManager.getInstance(this);

        initViews();
        setupRecyclerView();
        fetchConversationDetails();
        fetchMessages(null);
        setupSocket();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etInput = findViewById(R.id.etMessageInput);
        ImageView btnSend = findViewById(R.id.btnSendMessage);
        ImageView btnGallery = findViewById(R.id.btnSelectImage);
        TextView tvTitle = findViewById(R.id.tvChatTitle);

        tvTitle.setText(chatName);
        updateHeaderAvatar(chatName, avatarUrl);

        // KHÔI PHỤC NÚT BACK CHUẨN XỊN
        ImageView btnBack = findViewById(R.id.btnChatBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // Thoát màn hình chuẩn Android đời mới
                getOnBackPressedDispatcher().onBackPressed();
            });
        }

        // KẾT NỐI NÚT BẤM QR CODE
        ImageView btnQrCode = findViewById(R.id.btnQrCode);
        if (btnQrCode != null) {
            btnQrCode.setOnClickListener(v -> showQrDialog());
        }

        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void updateHeaderAvatar(String name, String url) {
        CircleImageView ivAvatar = findViewById(R.id.ivChatAvatar);
        TextView tvDefault = findViewById(R.id.tvChatDefaultAvatar);
        if (ivAvatar == null || tvDefault == null) return;

        if (url != null && !url.isEmpty()) {
            ivAvatar.setVisibility(View.VISIBLE);
            tvDefault.setVisibility(View.GONE);
            Glide.with(this).load(url).placeholder(R.drawable.edit_text_bg).into(ivAvatar);
        } else {
            ivAvatar.setVisibility(View.GONE);
            tvDefault.setVisibility(View.VISIBLE);
            tvDefault.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?");
        }
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messageList, myId, currentParticipants);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    private void fetchConversationDetails() {
        ApiService service = ApiClient.getClient(this).create(ApiService.class);
        service.getConversations().enqueue(new Callback<ConversationsResponse>() {
            @Override
            public void onResponse(Call<ConversationsResponse> call, Response<ConversationsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Conversation c : response.body().getConversations()) {
                        if (c.getId().equals(conversationId)) {
                            conversationType = c.getType();
                            currentParticipants.clear();
                            currentParticipants.addAll(c.getParticipants());

                            if ("direct".equals(conversationType)) {
                                for (Conversation.Participant p : currentParticipants) {
                                    if (!p.getId().equals(myId)) {
                                        updateHeaderAvatar(p.getDisplayName(), p.getAvatarUrl());
                                        ((TextView)findViewById(R.id.tvChatTitle)).setText(p.getDisplayName());
                                        break;
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                            updateHeaderStatus();
                            break;
                        }
                    }
                }
            }
            @Override public void onFailure(Call<ConversationsResponse> call, Throwable t) {}
        });
    }

    private void fetchMessages(String cursor) {
        ApiService service = ApiClient.getClient(this).create(ApiService.class);
        service.getMessages(conversationId, 20, cursor).enqueue(new Callback<MessagesResponse>() {
            @Override
            public void onResponse(Call<MessagesResponse> call, Response<MessagesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Message> newMessages = response.body().getMessages();
                    if (newMessages != null) {
                        messageList.addAll(0, newMessages);
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                }
            }
            @Override public void onFailure(Call<MessagesResponse> call, Throwable t) {}
        });
    }

    private void sendMessage() {
        String content = etInput.getText().toString().trim();
        if (content.isEmpty() && selectedImageUri == null) return;
        etInput.setText("");

        RequestBody rbConvoId = RequestBody.create(MediaType.parse("text/plain"), conversationId);
        RequestBody rbContent = RequestBody.create(MediaType.parse("text/plain"), content);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        String recipientId = "";
        for (Conversation.Participant p : currentParticipants) {
            if (!p.getId().equals(myId)) { recipientId = p.getId(); break; }
        }

        RequestBody rbRecipient = RequestBody.create(MediaType.parse("text/plain"), recipientId);
        api.sendDirectMessage(rbRecipient, rbContent, rbConvoId, null).enqueue(new Callback<SendMessageResponse>() {
            @Override
            public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addMessageToList(response.body().getMessage());
                }
            }
            @Override public void onFailure(Call<SendMessageResponse> call, Throwable t) {}
        });
    }

    private void setupSocket() {
        socketManager.connect();
        updateHeaderStatus();
        onlineListener = onlineIds -> runOnUiThread(this::updateHeaderStatus);
        messageListener = message -> runOnUiThread(() -> {
            if (message.getConversationId().equals(conversationId)) {
                addMessageToList(message);
                markConversationAsSeen();
            }
        });
        socketManager.addOnlineUsersListener(onlineListener);
        socketManager.addMessageListener(messageListener);
    }

    private void addMessageToList(Message message) {
        for (Message m : messageList) { if (m.getId() != null && m.getId().equals(message.getId())) return; }
        messageList.add(message);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
        handleReminderLogic(message);
    }

    private void handleReminderLogic(Message message) {
        if (!"direct".equals(conversationType) || message.getContent() == null) return;
        if (!message.getContent().trim().startsWith("/reminder")) return;

        ReminderParser.ReminderData data = ReminderParser.parse(message.getContent());
        if (data == null) return;

        String senderId = message.getSenderId();
        if (senderId == null) senderId = myId;

        String partnerId = null;
        for (Conversation.Participant p : currentParticipants) {
            if (!p.getId().equals(senderId)) { partnerId = p.getId(); break; }
        }

        if (partnerId != null) {
            Map<String, Object> body = new HashMap<>();
            body.put("conversationId", conversationId);
            body.put("messageId", message.getId());
            body.put("creatorId", senderId);
            body.put("partnerId", partnerId);
            body.put("content", data.content);
            body.put("dueDate", data.dueDate);

            ApiClient.getClient(this).create(ApiService.class).createReminder(body).enqueue(new Callback<Reminder>() {
                @Override public void onResponse(Call<Reminder> call, Response<Reminder> response) {
                    if (response.isSuccessful()) Toast.makeText(ChatActivity.this, "🚀 Đã tạo nhắc hẹn!", Toast.LENGTH_SHORT).show();
                }
                @Override public void onFailure(Call<Reminder> call, Throwable t) {}
            });
        }
    }

    private void updateHeaderStatus() {
        View dot = findViewById(R.id.viewStatusDot);
        TextView tvStatus = findViewById(R.id.tvOnlineStatus);
        if (dot == null || tvStatus == null || currentParticipants.isEmpty()) return;

        String otherId = null;
        for (Conversation.Participant p : currentParticipants) {
            if (!p.getId().equals(myId)) { otherId = p.getId(); break; }
        }

        if (otherId != null) {
            boolean isOnline = socketManager.isUserOnline(otherId);
            dot.setVisibility(View.VISIBLE);
            dot.setBackgroundTintList(ColorStateList.valueOf(isOnline ? 0xFF10B981 : 0xFF9CA3AF));
            tvStatus.setText(isOnline ? "Online" : "Offline");
            tvStatus.setTextColor(isOnline ? 0xFF10B981 : 0xFF9CA3AF);
        }
    }

    private void markConversationAsSeen() {
        if (conversationId == null) return;
        ApiClient.getClient(this).create(ApiService.class).markAsSeen(conversationId).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) {}
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        markConversationAsSeen();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketManager != null) {
            socketManager.removeOnlineUsersListener(onlineListener);
            socketManager.removeMessageListener(messageListener);
        }
    }

    private void showQrDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_qr_payment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View layoutCreateQr = dialog.findViewById(R.id.layoutCreateQr);
        View layoutSetupBank = dialog.findViewById(R.id.layoutSetupBank);
        android.widget.Button btnTabCreate = dialog.findViewById(R.id.btnTabCreate);
        android.widget.Button btnTabSetup = dialog.findViewById(R.id.btnTabSetup);

        // -- KHỞI TẠO DANH SÁCH DROPDOWN CHỌN NGÂN HÀNG --
        android.widget.Spinner spinnerBank = dialog.findViewById(R.id.spinnerBank);
        String[] bankNames = {"MBBank - Ngân hàng Quân Đội", "Vietcombank", "VietinBank", "BIDV", "TPBank"};
        String[] bankCodes = {"970422", "970436", "970415", "970418", "970423"};
        android.widget.ArrayAdapter<String> bankAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bankNames);
        if (spinnerBank != null) {
            spinnerBank.setAdapter(bankAdapter);
        }

        // Logic Chuyển Tab
        if (btnTabCreate != null) {
            btnTabCreate.setOnClickListener(v -> {
                if (layoutCreateQr != null) layoutCreateQr.setVisibility(View.VISIBLE);
                if (layoutSetupBank != null) layoutSetupBank.setVisibility(View.GONE);
                btnTabCreate.setBackgroundTintList(ColorStateList.valueOf(0xFF0D6EFD));
                btnTabCreate.setTextColor(0xFFFFFFFF);
                if (btnTabSetup != null) {
                    btnTabSetup.setBackgroundTintList(ColorStateList.valueOf(0xFFF3F4F6));
                    btnTabSetup.setTextColor(0xFF000000);
                }
            });
        }

        if (btnTabSetup != null) {
            btnTabSetup.setOnClickListener(v -> {
                if (layoutCreateQr != null) layoutCreateQr.setVisibility(View.GONE);
                if (layoutSetupBank != null) layoutSetupBank.setVisibility(View.VISIBLE);
                btnTabSetup.setBackgroundTintList(ColorStateList.valueOf(0xFF0D6EFD));
                btnTabSetup.setTextColor(0xFFFFFFFF);
                if (btnTabCreate != null) {
                    btnTabCreate.setBackgroundTintList(ColorStateList.valueOf(0xFFF3F4F6));
                    btnTabCreate.setTextColor(0xFF000000);
                }
            });
        }

        // Logic Xử lý Lưu Ngân Hàng
        android.widget.Button btnSaveBank = dialog.findViewById(R.id.btnSaveBank);
        if (btnSaveBank != null) {
            btnSaveBank.setOnClickListener(v -> {
                EditText etBankAccNo = dialog.findViewById(R.id.etBankAccNo);
                EditText etBankAccName = dialog.findViewById(R.id.etBankAccName);
                if (etBankAccNo == null || etBankAccName == null) return;

                String accNo = etBankAccNo.getText().toString().trim();
                String accName = etBankAccName.getText().toString().trim();

                int selectedPosition = 0;
                if (spinnerBank != null) {
                    selectedPosition = spinnerBank.getSelectedItemPosition();
                }
                String selectedBankCode = bankCodes[selectedPosition];

                if (accNo.isEmpty() || accName.isEmpty()) {
                    Toast.makeText(this, "Nhập đủ thông tin ngân hàng!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, String> body = new HashMap<>();
                body.put("accountNo", accNo);
                body.put("accountName", accName);
                body.put("acqId", selectedBankCode);

                ApiClient.getClient(this).create(ApiService.class).setupBank(body).enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, "Lưu tài khoản thành công!", Toast.LENGTH_SHORT).show();
                            if (btnTabCreate != null) btnTabCreate.performClick();
                        } else {
                            // MOI RUỘT GAN LỖI TỪ BACKEND RA ĐÂY NÀY
                            try {
                                String errorDetails = response.errorBody() != null ? response.errorBody().string() : "Không có chi tiết";
                                Toast.makeText(ChatActivity.this, "Lỗi " + response.code() + ": " + errorDetails, Toast.LENGTH_LONG).show();
                                Log.e("API_ERROR", "Lỗi backend: " + errorDetails);
                            } catch (Exception e) {
                                Toast.makeText(ChatActivity.this, "Lỗi HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    @Override public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Mất kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        // Logic Xử lý Tạo & Gửi Yêu Cầu QR
        android.widget.Button btnSendQr = dialog.findViewById(R.id.btnSendQr);
        if (btnSendQr != null) {
            btnSendQr.setOnClickListener(v -> {
                EditText etQrAmount = dialog.findViewById(R.id.etQrAmount);
                EditText etQrContent = dialog.findViewById(R.id.etQrContent);
                if (etQrAmount == null || etQrContent == null) return;

                String amount = etQrAmount.getText().toString().trim();
                String content = etQrContent.getText().toString().trim();
                if (amount.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số tiền!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // THÊM HIỆU ỨNG LOADING ĐỂ CHẶN BẤM NHIỀU LẦN
                btnSendQr.setText("Đang xử lý...");
                btnSendQr.setEnabled(false);

                Map<String, Object> body = new HashMap<>();
                body.put("amount", Integer.parseInt(amount));
                body.put("addInfo", content.isEmpty() ? "Chuyen tien" : content);

                ApiClient.getClient(this).create(ApiService.class).generateQr(body).enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String base64String = response.body().get("qrDataURL").getAsString();
                                // Xử lý cắt chuỗi an toàn hơn
                                if (base64String.contains(",")) {
                                    base64String = base64String.split(",")[1];
                                }
                                byte[] decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);

                                java.io.File file = new java.io.File(getCacheDir(), "qr_payment.png");
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                                fos.write(decodedBytes);
                                fos.flush(); fos.close();

                                RequestBody reqFile = RequestBody.create(MediaType.parse("image/png"), file);
                                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), reqFile);

                                RequestBody rbConvoId = RequestBody.create(MediaType.parse("text/plain"), conversationId);
                                String captionText = "💸 Gửi yêu cầu chuyển tiền: " + amount + " VNĐ\nNội dung: " + (content.isEmpty() ? "Chuyen tien" : content);
                                RequestBody rbContent = RequestBody.create(MediaType.parse("text/plain"), captionText);

                                String recipientId = "";
                                for (Conversation.Participant p : currentParticipants) {
                                    if (!p.getId().equals(myId)) { recipientId = p.getId(); break; }
                                }

                                if (recipientId.isEmpty()) {
                                    btnSendQr.setText("Gửi mã QR");
                                    btnSendQr.setEnabled(true);
                                    Toast.makeText(ChatActivity.this, "Lỗi: Không tìm thấy người nhận!", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                RequestBody rbRecipient = RequestBody.create(MediaType.parse("text/plain"), recipientId);

                                ApiClient.getClient(ChatActivity.this).create(ApiService.class).sendDirectMessage(rbRecipient, rbContent, rbConvoId, imagePart).enqueue(new Callback<SendMessageResponse>() {
                                    @Override
                                    public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
                                        btnSendQr.setText("Gửi mã QR");
                                        btnSendQr.setEnabled(true);

                                        if (response.isSuccessful() && response.body() != null) {
                                            addMessageToList(response.body().getMessage());
                                            dialog.dismiss(); // Tắt popup khi thành công
                                        } else {
                                            try {
                                                String err = response.errorBody() != null ? response.errorBody().string() : "";
                                                Toast.makeText(ChatActivity.this, "Lỗi gửi ảnh: " + response.code() + " " + err, Toast.LENGTH_LONG).show();
                                            } catch (Exception e) {
                                                Toast.makeText(ChatActivity.this, "Lỗi gửi ảnh vào chat!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                    @Override public void onFailure(Call<SendMessageResponse> call, Throwable t) {
                                        btnSendQr.setText("Gửi mã QR");
                                        btnSendQr.setEnabled(true);
                                        Toast.makeText(ChatActivity.this, "Mất kết nối mạng khi gửi!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                btnSendQr.setText("Gửi mã QR");
                                btnSendQr.setEnabled(true);
                                Toast.makeText(ChatActivity.this, "Lỗi giải mã ảnh QR!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            btnSendQr.setText("Gửi mã QR");
                            btnSendQr.setEnabled(true);
                            if (response.code() == 400) {
                                Toast.makeText(ChatActivity.this, "Chưa cài ngân hàng!", Toast.LENGTH_SHORT).show();
                                if (btnTabSetup != null) btnTabSetup.performClick();
                            } else {
                                try {
                                    String err = response.errorBody() != null ? response.errorBody().string() : "";
                                    Toast.makeText(ChatActivity.this, "Lỗi API Backend: " + response.code() + " " + err, Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    Toast.makeText(ChatActivity.this, "Lỗi API Backend sinh QR!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                    @Override public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        btnSendQr.setText("Gửi mã QR");
                        btnSendQr.setEnabled(true);
                        Toast.makeText(ChatActivity.this, "Không thể kết nối đến Backend: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }
}