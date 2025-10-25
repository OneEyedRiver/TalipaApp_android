package com.example.talipaapp.cart;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talipaapp.ApiService;
import com.example.talipaapp.BaseActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.models.CartItem;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.events.MapEventsReceiver;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.widget.Button;

public class CartActivity extends BaseActivity {
    private RecyclerView rvCart;
    private TextView tvTotalPrice;
    private SharedPrefManager prefManager;
    private SharedPreferences sharedPreferences;
    private ApiService apiService;
    private CartAdapter adapter;

    private MapView osmMap;
    private GeoPoint userPoint, storePoint;
    private Marker userMarker, storeMarker;
    private Polyline currentRoad;
    private boolean isManualLocation = false;

    private LocationManager locationManager;
    private MapEventsOverlay mapEventsOverlay;
    private Button btnResetLocation;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setBaseContentView(R.layout.activity_cart);
       initBottomNavigation(R.id.nav_cart);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            startLocationUpdates();
        }
        progressBar= findViewById(R.id.progressBar);
        // 🗺️ Initialize map
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        osmMap = findViewById(R.id.osmMap);
        osmMap.setTileSource(TileSourceFactory.MAPNIK);
        osmMap.setMultiTouchControls(true);
        btnResetLocation = findViewById(R.id.btnResetLocation);
        btnResetLocation.setOnClickListener(v -> resetToGpsLocation());

        rvCart = findViewById(R.id.rvCart);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        prefManager = new SharedPrefManager(this);
        sharedPreferences = getSharedPreferences("com.example.talipaapp", MODE_PRIVATE);
        apiService = RetrofitClient.getInstance().create(ApiService.class);

        rvCart.setLayoutManager(new LinearLayoutManager(this));
        loadCartItems();

        // Add persistent map click listener (only once)
        setupMapClickListener();
    }

    /** 🔹 Setup a single persistent tap listener for manual location **/
    private void setupMapClickListener() {
        MapEventsReceiver receiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                isManualLocation = true;
                userPoint = p;
                updateUserMarker(userPoint, false);
                Toast.makeText(CartActivity.this, "Manual location set!", Toast.LENGTH_SHORT).show();
                if (storePoint != null) drawRoute(userPoint, storePoint);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        mapEventsOverlay = new MapEventsOverlay(receiver);
        osmMap.getOverlays().add(mapEventsOverlay);
    }

    /** 🔹 Fetch cart from API **/
    private void loadCartItems() {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sharedPreferences.getString("token", "");

        apiService.getUserCart(token).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<CartItem> cartItems = response.body().getCart();
                    prefManager.saveCart(cartItems);
                    displayCart(cartItems);
                } else {
                    loadCachedCart();
                    Toast.makeText(CartActivity.this, "Failed to load cart", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                loadCachedCart();
                Toast.makeText(CartActivity.this, "Offline mode: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCachedCart() {
        List<CartItem> cachedCart = prefManager.getCart();
        if (cachedCart == null || cachedCart.isEmpty()) {
            tvTotalPrice.setText("No cart data available");
        } else {
            displayCart(cachedCart);
        }
    }

    /** 🔹 Display cart items + store markers **/
    private void displayCart(List<CartItem> cartItems) {
        adapter = new CartAdapter(this, cartItems);
        rvCart.setAdapter(adapter);

        double total = 0;
        for (CartItem item : cartItems) total += item.getTotal_price();
        tvTotalPrice.setText("Total: ₱" + total);

        // Clear store markers only (keep map click overlay)
        osmMap.getOverlays().removeIf(o ->
                (o instanceof Marker) &&
                        o != userMarker &&
                        o != storeMarker);


        IMapController controller = osmMap.getController();
        controller.setZoom(13.0);

        GeoPoint first = null;
        for (CartItem item : cartItems) {
            Log.d("CartActivity", "🛒 CartItem -> StoreID: " + item.getStore_id() + ", StoreName: " + item.getStore_name());

            if (item.getLatitude() != 0 && item.getLongitude() != 0) {
                Marker m = new Marker(osmMap);
                m.setPosition(new GeoPoint(item.getLatitude(), item.getLongitude()));
                m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                String storeName = item.getStore_name() != null ? item.getStore_name() : "Unknown Store";
                m.setTitle(storeName);
                m.setSubDescription(item.getProduct_name());
                m.setIcon(getResources().getDrawable(R.drawable.ic_store_marker));

                osmMap.getOverlays().add(m);
                if (first == null) first = m.getPosition();
            }
        }


        if (first != null) controller.setCenter(first);
        osmMap.invalidate();
    }

    /** 🔹 Called when a product is tapped in RecyclerView **/
    public void showStoreOnMap(CartItem item) {
        // ✅ Only clear store markers, not user
        osmMap.getOverlays().removeIf(o ->
                (o instanceof Marker) && o != userMarker);

        // ✅ Add mapEventsOverlay only once
        if (!osmMap.getOverlays().contains(mapEventsOverlay)) {
            osmMap.getOverlays().add(mapEventsOverlay);
        }

        double storeLat = item.getLatitude(), storeLon = item.getLongitude();
        if (storeLat == 0 || storeLon == 0) {
            Toast.makeText(this, "No store location", Toast.LENGTH_SHORT).show();
            return;
        }

        storePoint = new GeoPoint(storeLat, storeLon);
        storeMarker = new Marker(osmMap);
        storeMarker.setPosition(storePoint);
        storeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        String storeName = item.getStore_name() != null ? item.getStore_name() : "Unknown Store";
        storeMarker.setTitle(storeName);
        storeMarker.setSubDescription("Product: " + item.getProduct_name());
        storeMarker.setIcon(getResources().getDrawable(R.drawable.ic_store_marker));

        storeMarker.setIcon(getResources().getDrawable(R.drawable.ic_store_marker));
        osmMap.getOverlays().add(storeMarker);

        // Set user location
        Location loc = null;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!isManualLocation && loc != null) {
            userPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
        } else if (userPoint == null) {
            userPoint = new GeoPoint(14.5995, 120.9842); // Default Manila
        }

        updateUserMarker(userPoint, !isManualLocation);
        drawRoute(userPoint, storePoint);
    }

    /** 🔹 Draw route **/
    /** 🔹 Draw route **/
    private void drawRoute(GeoPoint user, GeoPoint store) {
        if (osmMap == null) return;

        // 🔸 Always remove previous route overlay if exists
        if (currentRoad != null) {
            osmMap.getOverlays().remove(currentRoad);
            currentRoad = null;
        }

        new Thread(() -> {
            try {
                RoadManager rm = new OSRMRoadManager(this, "TalipaApp/1.0");
                ArrayList<GeoPoint> wp = new ArrayList<>();
                wp.add(user);
                wp.add(store);
                Road road = rm.getRoad(wp);

                // 🧭 Build overlay once
                Polyline newRoadOverlay = RoadManager.buildRoadOverlay(road);
                newRoadOverlay.setWidth(8f); // optional: make it visible and distinct

                runOnUiThread(() -> {
                    // Remove again to ensure no duplicates
                    osmMap.getOverlays().remove(currentRoad);
                    currentRoad = newRoadOverlay;
                    osmMap.getOverlays().add(currentRoad);

                    osmMap.getController().setCenter(user);
                    osmMap.invalidate();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Failed to draw route: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    /** 🔹 GPS updates **/
    private void startLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    2,
                    location -> {
                        if (!isManualLocation) {
                            userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            updateUserMarker(userPoint, true);
                        }
                    }
            );
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /** 🔹 Create or update user marker **/
    private void updateUserMarker(GeoPoint point, boolean fromGps) {
        if (userMarker == null) {
            userMarker = new Marker(osmMap);
            userMarker.setTitle("You");
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            userMarker.setIcon(getResources().getDrawable(R.drawable.ic_user_marker));
            osmMap.getOverlays().add(userMarker);
        }

        userMarker.setPosition(point);
        osmMap.invalidate();

        if (storePoint != null) drawRoute(userPoint, storePoint);

        Log.d("CartActivity", (fromGps ? "🟢 GPS updated" : "🔵 Manual updated") + " " + point);
    }
    public void updateTotalPrice() {
        double total = 0;
        for (CartItem item : adapter.getCartList()) {
            total += item.getTotal_price();
        }
        tvTotalPrice.setText("Total: ₱" + total);
    }
    /** 🔹 Reset user location to current GPS **/
    private void resetToGpsLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "GPS permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (loc == null) {
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (loc != null) {
            userPoint = new GeoPoint(loc.getLatitude(), loc.getLongitude());
            isManualLocation = false;
            updateUserMarker(userPoint, true);
            Toast.makeText(this, "Reset to GPS location", Toast.LENGTH_SHORT).show();

            if (storePoint != null) {
                drawRoute(userPoint, storePoint);
            } else {
                osmMap.getController().setCenter(userPoint);
            }
        } else {
            Toast.makeText(this, "Unable to get GPS location. Please move to an open area.", Toast.LENGTH_SHORT).show();
            // Optional: start listening for updates until we get a location
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, location -> {
                userPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                isManualLocation = false;
                updateUserMarker(userPoint, true);
                locationManager.removeUpdates((LocationListener) this); // stop after first fix
            });
        }
    }


}
