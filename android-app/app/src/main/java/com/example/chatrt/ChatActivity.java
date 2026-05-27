package com.example.chatrt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatrt.api.*;
import com.example.chatrt.models.*;
import com.example.chatrt.utils.ReminderParser;

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
    private LinearLayout expansionPanel;
    private ImageButton btnToggleFeatures;
    private View btnFeatureFund;
    private View btnFeatureQr;
    private View btnFeatureImage;

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
        TextView tvTitle = findViewById(R.id.tvChatTitle);
        expansionPanel = findViewById(R.id.expansion_panel);
        btnToggleFeatures = findViewById(R.id.btn_toggle_features);
        btnFeatureFund = findViewById(R.id.btn_feature_fund);
        btnFeatureQr = findViewById(R.id.btn_feature_qr);
        btnFeatureImage = findViewById(R.id.btn_feature_image);

        tvTitle.setText(chatName);
        updateHeaderAvatar(chatName, avatarUrl);

        ImageView btnBack = findViewById(R.id.btnChatBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnToggleFeatures.setOnClickListener(v -> {
            if (expansionPanel.getVisibility() == View.GONE) {
                expansionPanel.setVisibility(View.VISIBLE);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            } else {
                expansionPanel.setVisibility(View.GONE);
            }
        });

        etInput.setOnClickListener(v -> {
            if (expansionPanel.getVisibility() == View.VISIBLE) {
                expansionPanel.setVisibility(View.GONE);
            }
        });

        if (btnFeatureImage != null) {
            btnFeatureImage.setOnClickListener(v -> {
                expansionPanel.setVisibility(View.GONE);
                selectImage();
            });
        }

        if (btnFeatureQr != null) {
            btnFeatureQr.setOnClickListener(v -> {
                expansionPanel.setVisibility(View.GONE);
                showQrDialog();
            });
        }

        if (btnFeatureFund != null) {
            btnFeatureFund.setOnClickListener(v -> {
                expansionPanel.setVisibility(View.GONE);
                showCreateFundDialog();
            });
        }

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

        if ("/skipday".equals(content.trim())) {
            etInput.setText("");
            callSkipDayApi();
            return;
        }

        etInput.setText("");

        MultipartBody.Part imagePart = null;
        if (selectedImageUri != null) {
            imagePart = createImagePart(selectedImageUri);
            selectedImageUri = null;
        }

        RequestBody rbConvoId = RequestBody.create(MediaType.parse("text/plain"), conversationId);
        RequestBody rbContent = RequestBody.create(MediaType.parse("text/plain"), content);

        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        if ("direct".equals(conversationType)) {
            String recipientId = "";
            for (Conversation.Participant p : currentParticipants) {
                if (!p.getId().equals(myId)) {
                    recipientId = p.getId();
                    break;
                }
            }
            RequestBody rbRecipient = RequestBody.create(MediaType.parse("text/plain"), recipientId);
            api.sendDirectMessage(rbRecipient, rbContent, rbConvoId, imagePart).enqueue(messageCallback);
        } else {
            api.sendGroupMessage(rbConvoId, rbContent, imagePart).enqueue(messageCallback);
        }
    }

    private final Callback<SendMessageResponse> messageCallback = new Callback<SendMessageResponse>() {
        @Override
        public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
            if (response.isSuccessful() && response.body() != null) {
                addMessageToList(response.body().getMessage());
            }
        }
        @Override public void onFailure(Call<SendMessageResponse> call, Throwable t) {}
    };

    private void selectImage() {
        pickImageLauncher.launch("image/*");
    }

    private MultipartBody.Part createImagePart(Uri uri) {
        try (java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
             java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
            if (inputStream == null) return null;
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            String mimeType = getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = "image/*";
            }
            RequestBody requestFile = RequestBody.create(outputStream.toByteArray(), MediaType.parse(mimeType));
            return MultipartBody.Part.createFormData("image", "upload_image.png", requestFile);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showCreateFundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_fund, null);
        builder.setView(dialogView);
        builder.setTitle("Tạo quỹ mới");

        EditText etFundTitle = dialogView.findViewById(R.id.etFundTitle);
        EditText etFundAmount = dialogView.findViewById(R.id.etFundAmount);
        EditText etFundDays = dialogView.findViewById(R.id.etFundDays);

        builder.setPositiveButton("Tạo quỹ", null);
        builder.setNegativeButton("Hủy", (dialogInterface, which) -> dialogInterface.dismiss());

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dlg -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etFundTitle.getText().toString().trim();
                String amountText = etFundAmount.getText().toString().trim();
                String daysText = etFundDays.getText().toString().trim();

                if (title.isEmpty() || amountText.isEmpty() || daysText.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "Vui lòng nhập đầy đủ thông tin quỹ!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // KHAI BÁO VÀ PARSE DỮ LIỆU ĐỂ TRÁNH LỖI BÔI ĐỎ
                long totalAmount;
                int totalDays;
                try {
                    totalAmount = Long.parseLong(amountText);
                    totalDays = Integer.parseInt(daysText);
                } catch (NumberFormatException e) {
                    Toast.makeText(ChatActivity.this, "Số tiền và số ngày phải là số hợp lệ!", Toast.LENGTH_SHORT).show();
                    return;
                }

                callCreateFundApi(title, totalAmount, totalDays);
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void callCreateFundApi(String title, long totalAmount, int totalDays) {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId);
        body.put("title", title);
        body.put("totalAmount", totalAmount);
        body.put("totalDays", totalDays);

        String accessToken = new TokenManager(this).getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = "Bearer " + accessToken;

        api.createFund(token, body).enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "🎉 Tạo quỹ thành công!", Toast.LENGTH_LONG).show();
                    String fundMessage = "🪙 Quỹ mới được tạo: " + title + " - Tổng " + totalAmount + " VNĐ trong " + totalDays + " ngày.";
                    sendSystemMessage(fundMessage);
                } else {
                    Toast.makeText(ChatActivity.this, "Lỗi tạo quỹ: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi kết nối khi tạo quỹ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendSystemMessage(String content) {
        RequestBody rbConvoId = RequestBody.create(MediaType.parse("text/plain"), conversationId);
        RequestBody rbContent = RequestBody.create(MediaType.parse("text/plain"), content);
        ApiService api = ApiClient.getClient(this).create(ApiService.class);

        if ("direct".equals(conversationType)) {
            String recipientId = "";
            for (Conversation.Participant p : currentParticipants) {
                if (!p.getId().equals(myId)) {
                    recipientId = p.getId();
                    break;
                }
            }
            RequestBody rbRecipient = RequestBody.create(MediaType.parse("text/plain"), recipientId);
            api.sendDirectMessage(rbRecipient, rbContent, rbConvoId, null).enqueue(messageCallback);
        } else {
            api.sendGroupMessage(rbConvoId, rbContent, null).enqueue(messageCallback);
        }
    }

    private void callSkipDayApi() {
        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        Map<String, Object> body = new HashMap<>();
        body.put("conversationId", conversationId);

        String accessToken = new TokenManager(this).getAccessToken();
        if (accessToken == null) {
            Toast.makeText(this, "Phiên đăng nhập hết hạn!", Toast.LENGTH_SHORT).show();
            return;
        }
        String token = "Bearer " + accessToken;

        api.skipDay(token, body).enqueue(new Callback<com.google.gson.JsonObject>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String message = response.body().has("message") ? response.body().get("message").getAsString() : "Đã chuyển ngày thành công.";
                    Toast.makeText(ChatActivity.this, message, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ChatActivity.this, "Lỗi khi chạy /skipday: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi kết nối khi chạy /skipday", Toast.LENGTH_SHORT).show();
            }
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

        Spinner spinnerBank = dialog.findViewById(R.id.spinnerBank);
        String[] bankNames = {"MBBank - Ngân hàng Quân Đội", "Vietcombank", "VietinBank", "BIDV", "TPBank"};
        String[] bankCodes = {"970422", "970436", "970415", "970418", "970423"};
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, bankNames);
        if (spinnerBank != null) spinnerBank.setAdapter(bankAdapter);

        EditText etBankAccNo = dialog.findViewById(R.id.etBankAccNo);
        EditText etBankAccName = dialog.findViewById(R.id.etBankAccName);

        // --- FETCH DỮ LIỆU CŨ ĐỂ HIỂN THỊ ---
        ApiService apiService = ApiClient.getClient(this).create(ApiService.class);
        apiService.fetchMe().enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getUser();
                    if (user != null) {
                        if (etBankAccNo != null) etBankAccNo.setText(user.getAccountNo());
                        if (etBankAccName != null) etBankAccName.setText(user.getAccountName());
                        if (spinnerBank != null && user.getAcqId() != null) {
                            for (int i = 0; i < bankCodes.length; i++) {
                                if (bankCodes[i].equals(user.getAcqId())) {
                                    spinnerBank.setSelection(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            @Override public void onFailure(Call<SearchResponse> call, Throwable t) {}
        });

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

        android.widget.Button btnSaveBank = dialog.findViewById(R.id.btnSaveBank);
        if (btnSaveBank != null) {
            btnSaveBank.setOnClickListener(v -> {
                if (etBankAccNo == null || etBankAccName == null) return;
                String accNo = etBankAccNo.getText().toString().trim();
                String accName = etBankAccName.getText().toString().trim();
                String selectedBankCode = bankCodes[spinnerBank.getSelectedItemPosition()];

                if (accNo.isEmpty() || accName.isEmpty()) {
                    Toast.makeText(this, "Nhập đủ thông tin ngân hàng!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, String> body = new HashMap<>();
                body.put("accountNo", accNo);
                body.put("accountName", accName);
                body.put("acqId", selectedBankCode);

                apiService.setupBank(body).enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ChatActivity.this, "Lưu tài khoản thành công!", Toast.LENGTH_SHORT).show();

                            // Refresh my profile immediately so UI reflects saved bank info
                            apiService.fetchMe().enqueue(new Callback<SearchResponse>() {
                                @Override public void onResponse(Call<SearchResponse> call, Response<SearchResponse> resp) {
                                    if (resp.isSuccessful() && resp.body() != null) {
                                        User updated = resp.body().getUser();
                                        if (updated != null) {
                                            if (etBankAccNo != null) etBankAccNo.setText(updated.getAccountNo());
                                            if (etBankAccName != null) etBankAccName.setText(updated.getAccountName());
                                            if (spinnerBank != null && updated.getAcqId() != null) {
                                                for (int i = 0; i < bankCodes.length; i++) {
                                                    if (bankCodes[i].equals(updated.getAcqId())) { spinnerBank.setSelection(i); break; }
                                                }
                                            }
                                        }
                                    }
                                }
                                @Override public void onFailure(Call<SearchResponse> call, Throwable t) {}
                            });

                            if (btnTabCreate != null) btnTabCreate.performClick();
                        } else {
                            try {
                                String err = response.errorBody() != null ? response.errorBody().string() : "Lỗi không xác định";
                                Toast.makeText(ChatActivity.this, "Lỗi " + response.code() + ": " + err, Toast.LENGTH_LONG).show();
                            } catch (Exception e) { e.printStackTrace(); }
                        }
                    }
                    @Override public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(ChatActivity.this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

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

                btnSendQr.setText("Đang xử lý...");
                btnSendQr.setEnabled(false);

                Map<String, Object> body = new HashMap<>();
                body.put("amount", Integer.parseInt(amount));
                body.put("addInfo", content.isEmpty() ? "Chuyen tien" : content);

                apiService.generateQr(body).enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            try {
                                String base64String = response.body().get("qrDataURL").getAsString();
                                if (base64String.contains(",")) base64String = base64String.split(",")[1];
                                byte[] decodedBytes = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);

                                java.io.File file = new java.io.File(getCacheDir(), "qr_payment.png");
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                                fos.write(decodedBytes);
                                fos.flush(); fos.close();

                                RequestBody reqFile = RequestBody.create(MediaType.parse("image/png"), file);
                                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), reqFile);
                                RequestBody rbConvoId = RequestBody.create(MediaType.parse("text/plain"), conversationId);
                                String captionText = "💸 Yêu cầu chuyển tiền: " + amount + " VNĐ\nNội dung: " + (content.isEmpty() ? "Chuyen tien" : content);
                                RequestBody rbContent = RequestBody.create(MediaType.parse("text/plain"), captionText);

                                if ("direct".equals(conversationType)) {
                                    String recipientId = "";
                                    for (Conversation.Participant p : currentParticipants) {
                                        if (!p.getId().equals(myId)) { recipientId = p.getId(); break; }
                                    }
                                    RequestBody rbRecipient = RequestBody.create(MediaType.parse("text/plain"), recipientId);
                                    apiService.sendDirectMessage(rbRecipient, rbContent, rbConvoId, imagePart).enqueue(qrResultCallback);
                                } else {
                                    apiService.sendGroupMessage(rbConvoId, rbContent, imagePart).enqueue(qrResultCallback);
                                }
                                dialog.dismiss();
                            } catch (Exception e) { e.printStackTrace(); }
                        } else {
                            btnSendQr.setText("Gửi mã QR");
                            btnSendQr.setEnabled(true);
                            if (response.code() == 400) {
                                Toast.makeText(ChatActivity.this, "Vui lòng cài đặt ngân hàng trước!", Toast.LENGTH_SHORT).show();
                                if (btnTabSetup != null) btnTabSetup.performClick();
                            } else {
                                try {
                                    Toast.makeText(ChatActivity.this, "Lỗi: " + response.errorBody().string(), Toast.LENGTH_LONG).show();
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }
                    }
                    @Override public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        btnSendQr.setText("Gửi mã QR");
                        btnSendQr.setEnabled(true);
                    }
                });
            });
        }
        dialog.show();
    }

    private final Callback<SendMessageResponse> qrResultCallback = new Callback<SendMessageResponse>() {
        @Override
        public void onResponse(Call<SendMessageResponse> call, Response<SendMessageResponse> response) {
            if (response.isSuccessful() && response.body() != null) {
                addMessageToList(response.body().getMessage());
            }
        }
        @Override public void onFailure(Call<SendMessageResponse> call, Throwable t) {}
    };
}
