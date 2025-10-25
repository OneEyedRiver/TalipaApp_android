package com.example.talipaapp.store;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.talipaapp.ApiService;
import com.example.talipaapp.MenuActivity;
import com.example.talipaapp.R;
import com.example.talipaapp.map.MapActivity;
import com.example.talipaapp.models.Store;
import com.example.talipaapp.network.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import retrofit2.Call;
import retrofit2.Retrofit;

public class SetUpActivity extends AppCompatActivity {
    private static final int MAP_REQUEST_CODE = 100;
    private EditText  edtStoreName, edtPhone,edtEmail, edtAddress, edtPostalCode, edtCity, edtState, edtLatitude, edtLongitude;
    SharedPreferences sharedPreferences;

    Button btnSubmit, btnReset, btnBack;


    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_up);
        sharedPreferences =
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );




        edtStoreName=findViewById(R.id.edtStoreName);
        edtPhone=findViewById(R.id.edtPhone);
        edtEmail=findViewById(R.id.edtEmail);
        edtAddress=findViewById(R.id.edtAddress);
        edtPostalCode=findViewById(R.id.edtPostalCode);
        edtCity=findViewById(R.id.edtCity);
        edtState=findViewById(R.id.edtState);
        edtLatitude = findViewById(R.id.edtLatitude);
        edtLongitude = findViewById(R.id.edtLongitude);
        btnSubmit=findViewById(R.id.btnSubmit);
        btnReset=findViewById(R.id.btnReset);
        btnBack=findViewById(R.id.btnBack);

        Button btnPickLocation = findViewById(R.id.btnPickLocation);
        btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(SetUpActivity.this, MapActivity.class);
            startActivityForResult(intent, MAP_REQUEST_CODE);
        });


        btnSubmit.setOnClickListener(v -> submitStore());

        btnReset.setOnClickListener(view -> {
            edtStoreName.setText("");
            edtPhone.setText("");
            edtEmail.setText("");
            edtAddress.setText("");
            edtPostalCode.setText("");
            edtCity.setText("");
            edtState.setText("");
            edtLatitude.setText("");
            edtLongitude.setText("");
        });

        btnBack.setOnClickListener(view -> {
            startActivity(new Intent(SetUpActivity.this, MenuActivity.class));
            finish();
        });

        progressBar= findViewById(R.id.progressBar);


    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAP_REQUEST_CODE && resultCode == RESULT_OK) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lon = data.getDoubleExtra("longitude", 0);
            edtLatitude.setText(String.valueOf(lat));
            edtLongitude.setText(String.valueOf(lon));
        }
    }




    private void submitStore() {
        progressBar.setVisibility(View.VISIBLE);
        String token = "Bearer " + sharedPreferences.getString("token", "");

        Store store = new Store(
                edtStoreName.getText().toString(),
                edtPhone.getText().toString(),
                edtEmail.getText().toString(),
                edtAddress.getText().toString(),
                edtPostalCode.getText().toString(),
                edtCity.getText().toString(),
                edtState.getText().toString(),
                Double.parseDouble(edtLatitude.getText().toString()),
                Double.parseDouble(edtLongitude.getText().toString())
        );

        ApiService api = RetrofitClient.getInstance().create(ApiService.class);

        Call<JsonObject> call = api.saveStore(token, store);
        Gson gson = new Gson();
        Log.d("API_REQUEST", gson.toJson(store));

        call.enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);

                try {
                    JSONObject jsonObject;

                    if (response.isSuccessful() && response.body() != null) {
                        // ✅ Success response
                        jsonObject = new JSONObject(response.body().toString());
                    } else if (response.errorBody() != null) {
                        // ✅ Validation / error response
                        String errorString = response.errorBody().string();
                        jsonObject = new JSONObject(errorString);
                    } else {
                        Toast.makeText(SetUpActivity.this,
                                "Unknown error occurred",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Handle JSON response
                    String status = jsonObject.has("status")
                            ? jsonObject.optString("status", "")
                            : jsonObject.optString("Status", "");

                    switch (status) {
                        case "error":
                            StringBuilder errorBuilder = new StringBuilder();

                            if (jsonObject.has("errors")) {
                                JSONObject jsonErrors = jsonObject.getJSONObject("errors");
                                Iterator<String> keys = jsonErrors.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    errorBuilder.append(jsonErrors.getJSONArray(key).getString(0)).append("\n");
                                }
                            }

                            if (jsonObject.has("message")) {
                                errorBuilder.append(jsonObject.optString("message"));
                            }

                            Toast.makeText(SetUpActivity.this,
                                    errorBuilder.toString().trim(),
                                    Toast.LENGTH_LONG).show();
                            break;

                        case "success":
                            Toast.makeText(SetUpActivity.this,
                                    jsonObject.optString("message", "Store created successfully!"),
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SetUpActivity.this, SellActivity.class));
                            finish();
                            break;

                        default:
                            Toast.makeText(SetUpActivity.this,
                                    "Unexpected response: " + jsonObject.toString(),
                                    Toast.LENGTH_LONG).show();
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(SetUpActivity.this,
                            "Parsing error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SetUpActivity.this, "Failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}