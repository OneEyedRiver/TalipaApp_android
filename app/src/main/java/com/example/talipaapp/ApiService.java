package com.example.talipaapp;


import com.example.talipaapp.models.Fast.DishResponse;
import com.example.talipaapp.models.Fast.SearchResponse;
import com.example.talipaapp.models.Store;
import com.example.talipaapp.network.ApiResponse;
import com.google.gson.JsonObject;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {



    @POST("/api/registerApi")
    Call<ResponseBody> postRegister(

            @Body User user
    );


    @POST("/api/loginApi")
    Call<ResponseBody> postLogin(
           // @Header("Authorization") String authToken,
            @Body User user
    );

    @POST("/api/list")
    Call<ResponseBody> postList(
            @Header("Authorization") String authToken

    );
    @GET("/api/showMenu")
    Call<ApiResponse> getProducts(
            @Header("Authorization") String token,
            @Query("cat_name") String category,
            @Query("search") String search,
            @Query("user_id") int userId
    );



    @GET("api/sellViewApi")
    Call<ResponseBody> getUserProducts(
            @Header("Authorization") String authToken,
            @Query("cat_name") String category,
            @Query("search") String search
    );


    @Multipart
    @POST("api/products")
    Call<ApiResponse> uploadProduct(
            @Header("Authorization") String authToken,
            @Part("product_name") RequestBody name,
            @Part("product_category") RequestBody category,
            @Part("product_price") RequestBody price,
            @Part("product_unit") RequestBody unit,
            @Part("product_freshness") RequestBody freshness,
            @Part("product_description") RequestBody description,
            @Part("harvest_date") RequestBody harvest,
            @Part MultipartBody.Part product_image
    );


    @GET("api/getProduct/{id}")
    Call<ApiResponse> getProductEdit(
            @Header("Authorization") String authToken,
            @Path("id") int productId
    );


    @Multipart
    @POST("api/updateProduct/{id}")
    Call<ApiResponse> updateProduct(
            @Header("Authorization") String authToken,
            @Path("id") int productId,
            @Part("product_name") RequestBody name,
            @Part("product_category") RequestBody category,
            @Part("product_price") RequestBody price,
            @Part("product_unit") RequestBody unit,
            @Part("product_freshness") RequestBody freshness,
            @Part("harvest_date") RequestBody harvestDate,
            @Part("product_description") RequestBody description,
            @Part("product_availability") RequestBody availability,
            @Part MultipartBody.Part product_image // optional
    );


    @DELETE("api/deleteProduct/{id}")
    Call<ApiResponse> deleteProduct(
            @Header("Authorization") String authToken,
            @Path("id") int productId
    );


    @POST("api/saveStore")
    Call<JsonObject> saveStore(
            @Header("Authorization") String authToken, // "Bearer <token>"
            @Body Store store
    );

    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST("api/fastSearchApi")
    Call<SearchResponse> fastSearch(
            @Header("Authorization") String token,
            @Field("search") String search,
            @Field("latitude") double latitude,
            @Field("longitude") double longitude
    );

    @Multipart
    @POST("api/describeUploadedImage_droid")
    Call<JsonObject> describeImage(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );
    @Multipart
    @POST("api/describeUploadedImageDish_droid")
    Call<JsonObject> describeImageDish(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image
    );
    @Multipart
    @POST("api/describeUploadedAudio_droid")
    Call<JsonObject> describeUploadedAudio_droid(
            @Header("Authorization") String token,
            @Part MultipartBody.Part audio
    );

    @Headers("Content-Type: application/json")
    @POST("api/describeDish_droid")
    Call<DishResponse> getDishIngredients(
            @Header("Authorization") String token,
            @Body Map<String, String> body
    );
    @POST("api/storeCart_droid")
    @FormUrlEncoded
    Call<JsonObject> addToCart(
            @Header("Authorization") String token,
            @Field("product_id") int productId,
            @Field("store_id") int storeId,
            @Field("product_name") String productName,
            @Field("product_price") double productPrice,
            @Field("product_unit") String productUnit,
            @Field("product_description") String productDescription,
            @Field("latitude") double latitude,
            @Field("longitude") double longitude,
            @Field("quantity") int quantity
    );


    @GET("api/userCart_droid")
    Call<ApiResponse> getUserCart(@Header("Authorization") String token);

    @FormUrlEncoded
    @POST("api/updateCart_droid/{id}")
    Call<ApiResponse> updateCart_droid(
            @Header("Authorization") String token,
            @Path("id") int cartId,
            @Field("quantity") int quantity
    );

    @DELETE("api/deleteCart_droid/{id}")
    Call<ApiResponse> deleteCart_droid(
            @Header("Authorization") String token,
            @Path("id") int cartId
    );
    @FormUrlEncoded
    @POST("api/updateAvailability/{id}")
    Call<ApiResponse> updateAvailability(
            @Header("Authorization") String authToken,
            @Path("id") int productId,
            @Field("is_available") int isAvailable // ← changed to int
    );


}
