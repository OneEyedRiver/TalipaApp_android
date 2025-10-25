package com.example.talipaapp;

import android.content.Intent;
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

public class RegisterActivity extends AppCompatActivity {
    EditText edt_firstName, edt_lastName, edt_userName, edt_email,edt_phone, edt_password, edt_passwordConfirmation;
    ProgressBar progressBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        progressBar = findViewById(R.id.progressBar);


        edt_firstName= findViewById(R.id.edt_fname);
        edt_lastName= findViewById(R.id.edt_lname);
        edt_userName= findViewById(R.id.edt_uname);
        edt_email= findViewById(R.id.edt_email);
        edt_phone=findViewById(R.id.edt_phone);
        edt_password= findViewById(R.id.edt_password);
        edt_passwordConfirmation= findViewById(R.id.edt_passwordConfirmation);
        Button btn_register= findViewById(R.id.btn_register);
        Button btn_back= findViewById(R.id.btn_back);


        btn_register.setOnClickListener(v -> {
                    regUser();
        });

        btn_back.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });



    }

    void regUser(){
        progressBar.setVisibility(View.VISIBLE);
        String f_name, l_name, username, email, phone_number, password, password_confirmation;
        f_name=edt_firstName.getText().toString();
        l_name=edt_lastName.getText().toString();
        username=edt_userName.getText().toString();
        email=edt_email.getText().toString();
        phone_number=edt_phone.getText().toString();

        password=edt_password.getText().toString();
        password_confirmation=edt_passwordConfirmation.getText().toString();



        ApiService apiService= RetrofitClient.getInstance().create(ApiService.class);



       User user = new User(f_name, l_name, username, email, phone_number,password,password_confirmation );
        Call<ResponseBody> call=apiService.postRegister(user);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if(response.isSuccessful() && response.body() != null){
                    try {
                        String res = response.body().string();

                        JSONObject jsonObject = new JSONObject(res);
                        String statuz = jsonObject.getString("Status");

                        switch (statuz) {
                            case "error":
                                // "message" might be object or string
                                Object messageObj = jsonObject.get("message");
                                StringBuilder errorBuilder = new StringBuilder();

                                if (messageObj instanceof JSONObject) {
                                    JSONObject jsonErrors = (JSONObject) messageObj;

                                    if (jsonErrors.has("username")) {
                                        String usernameError = jsonErrors.getJSONArray("username").getString(0);
                                        errorBuilder.append(usernameError).append("\n");
                                    }
                                    if (jsonErrors.has("email")) {
                                        String phoneError = jsonErrors.getJSONArray("email").getString(0);
                                        errorBuilder.append(phoneError).append("\n");
                                    }

                                    if (jsonErrors.has("phone_number")) {
                                        String phoneError = jsonErrors.getJSONArray("phone_number").getString(0);
                                        errorBuilder.append(phoneError).append("\n");
                                    }
                                    if (jsonErrors.has("password")) {
                                        String phoneError = jsonErrors.getJSONArray("password").getString(0);
                                        errorBuilder.append(phoneError).append("\n");
                                    }

                                } else {
                                    // Plain string
                                    errorBuilder.append(messageObj.toString());
                                }

                                Toast.makeText(getApplicationContext(), errorBuilder.toString().trim(), Toast.LENGTH_SHORT).show();
                                break;

                            case "Success":
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                Toast.makeText(getApplicationContext(), "Account Created!", Toast.LENGTH_SHORT).show();
                                finish();
                                break;
                        }

                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Parsing error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }



                }
                else{
                    Toast.makeText(getApplicationContext(), "Error please try again", Toast.LENGTH_SHORT).show();
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