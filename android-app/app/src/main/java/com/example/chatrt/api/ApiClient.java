package com.example.chatrt.api;

import android.content.Context;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.chatrt.models.AuthResponse;

public class ApiClient {
    // Đã đổi sang 10.0.2.2 để máy ảo có thể gọi xuống Backend ở máy thật
    // private static final String BASE_URL = "http://10.0.2.2:5001/api/";
    // MỞ link Server thật lên để chạy thực tế
    private static final String BASE_URL = "https://uit-androidproject-backend.onrender.com/api/";
    private static Retrofit retrofit = null;

    public static void resetClient() {
        retrofit = null;
    }

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            TokenManager tokenManager = new TokenManager(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // 1. Interceptor gắn Token vào Header
            Interceptor authInterceptor = chain -> {
                String token = tokenManager.getAccessToken();
                Request.Builder builder = chain.request().newBuilder();
                if (token != null) {
                    builder.addHeader("Authorization", "Bearer " + token);
                }
                return chain.proceed(builder.build());
            };

            // 2. Interceptor xử lý Refresh Token
            Interceptor refreshInterceptor = chain -> {
                Request request = chain.request();
                Response response = chain.proceed(request);

                if (response.code() == 403 && !request.url().toString().contains("auth/refresh")) {
                    synchronized (ApiClient.class) {
                        Retrofit tempRetrofit = new Retrofit.Builder()
                                .baseUrl(BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();

                        ApiService service = tempRetrofit.create(ApiService.class);
                        try {
                            retrofit2.Response<AuthResponse> refreshRes = service.refreshToken().execute();

                            if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                                response.close(); // Chỉ close khi chắc chắn thay thế bằng response mới
                                String newAccess = refreshRes.body().getAccessToken();
                                tokenManager.saveAccessToken(newAccess);

                                Request newRequest = request.newBuilder()
                                        .header("Authorization", "Bearer " + newAccess)
                                        .build();
                                return chain.proceed(newRequest);
                            } else {
                                tokenManager.clear();
                            }
                        } catch (Exception e) {
                            tokenManager.clear();
                        }
                    }
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(refreshInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}