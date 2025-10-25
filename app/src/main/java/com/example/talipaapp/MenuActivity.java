package com.example.talipaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talipaapp.adapters.CategoryAdapter;
import com.example.talipaapp.adapters.ProductAdapter;
import com.example.talipaapp.models.Category;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;

import com.example.talipaapp.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuActivity extends BaseActivity {

    RecyclerView recyclerProducts, recyclerCategories;
    ProductAdapter productAdapter;
    List<Product> productList = new ArrayList<>();

    ApiService apiService = RetrofitClient.getInstance().create(ApiService.class);

    EditText edtSearch;
    Button btnSearch;

    SharedPreferences sharedPreferences;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_menu);
    initBottomNavigation(R.id.nav_home);
//        setHeaderTitle("Products Menu");
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);



        sharedPreferences=
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );
        if(sharedPreferences.getString("loginStatus", "false")
                .equals("false")){
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();

        }


        edtSearch = findViewById(R.id.edtSearch);
        btnSearch = findViewById(R.id.btnSearch);


        recyclerProducts = findViewById(R.id.recyclerView);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));

        recyclerCategories = findViewById(R.id.recyclerCategories);
        recyclerCategories.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        fetchProducts(null, null);

        btnSearch.setOnClickListener(v -> {
            String query = edtSearch.getText().toString().trim();
            fetchProducts(null, query.isEmpty() ? null : query);
        });

    }

    private void fetchProducts(String category, String search) {
        progressBar.setVisibility(View.VISIBLE);
        int userId = sharedPreferences.getInt("user_id", 0);
        String token = "Bearer " + sharedPreferences.getString("token", "");
        apiService.getProducts(token, category, search, userId).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();

                    // Categories (add "All" at the beginning)
                    List<Category> categories = new ArrayList<>();
                    categories.add(new Category("All")); // <-- new “All” category
                    categories.addAll(apiResponse.getCategories());

                    CategoryAdapter categoryAdapter = new CategoryAdapter(MenuActivity.this, categories, clickedCategory -> {
                        if (clickedCategory.getProduct_category().equals("All")) {
                            // Show all products
                            fetchProducts(null, null);
                        } else {
                            // Show filtered category
                            fetchProducts(clickedCategory.getProduct_category(), null);
                        }
                    });
                    recyclerCategories.setAdapter(categoryAdapter);

                    // Products (vertical list)
                    productList = apiResponse.getProducts();
                    productAdapter = new ProductAdapter(MenuActivity.this, productList);
                    recyclerProducts.setAdapter(productAdapter);

                } else {
                    Toast.makeText(MenuActivity.this, "Failed to load products", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MenuActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


}
