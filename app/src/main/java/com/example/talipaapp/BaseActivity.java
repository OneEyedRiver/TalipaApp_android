package com.example.talipaapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;


import com.example.talipaapp.cart.CartActivity;
import com.example.talipaapp.fast.FastActivity;
import com.example.talipaapp.fast.GroupActivity;
import com.example.talipaapp.store.SellActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {

    protected TextView txtHeaderTitle;
    protected Button btnLogout;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    protected BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }

    /**
     * Sets the layout of the activity and initializes the shared header
     */
    protected void setBaseContentView(@LayoutRes int layoutResID) {
        setContentView(layoutResID);
        sharedPreferences=
                getApplication().getSharedPreferences(
                        "com.example.talipaapp", MODE_PRIVATE
                );
        editor= sharedPreferences.edit();
        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Example: set default title
        txtHeaderTitle.setText("My App");

        btnLogout.setOnClickListener(v -> {
            editor.clear();
            editor.apply();
            Intent intent= new Intent(getApplicationContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            SharedPreferences prefs = getSharedPreferences("DishPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();

        });
    }

    /**
     * Helper to change header title dynamically
     */
    protected void setHeaderTitle(String title) {
        if (txtHeaderTitle != null) {
            txtHeaderTitle.setText(title);
        }
    }


    protected void initBottomNavigation(int currentItemId) {
        if (bottomNavigation == null) return;

        // Highlight current item
        bottomNavigation.setSelectedItemId(currentItemId);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            // Navigate only if not the current activity
            if (id == R.id.nav_home && !(this instanceof MenuActivity)) {
                startActivity(new Intent(this, MenuActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

            }  else if (id == R.id.nav_search1 && !(this instanceof FastActivity)) {
                startActivity(new Intent(this, FastActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

            } else if (id == R.id.nav_search2 && !(this instanceof GroupActivity)) {
                startActivity(new Intent(this, GroupActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

            }

            else if (id == R.id.nav_cart && !(this instanceof CartActivity)) {
                startActivity(new Intent(this, CartActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                //  Toast.makeText(this, "Search2 clicked", Toast.LENGTH_SHORT).show();
            }

          else if (id == R.id.nav_store && !(this instanceof SellActivity)) {
                startActivity(new Intent(this, SellActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));

            }

            return true;
        });
    }




}
