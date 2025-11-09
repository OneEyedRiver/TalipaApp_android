package com.example.talipaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.talipaapp.network.RetrofitClient;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {

    ImageView imgProduct;
    TextView txtName, txtCategory, txtPrice, txtDescription, txtUnit;
    Button btnAddToCart;

    ApiService apiService;
    SharedPreferences sharedPreferences;

    int productId, storeId, sellerId;
    String productName, productCategory, productUnit, productDescription, productImage;
    double productPrice, latitude, longitude, productQty;
    String productFreshness, harvestDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_product_detail);
        initBottomNavigation(R.id.nav_home);

        imgProduct = findViewById(R.id.imgProductDetail);
        txtName = findViewById(R.id.txtNameDetail);
        txtCategory = findViewById(R.id.txtCategoryDetail);
        txtPrice = findViewById(R.id.txtPriceDetail);
        txtUnit = findViewById(R.id.txtUnitDetail);
        txtDescription = findViewById(R.id.txtDescriptionDetail);
        btnAddToCart = findViewById(R.id.btnAddToCart);

        apiService = RetrofitClient.getInstance().create(ApiService.class);
        sharedPreferences = getSharedPreferences("com.example.talipaapp", MODE_PRIVATE);

        // ✅ Fetch data from Intent
        productId = getIntent().getIntExtra("productId", 0);
        sellerId = getIntent().getIntExtra("sellerId", 0);
        storeId = getIntent().getIntExtra("storeId", 0);
        productName = getIntent().getStringExtra("productName");
        productCategory = getIntent().getStringExtra("productCategory");
        productPrice = getIntent().getDoubleExtra("price", 0.0);
        productUnit = getIntent().getStringExtra("unit");
        productFreshness = getIntent().getStringExtra("freshness");
        productQty = getIntent().getDoubleExtra("qty", 0.0);
        harvestDate = getIntent().getStringExtra("harvestDate");
        productImage = getIntent().getStringExtra("productImage");
        productDescription = getIntent().getStringExtra("productDescription");

        // ✅ Get coordinates from Intent
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);
        // Treat sellerId as storeId for cart consistency

        Log.d("DEBUG_ADD_CART",
                "productId=" + productId +
                        ", storeId=" + storeId +
                        ", productName=" + productName +
                        ", productPrice=" + productPrice +
                        ", unit=" + productUnit +
                        ", desc=" + productDescription +
                        ", lat=" + latitude +
                        ", lng=" + longitude);

        // ✅ Show product details
        txtName.setText(productName);
        txtCategory.setText(productCategory != null ? productCategory : "Uncategorized");
        txtPrice.setText("₱" + productPrice);
        txtDescription.setText(productDescription != null ? productDescription : "No description available");
        txtUnit.setText("Per " + productUnit);
        Glide.with(this)
                .load(productImage)
                .placeholder(R.drawable.imagesnon)
                .into(imgProduct);

        btnAddToCart.setOnClickListener(v -> addToCart());
    }

    private void addToCart() {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        if (token.trim().equals("Bearer")) {
            Toast.makeText(this, "⚠️ Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("DEBUG_ADD_CART",
                "token=" + token +
                        ", productId=" + productId +
                        ", storeId=" + storeId +
                        ", productName=" + productName +
                        ", productPrice=" + productPrice +
                        ", unit=" + productUnit +
                        ", desc=" + productDescription +
                        ", lat=" + latitude +
                        ", lng=" + longitude);

        Call<JsonObject> call = apiService.addToCart(
                token,
                productId,
                storeId,
                productName != null ? productName : "",
                productPrice,
                productUnit != null ? productUnit : "pcs",
                productDescription != null ? productDescription : "",
                latitude,
                longitude,
                1
        );

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(ProductDetailActivity.this, "✅ Added to cart!", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        Log.e("DEBUG_ADD_CART_ERR", "Error: " + response.errorBody().string());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ProductDetailActivity.this, "❌ Failed to add: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
