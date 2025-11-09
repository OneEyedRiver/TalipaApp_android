package com.example.talipaapp.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

 private static final String BASE_URL = "https://talipaapp.shop/";
//private static final String BASE_URL = "http://172.25.48.1:8000/";
    private static Retrofit retrofit;

    public static Retrofit getInstance() {
        if (retrofit == null) {

            // Create logging interceptor
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Add interceptor to OkHttpClient
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .build();

            // Build Retrofit instance
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // domain only, do not include /api/
                    .client(client)    // attach logging client
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
