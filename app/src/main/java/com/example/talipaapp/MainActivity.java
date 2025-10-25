package com.example.talipaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.talipaapp.adapters.ProductAdapter;
import com.example.talipaapp.models.Product;
import com.example.talipaapp.network.ApiResponse;
import com.example.talipaapp.network.RetrofitClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    Button btn_logout, btn_menu;

    TextView txtToken;

    RecyclerView recyclerView;
    ProductAdapter adapter;
    List<Product> productList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        btn_logout=findViewById(R.id.btn_logout);
        btn_menu=findViewById(R.id.btn_menu);
        sharedPreferences=
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );


        editor= sharedPreferences.edit();

        if(sharedPreferences.getString("loginStatus", "false")
                .equals("false")){
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();

        }
        txtToken=findViewById(R.id.txt_token);
        getData();


        btn_logout.setOnClickListener(view -> {
            logout();
        });

        btn_menu.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), MenuActivity.class));
        });





    }



    void getData(){





        ApiService apiService= RetrofitClient.getInstance().create(ApiService.class);



        Call<ResponseBody> call=apiService.postList("Bearer " + sharedPreferences.getString("token", ""));
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {


                if(response.isSuccessful() && response.body() != null){
                    //startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    try {
                        String res = response.body().string();
                        txtToken.setText(res);






                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }


                } else{
                    Toast.makeText(getApplicationContext(), "Error please try again", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {


                Toast.makeText(getApplicationContext(), "Error:" + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();


            }
        });



    }

    void logout(){
        editor.clear();
        editor.apply();
        Intent intent= new Intent(getApplicationContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }





}