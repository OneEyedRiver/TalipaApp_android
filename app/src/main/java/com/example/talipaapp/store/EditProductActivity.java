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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;
import com.example.talipaapp.utils.FileUtils;

import java.io.File;

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
        spUnit = findViewById(R.id.spUnit);
        spFreshness = findViewById(R.id.spFreshness);
        spCategory = findViewById(R.id.spCategory);
        imgProduct = findViewById(R.id.imgProduct);
        switchAvailable = findViewById(R.id.switchAvailable);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnBack=findViewById(R.id.btnBack);
        progressBar= findViewById(R.id.progressBar);


        productId = getIntent().getIntExtra("product_id", -1);

        fetchProductData(productId);

        btnChooseImage.setOnClickListener(v -> openFileChooser());

        btnUpdate.setOnClickListener(v -> {
            updateProduct(); // <-- now the function is actually called
        });
        btnBack.setOnClickListener(view -> {
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
                    Product product = response.body().getProductSingle(); // single object

                    // Fill the fields
                    edtName.setText(product.getProduct_name());
                    edtPrice.setText(String.valueOf(product.getProduct_price()));
                    edtDescription.setText(product.getProduct_description());
                    edtHarvestDate.setText(product.getHarvest_date());

                    // Spinner selections (unit, freshness, category)
                    spUnit.setSelection(getSpinnerIndex(spUnit, product.getProduct_unit()));
                    spFreshness.setSelection(getSpinnerIndex(spFreshness, product.getProduct_freshness()));
                    spCategory.setSelection(getSpinnerIndex(spCategory, product.getProduct_category()));

                    // Availability switch
                    switchAvailable.setChecked(product.isIs_available());

                    // Load image
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
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }
    private void updateProduct() {
        progressBar.setVisibility(View.VISIBLE);
        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        String token = "Bearer " + getSharedPreferences("com.example.talipaapp", MODE_PRIVATE).getString("token", "");


        // Convert inputs to RequestBody
        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), edtName.getText().toString().trim());
        RequestBody category = RequestBody.create(MediaType.parse("text/plain"), spCategory.getSelectedItem().toString());
        RequestBody price = RequestBody.create(MediaType.parse("text/plain"), edtPrice.getText().toString().trim());
        RequestBody unit = RequestBody.create(MediaType.parse("text/plain"), spUnit.getSelectedItem().toString());
        RequestBody freshness = RequestBody.create(MediaType.parse("text/plain"), spFreshness.getSelectedItem().toString());
        RequestBody harvestDate = RequestBody.create(MediaType.parse("text/plain"), edtHarvestDate.getText().toString().trim());
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), edtDescription.getText().toString().trim());
        RequestBody availability = RequestBody.create(MediaType.parse("text/plain"), switchAvailable.isChecked() ? "1" : "0");

        MultipartBody.Part imagePart = null;

        if (imageUri != null) {
            File file = new File(FileUtils.getPath(this, imageUri)); // FileUtils: helper to get real file path
            RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(imageUri)), file);
            imagePart = MultipartBody.Part.createFormData("product_image", file.getName(), requestFile);
        }

        // Call API
        Call<ApiResponse> call = api.updateProduct(
                token,
                productId,
                name,
                category,
                price,
                unit,
                freshness,
                harvestDate,
                description,
                availability,
                imagePart
        );

        // Enqueue so it actually executes
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(EditProductActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(EditProductActivity.this, SellActivity.class);
                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(EditProductActivity.this, "Failed to update product: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("UPDATE", "Error: " + response.code() + " " + response.message());
                    if (response.errorBody() != null) {
                        try {
                            Log.e("UPDATE", "Error body: " + response.errorBody().string());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toast.makeText(EditProductActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("UPDATE", "API failure: " + t.getMessage());
                progressBar.setVisibility(View.GONE);
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData(); // store URI
            imgProduct.setImageURI(imageUri); // update preview immediately
        }
    }


}
