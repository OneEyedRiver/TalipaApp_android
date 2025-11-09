package com.example.talipaapp.store;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;
import com.example.talipaapp.utils.FileUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddProductActivity extends AppCompatActivity {

    EditText edtName, edtPrice, edtDescription, edtHarvestDate;
    Spinner spUnit, spFreshness ,spCategory;
    ImageView imgProduct;
    Button btnChooseImage, btnUpload, btnBack;
    Uri imageUri;
    File imageFile;
    SharedPreferences sharedPreferences;

    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        edtName = findViewById(R.id.edtProductName);
        edtPrice = findViewById(R.id.edtProductPrice);
        edtDescription = findViewById(R.id.edtDescription);
        edtHarvestDate = findViewById(R.id.edtHarvestDate);
        spUnit = findViewById(R.id.spUnit);
        spFreshness = findViewById(R.id.spFreshness);
        spCategory = findViewById(R.id.spCat);
        imgProduct = findViewById(R.id.imgProduct);
        btnChooseImage = findViewById(R.id.btnChooseImage);
        btnUpload = findViewById(R.id.btnUpload);
        btnBack = findViewById(R.id.btnBack);
        sharedPreferences =
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );
        progressBar= findViewById(R.id.progressBar);



        btnChooseImage.setOnClickListener(v -> {
            // Launch gallery to pick image
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

        btnUpload.setOnClickListener(v -> uploadProduct());

        btnBack.setOnClickListener(view -> {
            startActivity(new Intent(AddProductActivity.this, SellActivity.class));
            finish();
        });


        checkStoragePermission();



        EditText edtHarvestDate = findViewById(R.id.edtHarvestDate);

        edtHarvestDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddProductActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format the date as YYYY-MM-DD
                        String date = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                        edtHarvestDate.setText(date);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgProduct.setImageURI(imageUri);
            imageFile = new File(FileUtils.getPath(this, imageUri)); // you may need a helper to get real path
        }
    }

    private void uploadProduct() {

        String nameText = edtName.getText().toString().trim();
        String priceText = edtPrice.getText().toString().trim();
        String descriptionText = edtDescription.getText().toString().trim();
        String harvestText = edtHarvestDate.getText().toString().trim();

        // 🔍 Validate required fields before upload
        // 🔍 Validate required fields before upload
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

        if (imageFile == null) {
            Toast.makeText(this, "Please choose an image", Toast.LENGTH_SHORT).show();
            return;
        }


        // ✅ If everything is okay, show progress bar
        progressBar.setVisibility(View.VISIBLE);

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);
        String token = "Bearer " + sharedPreferences.getString("token", "");

        RequestBody name = RequestBody.create(MediaType.parse("text/plain"), nameText);
        RequestBody category = RequestBody.create(MediaType.parse("text/plain"), spCategory.getSelectedItem().toString());
        RequestBody price = RequestBody.create(MediaType.parse("text/plain"), priceText);
        RequestBody unit = RequestBody.create(MediaType.parse("text/plain"), spUnit.getSelectedItem().toString());
        RequestBody freshness = RequestBody.create(MediaType.parse("text/plain"), spFreshness.getSelectedItem().toString());
        RequestBody description = RequestBody.create(MediaType.parse("text/plain"), descriptionText);
        RequestBody harvest = RequestBody.create(MediaType.parse("text/plain"), harvestText);

        // ✅ Proper image handling
        MultipartBody.Part imagePart = null;
        if (imageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);

                RequestBody requestFile = RequestBody.create(
                        MediaType.parse("image/*"),
                        bytes
                );

                imagePart = MultipartBody.Part.createFormData(
                        "product_image",
                        "upload_" + System.currentTimeMillis() + ".jpg",
                        requestFile
                );

                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to read image", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Log.d("TOKEN_CHECK", token);
        Call<ApiResponse> call = api.uploadProduct(token, name, category, price, unit, freshness, description, harvest, imagePart);
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);

                try {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d("UPLOAD_SUCCESS", new Gson().toJson(response.body()));
                        Toast.makeText(getApplicationContext(), "Uploaded Successfully", Toast.LENGTH_SHORT).show();

                        Product uploadedProduct = response.body().getProduct();
                        if (uploadedProduct != null && uploadedProduct.getProduct_image() != null) {
                            String imageUrl = uploadedProduct.getProduct_image().replace("\\/", "/");
                            Log.d("GLIDE_URL", "Loading: " + imageUrl);

                            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                                Glide.with(AddProductActivity.this)
                                        .load(imageUrl)
                                        .into(imgProduct);
                            } else {
                                Log.e("GLIDE_ERROR", "Invalid image URL: " + imageUrl);
                            }
                        }

                        Intent intent = new Intent(AddProductActivity.this, SellActivity.class);
                        startActivity(intent);
                        finish();

                    } else if (response.errorBody() != null) {
                        String errorRaw = response.errorBody().string();
                        Log.e("UPLOAD_ERROR_RAW", errorRaw);

                        if (errorRaw.contains("must be a file of type")) {
                            Toast.makeText(AddProductActivity.this,
                                    "Image type not supported. Please upload JPG, JPEG, PNG, or GIF.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AddProductActivity.this,
                                    "Upload failed. Please check your inputs.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("UPLOAD_UNKNOWN", "Empty response");
                        Toast.makeText(AddProductActivity.this,
                                "Unknown server response. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(AddProductActivity.this,
                            "An unexpected error occurred.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Log.e("UPLOAD_FAILURE", t.getMessage(), t);
                Toast.makeText(AddProductActivity.this,
                        "Failed to connect to server. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static final int REQUEST_CODE_READ_STORAGE = 100;

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_STORAGE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
            }
//            else {
//                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
//            }
        }
    }



}
