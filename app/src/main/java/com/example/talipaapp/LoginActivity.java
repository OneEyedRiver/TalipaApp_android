package com.example.talipaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.talipaapp.network.RetrofitClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    EditText edt_email, edt_password;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        progressBar= findViewById(R.id.progressBar);

        sharedPreferences=
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );


        editor= sharedPreferences.edit();





        edt_email= findViewById(R.id.edt_email);

        edt_password= findViewById(R.id.edt_password);

        Button btn_login= findViewById(R.id.btn_login);
        Button btn_register= findViewById(R.id.btn_register);


        btn_login.setOnClickListener(view -> {
            loginUser();
        });

        btn_register.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
            finish();
        });


        if(sharedPreferences.getString("loginStatus", "false")
                .equals("true")){
            startActivity(new Intent(getApplicationContext(), MenuActivity.class));
            finish();

        }




    }

   void loginUser(){

       String  email,  password;

       email=edt_email.getText().toString();
       password=edt_password.getText().toString();

       progressBar.setVisibility(View.VISIBLE);





       ApiService apiService= RetrofitClient.getInstance().create(ApiService.class);

       User user = new User(email,password);

       Call<ResponseBody> call=apiService.postLogin(user);
       call.enqueue(new Callback<ResponseBody>() {
           @Override
           public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
               progressBar.setVisibility(View.GONE);
                   //startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                   try {
                       String res;
                       if (response.isSuccessful() && response.body() != null) {
                           res = response.body().string();
                       } else if (response.errorBody() != null) {
                           res = response.errorBody().string();
                       } else {
                           Toast.makeText(getApplicationContext(), "Unknown error", Toast.LENGTH_SHORT).show();
                           return;
                       }
                    JSONObject jsonObject = new JSONObject(res);

                    String status=jsonObject.getString("Status");
                    String message1=jsonObject.getString("message");

                    switch (status) {
                        case "success":
                            JSONObject jsonObjectData = jsonObject.getJSONObject("data");
                            JSONObject jsonObjectUser = jsonObjectData.getJSONObject("user");
                            int user_id = jsonObjectUser.getInt("id");

                            String tokenAuth = jsonObjectData.getString("token");

                            String f_name = jsonObjectUser.getString("f_name");
                            String l_name = jsonObjectUser.getString("l_name");
                            String username = jsonObjectUser.getString("username");
                            String phone_number = jsonObjectUser.getString("phone_number");
                            String email = jsonObjectUser.getString("email");

                            SharedPreferences sharedPreferences =
                                    getApplication().getSharedPreferences(
                                            "com.example.talipaapp", MODE_PRIVATE
                                    );

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("token", tokenAuth);
                            editor.putString("f_name", f_name);
                            editor.putString("l_name", l_name);
                            editor.putString("username", username);
                            editor.putString("phone_number", phone_number);
                            editor.putString("email", email);
                            editor.putString("loginStatus", "true");
                            editor.putInt("user_id", user_id);

                            editor.apply();
                            startActivity(new Intent(getApplicationContext(), MenuActivity.class));
                            finish();
                            break;

                        case "error":

                            Toast.makeText(getApplicationContext(), "Error: " + message1, Toast.LENGTH_SHORT).show();
                            break;


                    }




                   } catch (IOException | JSONException e) {
                       throw new RuntimeException(e);
                   }


               }


           @Override
           public void onFailure(Call<ResponseBody> call, Throwable t) {

               progressBar.setVisibility(View.GONE);
               Toast.makeText(getApplicationContext(), "Error:" + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();


           }
       });



   }
}