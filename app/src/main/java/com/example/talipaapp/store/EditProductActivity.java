package com.example.talipaapp.store;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;
import com.example.talipaapp.utils.FileUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProductActivity extends AppCompatActivity {

    EditText edtName, edtPrice, edtDescription, edtHarvestDate;
    Spinner spUnit, spCategory, spFreshness;
    ImageView imgProduct;
    Switch switchAvailable;
    Button btnUpdate, btnChooseImage, btnBack;
    int productId;
    Uri imageUri;
    static final int PICK_IMAGE_REQUEST = 1;
    File imageFile;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);

        edtName = findViewById(R.id.edtProductName);
        edtPrice = findViewById(R.id.edtProductPrice);
        edtDescription = findViewById(R.id.edtDescription);
        edtHarvestDate = findViewById(R.id.edtHarvestDate);
        edtHarvestDate.setFocusable(false);
        edtHarvestDate.setOnClickListener(v -> showDatePicker());

        spUnit = findViewById(R.id.spUnit);
        spFreshness = findViewById(R.id.spFreshness);
        spCategory = findViewById(R.id.spCategory);
        imgProduct = findViewById(R.id.imgProduct);
        switchAvailable = findViewById(R.id.switchAvailable);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);

        productId = getIntent().getIntExtra("product_id", -1);
        fetchProductData(productId);

        btnChooseImage.setOnClickListener(v -> openFileChooser());
        btnUpdate.setOnClickListener(v -> updateProduct());
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(EditProductActivity.this, SellActivity.class));
            finish();
        });
    }

    private void fetchProductData(int id) {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        String token = "Bearer " + getSharedPreferences("com.example.talipaapp", MODE_PRIVATE).getString("token", "");

        Call<ApiResponse> call = api.getProductEdit(token, id);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null && response.body().getProductSingle() != null) {
                    Product product = response.body().getProductSingle();

                    edtName.setText(product.getProduct_name());
                    edtPrice.setText(String.valueOf(product.getProduct_price()));
                    edtDescription.setText(product.getProduct_description());
                    edtHarvestDate.setText(product.getHarvest_date());
                    spUnit.setSelection(getSpinnerIndex(spUnit, product.getProduct_unit()));
                    spFreshness.setSelection(getSpinnerIndex(spFreshness, product.getProduct_freshness()));
                    spCategory.setSelection(getSpinnerIndex(spCategory, product.getProduct_category()));
                    switchAvailable.setChecked(product.isIs_available());

                    Glide.with(EditProductActivity.this)
                            .load(product.getProduct_image())
                            .placeholder(R.drawable.ic_launcher_background)
                            .into(imgProduct);
                } else {
                    Toast.makeText(EditProductActivity.this, "Failed to load product", Toast.LENGTH_SHORT).show();
                    Log.e("EDIT", "Failed to load product: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(EditProductActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("EDIT", "API error: " + t.getMessage());
            }
        });
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void updateProduct() {
        String nameText = edtName.getText().toString().trim();
        String priceText = edtPrice.getText().toString().trim();
        String harvestText = edtHarvestDate.getText().toString().trim();
        String descriptionText = edtDescription.getText().toString().trim();

        // 🔍 Validation
        if (nameText.isEmpty()) {
            Toast.makeText(this, "Please enter product name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (priceText.isEmpty()) {
            Toast.makeText(this, "Please enter product price", Toast.LENGTH_SHORT).show();
            return;
        }
        if (harvestText.isEmpty()) {
            Toast.makeText(this, "Please enter harvest date", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        String token = "Bearer " + getSharedPreferences("com.example.talipaapp", MODE_PRIVATE).getString("token", "");

        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), nameText);
        RequestBody category = RequestBody.create(MediaType.parse("text/plain"), spCategory.getSelectedItem().toString());
        RequestBody price = RequestBody.create(MediaType.parse("text/plain"), priceText);
        RequestBody unit = RequestBody.create(MediaType.parse("text/plain"), spUnit.getSelectedItem().toString());
        RequestBody freshness = RequestBody.create(MediaType.parse("text/plain"), spFreshness.getSelectedItem().toString());
        RequestBody harvest = RequestBody.create(MediaType.parse("text/plain"), harvestText);
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), descriptionText);
        RequestBody availability = RequestBody.create(MediaType.parse("text/plain"), switchAvailable.isChecked() ? "1" : "0");

        MultipartBody.Part imagePart = null;
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
                imagePart = MultipartBody.Part.createFormData(
                        "product_image",
                        "upload_" + System.currentTimeMillis() + ".jpg",
                        requestFile
                );
                inputStream.close();
            } catch (IOException e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Call<ApiResponse> call = api.updateProduct(
                token, productId, name, category, price, unit,
                freshness, harvest, description, availability, imagePart
        );

        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("UPDATE_SUCCESS", new Gson().toJson(response.body()));
                        Toast.makeText(EditProductActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(EditProductActivity.this, SellActivity.class));
                        finish();
                    } else if (response.errorBody() != null) {
                        String errorRaw = response.errorBody().string();
                        Log.e("UPDATE_ERROR_RAW", errorRaw);

                        if (errorRaw.contains("must be a file of type")) {
                            Toast.makeText(EditProductActivity.this,
                                    "Image type not supported. Please upload JPG, JPEG, PNG, or GIF.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(EditProductActivity.this,
                                    "Update failed. Please check your inputs.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(EditProductActivity.this,
                                "Unknown server response.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(EditProductActivity.this, "Unexpected error occurred.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("UPDATE_FAILURE", t.getMessage(), t);
                Toast.makeText(EditProductActivity.this,
                        "Failed to connect to server. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            imgProduct.setImageURI(imageUri);
        }
    }
    private void showDatePicker() {
        // Get current date
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        // Create DatePickerDialog
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format to YYYY-MM-DD with leading zeros
                    String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    edtHarvestDate.setText(date);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

}
