package com.example.talipaapp.fast;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.Dialog;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.talipaapp.ApiService;
import com.example.talipaapp.BaseActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.models.Fast.DishResponse;
import com.example.talipaapp.models.Fast.ProductFast;
import com.example.talipaapp.models.Fast.SearchResponse;
import com.example.talipaapp.models.Fast.StoreFast;
import com.example.talipaapp.models.Fast.StoreProductRow;
import com.example.talipaapp.network.RetrofitClient;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupActivity extends BaseActivity {
    ProgressBar progressBar;
    private EditText inputDish;
    private Button btnSearch, btnAdd;
    private FlexboxLayout ingredientsContainer;

    private SharedPreferences sharedPreferences;
    private ApiService apiService;
    // RecyclerView + Adapter for table
    private androidx.recyclerview.widget.RecyclerView rvStoreTable;
    private com.example.talipaapp.adapters.StoreTableAdapter storeTableAdapter;

    // Map + location
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLat = 14.5995;   // default Manila
    private double currentLng = 120.9842;
    private boolean userTappedMap = false;
    private Button btnRelocateUser;
    private static final String KEY_INGREDIENTS_JSON = "ingredients_json";

    private Button btnRecognizeImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    private Button btnCamera;


    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri cameraImageUri;
    private File cameraPhotoFile;     // store the actual camera file
    private static final int IMAGE_PICK_REQUEST = 101;
    private static final int CAMERA_REQUEST = 102;

    private Uri photoUri;
    private Button btnVoice;
    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private Button btnTagRemover;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_group);
        initBottomNavigation(R.id.nav_search2);
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(this));




        initViews();
        initMap();
        initApi();

        loadAllDishes();
        loadManualIngredients();
        loadLastDish();

        setupListeners();
        setupImagePicker();
        btnVoice = findViewById(R.id.btnVoice);

        btnVoice.setOnClickListener(v -> {
            if (!isRecording) {
                progressBar.setVisibility(View.VISIBLE);
                startRecording();
            } else {
                stopRecordingAndSend();
            }
        });

    }

    private void initViews() {
        inputDish = findViewById(R.id.inputDish);
        btnSearch = findViewById(R.id.btnSearch);
        btnAdd = findViewById(R.id.btnAdd);
        ingredientsContainer = findViewById(R.id.ingredientsContainer);
        rvStoreTable = findViewById(R.id.rvStoreTable);
        rvStoreTable.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        btnRelocateUser = findViewById(R.id.btnRelocateUser);
        sharedPreferences = getApplication().getSharedPreferences("com.example.talipaapp", MODE_PRIVATE);
        btnRecognizeImage = findViewById(R.id.btnRecognizeImage);
        progressBar=findViewById(R.id.progressBar);
        btnTagRemover = findViewById(R.id.btnTagRemover);



    }

    private void initApi() {
        apiService = RetrofitClient.getInstance().create(ApiService.class);
    }

    private void initMap() {
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(12.0);
        map.getController().setCenter(new GeoPoint(currentLat, currentLng));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Map tap - allow user to choose custom location
        MapEventsReceiver mapReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                currentLat = p.getLatitude();
                currentLng = p.getLongitude();
                userTappedMap = true;

                // remove previous "You are here" markers and add the new one
                map.getOverlays().removeIf(o -> o instanceof Marker && "You are here".equals(((Marker)o).getTitle()));
                addUserMarker(currentLat, currentLng);

                Toast.makeText(GroupActivity.this,
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

        // Add initial user marker
        addUserMarker(currentLat, currentLng);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            String dish = inputDish.getText().toString().trim();
            if (dish.isEmpty()) {
                Toast.makeText(GroupActivity.this, "Enter a dish name", Toast.LENGTH_SHORT).show();
                return;
            }
            fetchDishFromServer(dish);
        });

        btnAdd.setOnClickListener(v -> {
            String newIngredient = inputDish.getText().toString().trim();
            if (newIngredient.isEmpty()) {
                Toast.makeText(GroupActivity.this, "Enter an ingredient name", Toast.LENGTH_SHORT).show();
                return;
            }
            addIngredientTag(newIngredient);
            saveIngredientToPrefs(newIngredient);
            inputDish.setText("");
        });
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
                // Add/update user marker
                addUserMarker(currentLat, currentLng);

                // Center map
                map.getController().setZoom(15.0);
                map.getController().setCenter(new GeoPoint(currentLat, currentLng));

                Toast.makeText(GroupActivity.this, "User relocated!", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(GroupActivity.this, "Failed to get location, using last known", Toast.LENGTH_SHORT).show();
                addUserMarker(currentLat, currentLng);
                map.getController().setCenter(new GeoPoint(currentLat, currentLng));
            });
        });
        btnTagRemover.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(GroupActivity.this)
                    .setTitle("Remove All Tags")
                    .setMessage("Are you sure you want to remove all ingredients?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        ingredientsContainer.removeAllViews();
                        sharedPreferences.edit().remove("manual_ingredients").apply();
                        sharedPreferences.edit().remove("combined_dishes").apply();
                        Toast.makeText(GroupActivity.this, "All tags removed!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


    }

    // --- Fetch dish from API (your existing backend that returns ingredients) ---
    private void fetchDishFromServer(String dish) {
        String rawToken = sharedPreferences.getString("token", null);
        if (rawToken == null || rawToken.isEmpty()) {
            Toast.makeText(this, "Please log in first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + rawToken;
        Map<String, String> body = new HashMap<>();
        body.put("dish", dish);

        apiService.getDishIngredients(token, body).enqueue(new Callback<DishResponse>() {
            @Override
            public void onResponse(Call<DishResponse> call, Response<DishResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(GroupActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                DishResponse dishResponse = response.body();
                saveDishToPrefs(dishResponse);
                displayIngredients(dishResponse);
            }

            @Override
            public void onFailure(Call<DishResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(GroupActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Save & Load Dishes (unchanged logic you had) ---
    private void saveDishToPrefs(DishResponse newDish) { /* keep your existing saveDishToPrefs code */
        try {
            String existingData = sharedPreferences.getString("saved_dishes", "[]");
            JSONArray dishesArray = new JSONArray(existingData);

            String combinedData = sharedPreferences.getString("combined_dishes", "[]");
            JSONArray combinedArray = new JSONArray(combinedData);

            JSONObject newDishObj = new JSONObject();
            newDishObj.put("dish", newDish.getDish());

            JSONArray ingredientsArray = new JSONArray();
            for (String ingredient : newDish.getIngredients()) {
                ingredientsArray.put(ingredient);
            }
            newDishObj.put("ingredients", ingredientsArray);

            boolean exists = false;
            for (int i = 0; i < dishesArray.length(); i++) {
                JSONObject existingDish = dishesArray.getJSONObject(i);
                if (existingDish.getString("dish").equalsIgnoreCase(newDish.getDish())) {
                    dishesArray.put(i, newDishObj);
                    exists = true;
                    break;
                }
            }

            if (!exists) dishesArray.put(newDishObj);
            combinedArray.put(newDishObj);

            sharedPreferences.edit()
                    .putString("saved_dishes", dishesArray.toString())
                    .putString("combined_dishes", combinedArray.toString())
                    .apply();

            Log.d("GroupActivity", "✅ Saved dish: " + newDish.getDish());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(GroupActivity.this, "Failed to save dish", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLastDish() { /* keep your loadLastDish code */
        try {
            String savedJson = sharedPreferences.getString(KEY_INGREDIENTS_JSON, null);
            if (savedJson == null) return;

            JSONArray dishesArray = new JSONArray(savedJson);
            if (dishesArray.length() == 0) return;

            JSONObject lastDish = dishesArray.getJSONObject(dishesArray.length() - 1);
            String dishName = lastDish.getString("dish");
            JSONArray ingredientsArray = lastDish.getJSONArray("ingredients");

            List<String> ingredients = new ArrayList<>();
            for (int i = 0; i < ingredientsArray.length(); i++) {
                ingredients.add(ingredientsArray.getString(i));
            }

            DishResponse dishResponse = new DishResponse();
            Field dishField = DishResponse.class.getDeclaredField("dish");
            Field ingredientsField = DishResponse.class.getDeclaredField("ingredients");
            dishField.setAccessible(true);
            ingredientsField.setAccessible(true);
            dishField.set(dishResponse, dishName);
            ingredientsField.set(dishResponse, ingredients);

            displayIngredients(dishResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAllDishes() { /* keep your loadAllDishes code */
        try {
            String combinedData = sharedPreferences.getString("combined_dishes", "[]");
            Gson gson = new Gson();
            Type listType = new TypeToken<List<DishResponse>>(){}.getType();
            List<DishResponse> dishes = gson.fromJson(combinedData, listType);

            if (dishes == null || dishes.isEmpty()) return;

            ingredientsContainer.removeAllViews();
            for (DishResponse dishResponse : dishes) displayIngredients(dishResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Tag display & click behavior (modified to search + show markers) ---
    private void displayIngredients(DishResponse dishResponse) {
        List<String> ingredients = dishResponse.getIngredients();
        for (String ingredient : ingredients) {
            addIngredientTagToFlexbox(ingredient);
        }
    }


    // --- Add manual tag (unchanged) ---
    private void addIngredientTag(String ingredient) {
        LinearLayout tagLayout = new LinearLayout(this);
        tagLayout.setOrientation(LinearLayout.HORIZONTAL);
        tagLayout.setPadding(8, 8, 8, 8);
        tagLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);

        Button tag = new Button(this);
        tag.setText(ingredient);
        tag.setAllCaps(false);
        tag.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        tag.setTextColor(android.graphics.Color.BLACK);

        // Clicking manual tag also searches
        tag.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Searching stores for: " + ingredient, Toast.LENGTH_SHORT).show();
            getUserLocationAndSearch(ingredient);
        });

        Button removeBtn = new Button(this);
        removeBtn.setText("×");
        removeBtn.setAllCaps(false);
        removeBtn.setTextColor(android.graphics.Color.RED);
        removeBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        removeBtn.setOnClickListener(v -> {
            ingredientsContainer.removeView(tagLayout);

            String manualData = sharedPreferences.getString("manual_ingredients", "[]");
            if (manualData.contains(ingredient)) {
                removeManualIngredientFromPrefs(ingredient);
            } else {
                removeIngredientFromPrefs(ingredient);
            }


        });

        tagLayout.addView(tag);
        tagLayout.addView(removeBtn);
        ingredientsContainer.addView(tagLayout);
    }

    // --- Store search flow (get location -> search -> show markers) ---
    @SuppressLint("MissingPermission")
    private void getUserLocationAndSearch(String query) {
        // If user tapped map earlier, use that location immediately
        if (userTappedMap) {
            searchProducts(query, currentLat, currentLng);
            return;
        }

        // Otherwise try to get last known/current GPS location
        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(location -> {
            double lat = currentLat;
            double lng = currentLng;
            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
                currentLat = lat;
                currentLng = lng;
            }
            searchProducts(query, lat, lng);
        }).addOnFailureListener(e -> {
            // fallback to default Manila coords
            searchProducts(query, currentLat, currentLng);
        });
    }

    private void searchProducts(String query, double lat, double lng) {
        String token = "Bearer " + sharedPreferences.getString("token", "");
        apiService.fastSearch(token, query, lat, lng).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<StoreFast> stores = response.body().stores;
                    if (stores == null || stores.isEmpty()) {
                        Toast.makeText(GroupActivity.this, "No stores found for: " + query, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showResultsOnMap(stores);
                } else {
                    Toast.makeText(GroupActivity.this, "Search failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Show results on embedded map (adds markers) ---
    private void showResultsOnMap(List<StoreFast> stores) {
        // Remove old "Store" markers only (keep user marker)
        map.getOverlays().removeIf(o -> o instanceof Marker && "Store".equals(((Marker)o).getTitle()));

        // Ensure user marker present
        addUserMarker(currentLat, currentLng);

        for (StoreFast store : stores) {
            if (store == null) continue;
            // validate coords
            if (store.latitude == 0 && store.longitude == 0) continue;

            GeoPoint storePoint = new GeoPoint(store.latitude, store.longitude);
            Marker storeMarker = new Marker(map);
            storeMarker.setPosition(storePoint);
            storeMarker.setTitle("Store"); // used for removal
            storeMarker.setSnippet(store.store_name != null ? store.store_name : "Store");

            // optional custom icon (if exists)
            try {
                storeMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_store_marker));
            } catch (Exception ignored) {}

            // when clicked, show the modal with products (if your StoreFast has matched_products)
            storeMarker.setOnMarkerClickListener((marker, mapView) -> {
                showStoreProductsDialog(store);
                return true;
            });

            map.getOverlays().add(storeMarker);
        }

        map.getController().setCenter(new GeoPoint(currentLat, currentLng));
        map.invalidate();
        showStoreTable(stores);
    }

    // --- User marker helper ---
    private void addUserMarker(double lat, double lng) {
        // remove previous 'You are here' marker(s)
        map.getOverlays().removeIf(o -> o instanceof Marker && "You are here".equals(((Marker)o).getTitle()));

        GeoPoint userPoint = new GeoPoint(lat, lng);
        Marker userMarker = new Marker(map);
        userMarker.setPosition(userPoint);
        userMarker.setTitle("You are here");

        try {
            userMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_user_marker));
        } catch (Exception ignored) {}

        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(userMarker);
    }

    // --- Dialog showing products inside a store (copied/adapted from FastActivity) ---
    private void showStoreProductsDialog(StoreFast store) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_store_products);
        dialog.setCancelable(true);

        TextView tvStoreName = dialog.findViewById(R.id.tvStoreName);
        LinearLayout llProductsContainer = dialog.findViewById(R.id.llProductsContainer);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        tvStoreName.setText(store.store_name != null ? store.store_name : "Store");
        llProductsContainer.removeAllViews();

        if (store.matched_products != null) {
            for (ProductFast product : store.matched_products) {
                View productView = getLayoutInflater().inflate(R.layout.item_store_product, null);
                TextView tvProductName = productView.findViewById(R.id.tvProductName);
                TextView tvProductPrice = productView.findViewById(R.id.tvProductPrice);
                TextView tvProductDesc = productView.findViewById(R.id.tvProductDesc);
                Button btnAddToCart = productView.findViewById(R.id.btnAddToCart);
                ImageView imgProduct = productView.findViewById(R.id.imgProduct);

                tvProductName.setText(product.product_name);
                tvProductPrice.setText("₱" + product.price);
                tvProductDesc.setText(product.description);

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
                                Toast.makeText(GroupActivity.this, "Added to cart!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(GroupActivity.this, "Failed to add: " + response.message(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Toast.makeText(GroupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });

                llProductsContainer.addView(productView);
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // --- prefs helpers (unchanged) ---
    private void removeIngredientFromPrefs(String ingredientToRemove) {
        try {
            String combinedData = sharedPreferences.getString("combined_dishes", "[]");
            JSONArray dishesArray = new JSONArray(combinedData);

            for (int i = 0; i < dishesArray.length(); i++) {
                JSONObject dishObj = dishesArray.getJSONObject(i);
                JSONArray ingredients = dishObj.getJSONArray("ingredients");

                JSONArray newIngredients = new JSONArray();
                for (int j = 0; j < ingredients.length(); j++) {
                    String ing = ingredients.getString(j);
                    if (!ing.equalsIgnoreCase(ingredientToRemove)) {
                        newIngredients.put(ing);
                    }
                }

                dishObj.put("ingredients", newIngredients);
            }

            sharedPreferences.edit()
                    .putString("combined_dishes", dishesArray.toString())
                    .apply();

            Log.d("GroupActivity", "✅ Ingredient removed: " + ingredientToRemove);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveIngredientToPrefs(String ingredient) {
        try {
            String existingData = sharedPreferences.getString("manual_ingredients", "[]");
            JSONArray ingredientsArray = new JSONArray(existingData);

            for (int i = 0; i < ingredientsArray.length(); i++) {
                if (ingredientsArray.getString(i).equalsIgnoreCase(ingredient)) return;
            }

            ingredientsArray.put(ingredient);
            sharedPreferences.edit().putString("manual_ingredients", ingredientsArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void removeManualIngredientFromPrefs(String ingredient) {
        try {
            String existingData = sharedPreferences.getString("manual_ingredients", "[]");
            JSONArray ingredientsArray = new JSONArray(existingData);
            JSONArray updatedArray = new JSONArray();

            for (int i = 0; i < ingredientsArray.length(); i++) {
                if (!ingredientsArray.getString(i).equalsIgnoreCase(ingredient)) {
                    updatedArray.put(ingredientsArray.getString(i));
                }
            }

            sharedPreferences.edit().putString("manual_ingredients", updatedArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadManualIngredients() {
        try {
            String existingData = sharedPreferences.getString("manual_ingredients", "[]");
            JSONArray ingredientsArray = new JSONArray(existingData);
            for (int i = 0; i < ingredientsArray.length(); i++) {
                addIngredientTag(ingredientsArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void showStoreTable(List<StoreFast> stores) {
        if (stores == null) return;

        // Flatten store + products
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
                            store // pass the store object
                    ));
                }
            }
        }

        // Sort by price ascending
        tableRows.sort((a, b) -> Double.compare(a.price, b.price));

        // Adapter
        if (storeTableAdapter == null) {
            storeTableAdapter = new com.example.talipaapp.adapters.StoreTableAdapter(tableRows);
            rvStoreTable.setAdapter(storeTableAdapter);

            // Row click: zoom map + show dialog
            storeTableAdapter.setOnRowClickListener(row -> {
                GeoPoint point = new GeoPoint(row.latitude, row.longitude);
                map.getController().setZoom(15.0);
                map.getController().setCenter(point);

                // Highlight marker
                for (int i = 0; i < map.getOverlays().size(); i++) {
                    if (map.getOverlays().get(i) instanceof Marker) {
                        Marker marker = (Marker) map.getOverlays().get(i);
                        if (marker.getPosition().getLatitude() == row.latitude
                                && marker.getPosition().getLongitude() == row.longitude) {
                            marker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_store_marker));
                            break;
                        }
                    }
                }

                // Show modal dialog
                showStoreProductsDialog(row.store);
            });

        } else {
            storeTableAdapter.setData(tableRows);
        }
    }
    private void addIngredientTagToFlexbox(String ingredient) {
        Button tag = new Button(this);
        tag.setText(ingredient);
        tag.setAllCaps(false);
        tag.setBackgroundColor(Color.TRANSPARENT);
        tag.setTextColor(Color.BLACK);
        tag.setPadding(8, 8, 8, 8);
        tag.setTextSize(14);
        tag.setMaxLines(1); // optional, keep single line

        tag.setOnClickListener(v -> getUserLocationAndSearch(ingredient));

        Button removeBtn = new Button(this);
        removeBtn.setText("×");
        removeBtn.setAllCaps(false);
        removeBtn.setTextColor(Color.RED);
        removeBtn.setBackgroundColor(Color.TRANSPARENT);
        removeBtn.setOnClickListener(v -> {
            ingredientsContainer.removeView((View) v.getParent());
            removeIngredientFromPrefs(ingredient);

        });

        LinearLayout tagLayout = new LinearLayout(this);
        tagLayout.setOrientation(LinearLayout.HORIZONTAL);
        tagLayout.setPadding(4, 4, 4, 4);
        tagLayout.setGravity(Gravity.CENTER);
        tagLayout.addView(tag);
        tagLayout.addView(removeBtn);

        // Force 1/3 width
        com.google.android.flexbox.FlexboxLayout.LayoutParams params =
                new com.google.android.flexbox.FlexboxLayout.LayoutParams(
                        0, // width 0 because weight will handle it
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.setMargins(4, 4, 4, 4);
        params.setFlexGrow(1);       // expand to fill available space
        params.setFlexBasisPercent(0.33f); // 1/3 of parent
        tagLayout.setLayoutParams(params);

        ingredientsContainer.addView(tagLayout);
    }

    private void setupImagePicker() {
        btnRecognizeImage = findViewById(R.id.btnRecognizeImage);

        // --- Launchers for both cases ---
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            progressBar.setVisibility(View.VISIBLE);
                            uploadImageForRecognition(selectedImageUri);
                        }
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && cameraImageUri != null) {
                        progressBar.setVisibility(View.VISIBLE);
                        uploadImageForRecognition(cameraImageUri);
                    }
                }
        );

        btnRecognizeImage.setOnClickListener(v -> showImageSourceDialog());
    }

    private void showImageSourceDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_image_source);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnCameraOption = dialog.findViewById(R.id.btnCameraOption);
        Button btnGalleryOption = dialog.findViewById(R.id.btnGalleryOption);
        Button btnCancel = dialog.findViewById(R.id.btnCancelOption);

        btnCameraOption.setOnClickListener(v -> {
            dialog.dismiss();
            openCameraIntent();
        });

        btnGalleryOption.setOnClickListener(v -> {
            dialog.dismiss();
            openGalleryIntent();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void openCameraIntent() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            return;
        }

        try {
            File photoFile = File.createTempFile(
                    "captured_image", ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            );
            cameraImageUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", photoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void uploadImageForRecognition(Uri imageUri) {
        try {
            byte[] bytes = compressImage(imageUri);


            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), bytes);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image", "image.jpg", requestFile);


            String token = "Bearer " + sharedPreferences.getString("token", "");

            if (token == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
                return;
            }


            // 🔹 Create Retrofit client and call API
            ApiService uploadService = RetrofitClient.getInstance().create(ApiService.class);
            Call<JsonObject> call = uploadService.describeImageDish(token, body);

            Toast.makeText(this, "Recognizing image...", Toast.LENGTH_SHORT).show();

            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    progressBar.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject obj = new JSONObject(response.body().toString());
                            String recognized = obj.getString("object");

                            inputDish.setText(recognized);
                            btnSearch.performClick();

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(GroupActivity.this, "Parse error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(GroupActivity.this, "Recognition failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Toast.makeText(GroupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to open image", Toast.LENGTH_SHORT).show();
        }
    }



    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = File.createTempFile(
                        "captured_image", ".jpg",
                        getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                );
                photoUri = FileProvider.getUriForFile(
                        this, getPackageName() + ".provider", photoFile
                );
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(intent, CAMERA_REQUEST);
            } catch (IOException e) {
                Toast.makeText(this, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_REQUEST && data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) uploadImageForRecognition(imageUri);
            } else if (requestCode == CAMERA_REQUEST && photoUri != null) {
                uploadImageForRecognition(photoUri);
            }
        }
    }
    private byte[] compressImage(Uri uri) throws IOException {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos); // reduce to 70% quality
        return baos.toByteArray();
    }
    private void startRecording() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        2001);
                return;
            }

            File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            audioFile = new File(outputDir, "recorded_audio.m4a");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();
            mediaRecorder.start();

            isRecording = true;
            btnVoice.setText("⏹️ Stop Recording");
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Recording failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecordingAndSend() {
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;

            isRecording = false;
            btnVoice.setText("🎤 Record Voice");

            Toast.makeText(this, "Recording stopped, sending...", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            uploadAudioFile(audioFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Stop failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void uploadAudioFile(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            Toast.makeText(this, "Audio file missing", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = "Bearer " + sharedPreferences.getString("token", "");

        RequestBody requestFile = RequestBody.create(MediaType.parse("audio/m4a"), audioFile);
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData("audio", audioFile.getName(), requestFile);

        ApiService uploadService = RetrofitClient.getInstance().create(ApiService.class);
        Call<JsonObject> call = uploadService.describeUploadedAudio_droid(token, audioPart);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject obj = new JSONObject(response.body().toString());
                        JSONObject resultObj = obj.optJSONObject("result");
                        if (resultObj != null) {
                            JSONArray choices = resultObj.optJSONArray("choices");
                            if (choices != null && choices.length() > 0) {
                                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                                if (message != null) {
                                    String content = message.optString("content");
                                    if (content != null && content.contains("dish")) {
                                        JSONObject dishJson = new JSONObject(content);
                                        String dish = dishJson.optString("dish");
                                        inputDish.setText(dish);
                                        btnSearch.performClick();
                                        Toast.makeText(GroupActivity.this, "Recognized dish: " + dish, Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                }
                            }
                        }
                        Toast.makeText(GroupActivity.this, "Recognition complete (no dish found)", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(GroupActivity.this, "Parsing error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(GroupActivity.this, "Recognition failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(GroupActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

}
