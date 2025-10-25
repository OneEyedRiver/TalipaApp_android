package com.example.talipaapp.store;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talipaapp.ApiService;
import com.example.talipaapp.BaseActivity;
import com.example.talipaapp.LoginActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.adapters.CategoryAdapter;
import com.example.talipaapp.adapters.ProductAdapter;
import com.example.talipaapp.adapters.ProductAdapter1;
import com.example.talipaapp.models.Category;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SellActivity extends BaseActivity {

    SharedPreferences sharedPreferences;
    RecyclerView recyclerView, recyclerCategories;

    CategoryAdapter categoryAdapter;
    ProductAdapter1 adapter;
    List<Product> productList = new ArrayList<>();

    List<Category> categoryList = new ArrayList<>();

    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setBaseContentView(R.layout.activity_sell);
       initBottomNavigation(R.id.nav_store);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);


        recyclerView = findViewById(R.id.recyclerViewProducts);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        sharedPreferences =
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );

        String token = "Bearer " + sharedPreferences.getString("token", "");

        adapter = new ProductAdapter1(this, productList, token);
        recyclerView.setAdapter(adapter);


        recyclerCategories = findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        categoryAdapter = new CategoryAdapter(this, categoryList, category -> {
            if ("All".equalsIgnoreCase(category.getProduct_category())) {
                viewProducts(null, null); // reset filter
            } else {
                viewProducts(category.getProduct_category(), null);
            }
        });



        recyclerCategories.setAdapter(categoryAdapter);

        findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String search = ((android.widget.EditText)findViewById(R.id.edtSearch)).getText().toString().trim();
            viewProducts(null, search);
        });



        viewProducts(null, null);


        findViewById(R.id.btnAddProduct).setOnClickListener(v -> {
            Intent intent = new Intent(SellActivity.this, AddProductActivity.class);
            startActivity(intent);
        });


    }

    void viewProducts(String category, String search) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        String token = "Bearer " + sharedPreferences.getString("token", "");


        Call<ResponseBody> call = api.getUserProducts(token, category, search);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    try {
                        String res = response.body().string();
                        Log.d("API", "Response: " + res);

                        JSONObject json = new JSONObject(res);
                        String status = json.getString("Status");

                        if ("success".equals(status)) {
                            // products
                            JSONArray productsArray = json.getJSONArray("products");
                            Type listType = new TypeToken<List<Product>>() {}.getType();
                            List<Product> products = new Gson().fromJson(productsArray.toString(), listType);
                            productList.clear();
                            productList.addAll(products);
                            adapter.notifyDataSetChanged();

                            // categories (add "All" first)
                            categoryList.clear();
                            categoryList.add(new Category("All")); // ✅ default option

                            JSONArray catsArray = json.getJSONArray("categories");
                            for (int i = 0; i < catsArray.length(); i++) {
                                String catName = catsArray.getString(i);
                                categoryList.add(new Category(catName));
                            }
                            categoryAdapter.notifyDataSetChanged();

                        }
                        else if ("not_seller".equals(status)) {
                            Toast.makeText(SellActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SellActivity.this, SetUpActivity.class));
                            finish();

                        }
                        else if ("error".equals(status)) {
                            Toast.makeText(SellActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (response.code() == 401) {
                        Toast.makeText(SellActivity.this, "Unauthorized. Please log in again.", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SellActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Log.e("API", "Failed Response: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("API", "Request failed", t);
                Toast.makeText(SellActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
