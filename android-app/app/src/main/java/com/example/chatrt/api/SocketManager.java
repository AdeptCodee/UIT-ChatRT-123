package com.example.chatrt.api;

import android.content.Context;
import android.util.Log;
import com.example.chatrt.models.*;
import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {
    private static final String TAG = "SocketManager";
    private static final String SOCKET_URL = "https://uit-androidproject-backend.onrender.com";
//    private static final String SOCKET_URL = "http://10.0.2.2:5001";
    private static SocketManager instance;
    private Socket mSocket;
    private final Gson gson = new Gson();
    private final TokenManager tokenManager;
    private final List<String> onlineUserIds = new CopyOnWriteArrayList<>();

    // Danh sách các màn hình đang nghe
    public interface MessageListener { void onNewMessage(Message message); }
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();

    public interface OnOnlineUsersChangedListener { void onUpdate(List<String> onlineIds); }
    private final List<OnOnlineUsersChangedListener> onlineListeners = new CopyOnWriteArrayList<>();

    public interface ConversationUpdateListener { void onConversationUpdated(JSONObject data); }
    private final List<ConversationUpdateListener> convoListeners = new CopyOnWriteArrayList<>();

    private SocketManager(Context context) {
        tokenManager = new TokenManager(context);
    }

    public static synchronized SocketManager getInstance(Context context) {
        if (instance == null) instance = new SocketManager(context.getApplicationContext());
        return instance;
    }

    public void connect() {
        if (mSocket != null && mSocket.connected()) return;

        try {
            Log.d(TAG, "Đang kết nối Socket...");
            IO.Options opts = new IO.Options();
            opts.auth = Collections.singletonMap("token", tokenManager.getAccessToken());
            opts.transports = new String[]{"websocket"};
            opts.forceNew = true;

            mSocket = IO.socket(SOCKET_URL, opts);

            mSocket.on(Socket.EVENT_CONNECT, args -> Log.d(TAG, "Socket Connected!"));
            
            mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.d(TAG, "Socket Disconnected!"));

            // Cập nhật trạng thái online
            mSocket.on("online-users", args -> {
                try {
                    JSONArray arr = (JSONArray) args[0];
                    onlineUserIds.clear();
                    for (int i = 0; i < arr.length(); i++) onlineUserIds.add(arr.getString(i));
                    List<String> copy = new ArrayList<>(onlineUserIds);
                    for (OnOnlineUsersChangedListener l : onlineListeners) l.onUpdate(copy);
                } catch (Exception e) { Log.e(TAG, "Error online-users"); }
            });

            // Nhận tin nhắn mới
            mSocket.on("new-message", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    Message msg = gson.fromJson(data.getJSONObject("message").toString(), Message.class);
                    // Báo cho màn hình Chat chi tiết
                    for (MessageListener l : messageListeners) l.onNewMessage(msg);
                    // Báo cho màn hình Danh sách (để cập nhật số unread/tin nhắn cuối)
                    for (ConversationUpdateListener l : convoListeners) l.onConversationUpdated(data);
                } catch (Exception e) { Log.e(TAG, "Error new-message"); }
            });

            // Nhận thông báo đã đọc
            mSocket.on("read-message", args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    // Báo cho màn hình Danh sách (để xóa Badge số unread)
                    for (ConversationUpdateListener l : convoListeners) l.onConversationUpdated(data);
                } catch (Exception e) { Log.e(TAG, "Error read-message"); }
            });

            mSocket.connect();
        } catch (URISyntaxException e) { Log.e(TAG, e.getMessage()); }
    }

    public void disconnect() {
        if (mSocket != null) {
            Log.d(TAG, "Thực hiện ngắt kết nối Socket...");
            mSocket.off(); 
            mSocket.disconnect();
            mSocket.close();
            mSocket = null;
            onlineUserIds.clear();
            Log.d(TAG, "Socket đã được giải phóng.");
        }
    }

    public boolean isUserOnline(String userId) { return userId != null && onlineUserIds.contains(userId); }
    public void addMessageListener(MessageListener l) { if (!messageListeners.contains(l)) messageListeners.add(l); }
    public void removeMessageListener(MessageListener l) { messageListeners.remove(l); }
    public void addOnlineUsersListener(OnOnlineUsersChangedListener l) { if (!onlineListeners.contains(l)) onlineListeners.add(l); }
    public void removeOnlineUsersListener(OnOnlineUsersChangedListener l) { onlineListeners.remove(l); }
    public void addConvoListener(ConversationUpdateListener l) { if (!convoListeners.contains(l)) convoListeners.add(l); }
    public void removeConvoListener(ConversationUpdateListener l) { convoListeners.remove(l); }
}