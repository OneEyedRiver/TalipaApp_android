package com.example.talipaapp.cart;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.talipaapp.models.CartItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPrefManager {
    private static final String PREF_NAME = "TalipaAppCart";
    private static final String KEY_CART = "user_cart";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Gson gson;

    public SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        gson = new Gson();
    }

    // Save cart list
    public void saveCart(List<CartItem> cartList) {
        String json = gson.toJson(cartList);
        editor.putString(KEY_CART, json);
        editor.apply();
    }

    // Load cart list
    public List<CartItem> getCart() {
        String json = prefs.getString(KEY_CART, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // Clear cart data
    public void clearCart() {
        editor.remove(KEY_CART);
        editor.apply();
    }
}
