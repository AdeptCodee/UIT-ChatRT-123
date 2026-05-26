package com.example.chatrt.api;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.chatrt.models.AuthResponse;

public class ApiClient {
    // KHÔI PHỤC URL CHUẨN từ mobile_reference
    private static final String BASE_URL = "https://uit-chatrt-123-backend.onrender.com/api/";
    private static Retrofit retrofit = null;

    // CookieJar để quản lý refreshToken cookie (Backend yêu cầu Cookie để làm mới token)
    private static final CookieJar cookieJar = new CookieJar() {
        private final Map<String, List<Cookie>> cookieStore = new HashMap<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            cookieStore.put(url.host(), cookies);
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = cookieStore.get(url.host());
            return cookies != null ? cookies : new ArrayList<>();
        }
    };

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

                // Nếu nhận mã 403 (Token hết hạn), thử tự động làm mới
                if (response.code() == 403 && !request.url().toString().contains("auth/refresh")) {
                    synchronized (ApiClient.class) {
                        // Gọi API refresh có kèm theo Cookie quản lý bởi cookieJar
                        Retrofit tempRetrofit = new Retrofit.Builder()
                                .baseUrl(BASE_URL)
                                .addConverterFactory(GsonConverterFactory.create())
                                .client(new OkHttpClient.Builder().cookieJar(cookieJar).build())
                                .build();

                        ApiService service = tempRetrofit.create(ApiService.class);
                        try {
                            retrofit2.Response<AuthResponse> refreshRes = service.refreshToken().execute();

                            if (refreshRes.isSuccessful() && refreshRes.body() != null) {
                                response.close();
                                String newAccess = refreshRes.body().getAccessToken();
                                tokenManager.saveAccessToken(newAccess);

                                Request newRequest = request.newBuilder()
                                        .header("Authorization", "Bearer " + newAccess)
                                        .build();
                                return chain.proceed(newRequest);
                            } else {
                                // Nếu refresh lỗi (401/403), mới xóa token để yêu cầu login lại
                                if (refreshRes.code() == 401 || refreshRes.code() == 403) {
                                    tokenManager.clear();
                                }
                            }
                        } catch (Exception e) {
                            // Lỗi mạng tạm thời, không xóa token
                        }
                    }
                }
                return response;
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
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
