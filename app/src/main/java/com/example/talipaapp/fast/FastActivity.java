package com.example.talipaapp.fast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.BaseActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.adapters.StoreTableAdapter;
import com.example.talipaapp.models.Fast.ProductFast;
import com.example.talipaapp.models.Fast.SearchResponse;
import com.example.talipaapp.models.Fast.StoreFast;
import com.example.talipaapp.models.Fast.StoreProductRow;
import com.example.talipaapp.network.RetrofitClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.preference.PreferenceManager;

import android.content.Intent;
import android.net.Uri;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.gson.JsonObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.gson.JsonObject;



public class FastActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private RecyclerView rvStoreTable;
    private StoreTableAdapter storeTableAdapter;
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    // Current location to use (either GPS or tapped)
    private double currentLat = 14.5995;   // Default Manila lat
    private double currentLng = 120.9842;  // Default Manila lng
    private boolean userTappedMap = false; // true if user taps the map
    ProgressBar progressBar;
    private static final int IMAGE_PICK_REQUEST = 101;
    private static final int CAMERA_REQUEST = 102;
    private Uri photoUri;

    private static final int AUDIO_PERMISSION_REQUEST = 103;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;

    private Button btnRelocateUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_fast);
        initBottomNavigation(R.id.nav_search1);
        sharedPreferences = getApplication().getSharedPreferences(
                "com.example.talipaapp", MODE_PRIVATE
        );

        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(this));



        setHeaderTitle("Products Menu");

        // Map setup
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(currentLat, currentLng));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        apiService = RetrofitClient.getInstance().create(ApiService.class);

        // Map tap to change user location
        MapEventsReceiver mapReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                currentLat = p.getLatitude();
                currentLng = p.getLongitude();
                userTappedMap = true;

                // Update marker immediately
                map.getOverlays().removeIf(o -> o instanceof Marker && ((Marker)o).getTitle() != null && ((Marker)o).getTitle().equals("You are here"));
                addUserMarker(currentLat, currentLng);

                Toast.makeText(FastActivity.this,
                        "Location set to: " + currentLat + ", " + currentLng,
                        Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        map.getOverlays().add(new MapEventsOverlay(mapReceiver));
        progressBar= findViewById(R.id.progressBar);
        // UI: search button
        EditText edtSearch = findViewById(R.id.edtSearch);
        Button btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String query = edtSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                getUserLocationAndSearch(query);
            }
        });

        rvStoreTable = findViewById(R.id.rvStoreTable);
        rvStoreTable.setLayoutManager(new LinearLayoutManager(this));
        Button btnImageOptions = findViewById(R.id.btnImageOptions);
        btnImageOptions.setOnClickListener(v -> {
            // Dialog with two choices
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(FastActivity.this);
            builder.setTitle("Select Option")
                    .setItems(new CharSequence[]{"Choose from Gallery", "Take Photo"}, (dialog, which) -> {
                        if (which == 0) {
                            openImagePicker(); // gallery
                        } else {
                            openCamera(); // camera
                        }
                    })
                    .show();
        });

        btnRelocateUser = findViewById(R.id.btnRelocateUser);
        Button btnRecordAudio = findViewById(R.id.btnRecordAudio);
        btnRecordAudio.setOnClickListener(v -> toggleAudioRecording());


        btnRelocateUser.setOnClickListener(v -> {
            // Check location permissions
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Request permissions if not granted
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        1001);
                return; // Wait until user grants permission
            }

            // Permissions granted, get current location
            fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                }

                // 🧹 Remove previous user marker before adding a new one
                map.getOverlays().removeIf(o ->
                        o instanceof Marker && ((Marker)o).getTitle() != null && ((Marker)o).getTitle().equals("You are here")
                );

                // Add new marker
                addUserMarker(currentLat, currentLng);

                // Center map
                map.getController().setZoom(15.0);
                map.getController().setCenter(new GeoPoint(currentLat, currentLng));

                Toast.makeText(FastActivity.this, "User relocated!", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(FastActivity.this, "Failed to get location, using last known", Toast.LENGTH_SHORT).show();

                // 🧹 Also remove old marker before re-adding
                map.getOverlays().removeIf(o ->
                        o instanceof Marker && ((Marker)o).getTitle() != null && ((Marker)o).getTitle().equals("You are here")
                );

                addUserMarker(currentLat, currentLng);
                map.getController().setCenter(new GeoPoint(currentLat, currentLng));
            });
        });

    }

    private void addUserMarker(double lat, double lng) {
        GeoPoint userPoint = new GeoPoint(lat, lng);
        Marker userMarker = new Marker(map);
        userMarker.setPosition(userPoint);
        userMarker.setTitle("You are here"); // user marker title

        // Custom user icon
        Drawable userIcon = ContextCompat.getDrawable(this, R.drawable.ic_user_marker); // create ic_user_marker.png
        userMarker.setIcon(userIcon);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        map.getOverlays().add(userMarker);
        map.getController().setCenter(userPoint);
    }

    private void checkLocationPermissionAndSearch(String query) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        } else {
            getUserLocationAndSearch(query);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted — now open the camera
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getUserLocationAndSearch(String query) {
        if (!userTappedMap) { // Only get GPS if user didn't tap
            fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
            ).addOnSuccessListener(location -> {
                if (location != null) {
                    currentLat = location.getLatitude();
                    currentLng = location.getLongitude();
                }
                searchProducts(query, currentLat, currentLng);
            }).addOnFailureListener(e -> searchProducts(query, currentLat, currentLng));
        } else {
            searchProducts(query, currentLat, currentLng);
        }
    }

    private void searchProducts(String query, double lat, double lng) {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        apiService.fastSearch(token, query, lat, lng).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("DEBUG", new Gson().toJson(response.body())); // <-- add this
                    showResultsOnMap(response.body().stores);
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(FastActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResultsOnMap(List<StoreFast> stores) {
        map.getOverlays().removeIf(o -> o instanceof Marker && ((Marker)o).getTitle() != null && ((Marker)o).getTitle().equals("Store"));

        // Add user marker
        addUserMarker(currentLat, currentLng);

        for (StoreFast store : stores) {
            GeoPoint storePoint = new GeoPoint(store.latitude, store.longitude);
            Marker storeMarker = new Marker(map);
            storeMarker.setPosition(storePoint);
            storeMarker.setTitle("Store");

            Drawable storeIcon = ContextCompat.getDrawable(this, R.drawable.ic_store_marker);
            storeMarker.setIcon(storeIcon);
            storeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            storeMarker.setOnMarkerClickListener((marker, mapView) -> {
                showStoreProductsDialog(store);
                return true;
            });

            map.getOverlays().add(storeMarker);
        }

        map.invalidate();

        // -----------------------------
        // FLATTEN STORE + PRODUCTS FOR TABLE
        List<StoreProductRow> tableRows = new ArrayList<>();
        for (StoreFast store : stores) {
            String storeAddress = store.store_address != null ? store.store_address : "";
            if (store.matched_products != null) {
                for (ProductFast product : store.matched_products) {
                    tableRows.add(new StoreProductRow(
                            store.store_name,
                            storeAddress,
                            product.product_name,
                            product.price,
                            store.latitude,
                            store.longitude,
                            store // <-- pass store here
                    ));
                }
            }
        }


        // Sort by price ascending
        Collections.sort(tableRows, (a, b) -> Double.compare(a.price, b.price));

        // Pass tableRows to your RecyclerView adapter
        if (storeTableAdapter == null) {
            storeTableAdapter = new StoreTableAdapter(tableRows);
            rvStoreTable.setAdapter(storeTableAdapter);


            // Add this here
            storeTableAdapter.setOnRowClickListener(row -> {
                // Zoom to store
                GeoPoint point = new GeoPoint(row.latitude, row.longitude);
                map.getController().setZoom(15.0);
                map.getController().setCenter(point);

                // Find the existing marker for this store
                for (int i = 0; i < map.getOverlays().size(); i++) {
                    if (map.getOverlays().get(i) instanceof Marker) {
                        Marker marker = (Marker) map.getOverlays().get(i);
                        if (marker.getPosition().getLatitude() == row.latitude
                                && marker.getPosition().getLongitude() == row.longitude) {
                            // Optional: highlight marker
                            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_store_marker));
                            break;
                        }
                    }
                }

                // Show the modal using the store object
                showStoreProductsDialog(row.store);
            });


        } else {
            storeTableAdapter.setData(tableRows);
        }

    }

    private void showStoreProductsDialog(StoreFast store) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_store_products);
        dialog.setCancelable(true);

        TextView tvStoreName = dialog.findViewById(R.id.tvStoreName);
        LinearLayout llProductsContainer = dialog.findViewById(R.id.llProductsContainer);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        tvStoreName.setText(store.store_name);
        llProductsContainer.removeAllViews();

        for (ProductFast product : store.matched_products) {
            android.view.View productView = getLayoutInflater().inflate(R.layout.item_store_product, null);
            TextView tvProductName = productView.findViewById(R.id.tvProductName);
            TextView tvProductPrice = productView.findViewById(R.id.tvProductPrice);
            TextView tvProductDesc = productView.findViewById(R.id.tvProductDesc);
            Button btnAddToCart = productView.findViewById(R.id.btnAddToCart);
            ImageView imgProduct = productView.findViewById(R.id.imgProduct);

            tvProductName.setText(product.product_name);
            tvProductPrice.setText("₱" + product.price); // format as needed
            tvProductDesc.setText(product.description);

            // TODO: load product image (if URL exists)
            // Use Glide or Picasso if image URLs exist
            // Glide.with(this).load(product.image_url).into(imgProduct);

            if (product.image_url != null && !product.image_url.isEmpty()) {
                Glide.with(this).load(product.image_url).into(imgProduct);
            }

            btnAddToCart.setOnClickListener(v -> {
                String token = "Bearer " + sharedPreferences.getString("token", "");

                // Create request
                Call<JsonObject> call = apiService.addToCart(
                        token,
                        product.id, // ensure ProductFast has an id field
                        store.id,   // ensure StoreFast has an id field
                        product.product_name,
                        product.price,
                        product.unit != null ? product.unit : "pcs",
                        product.description != null ? product.description : "",
                        store.latitude,
                        store.longitude,
                        1 // default quantity
                );

                call.enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(FastActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(FastActivity.this, "Failed to add: " + response.message(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Toast.makeText(FastActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });


            llProductsContainer.addView(productView);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }



    private void openImagePicker() {

        progressBar.setVisibility(View.VISIBLE);
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_REQUEST && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) recognizeImage(imageUri);
            } else if (requestCode == CAMERA_REQUEST && photoUri != null) {
                recognizeImage(photoUri);
            }
        }
    }


    private void recognizeImage(Uri imageUri) {
        try {
            progressBar.setVisibility(View.VISIBLE);

            // ✅ Compress image before sending
            byte[] bytes = compressImage(imageUri);

            // Prepare request body
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "upload.jpg", requestFile);

            // Get token
            String token = "Bearer " + sharedPreferences.getString("token", "");

            // Send to API
            apiService.describeImage(token, body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        String recognized = response.body().get("object").getAsString();
                        EditText edtSearch = findViewById(R.id.edtSearch);
                        edtSearch.setText(recognized);
                        Toast.makeText(FastActivity.this, "Recognized: " + recognized, Toast.LENGTH_SHORT).show();

                        // Auto-search after recognition
                        getUserLocationAndSearch(recognized);
                    } else {
                        Toast.makeText(FastActivity.this, "Failed to recognize image", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(FastActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        progressBar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = File.createTempFile(
                        "captured_image",
                        ".jpg",
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                );
                photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            } catch (IOException e) {
                Toast.makeText(this, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void toggleAudioRecording() {
        if (!isRecording) {
            // Start Recording
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_REQUEST);
                return;
            }

            try {
                File dir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                audioFile = new File(dir, "recorded_audio.mp3");

                mediaRecorder = new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
                mediaRecorder.prepare();
                mediaRecorder.start();

                isRecording = true;
                Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error starting recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        } else {
            // Stop Recording
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;

                Toast.makeText(this, "Recording stopped.", Toast.LENGTH_SHORT).show();
                uploadAudioFile(audioFile);
            } catch (Exception e) {
                Toast.makeText(this, "Error stopping recording: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    private void uploadAudioFile(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "No audio file found!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("audio", file.getName(), requestFile);

        String token = "Bearer " + sharedPreferences.getString("token", "");
        apiService.describeUploadedAudio_droid(token, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = response.body();

                    String text = "";
                    if (json.has("transcription")) {
                        text = json.get("transcription").getAsString();
                    }

                    EditText edtSearch = findViewById(R.id.edtSearch);
                    edtSearch.setText(text);
                    getUserLocationAndSearch(text);

                    Toast.makeText(FastActivity.this, "Recognized: " + text, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FastActivity.this, "Recognition failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(FastActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private byte[] compressImage(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // reduce to 70% quality
        return baos.toByteArray();
    }

}
