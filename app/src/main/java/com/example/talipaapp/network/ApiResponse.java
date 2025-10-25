package com.example.talipaapp.network;

import com.example.talipaapp.models.CartItem;
import com.example.talipaapp.models.Category;
import com.example.talipaapp.models.Product;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ApiResponse {

    // Common response fields
    @SerializedName("Status")
    private String Status;

    @SerializedName("message")
    private String message;

    @SerializedName("success")
    private boolean success;

    // For cart-related responses
    @SerializedName("quantity")
    private int quantity;

    @SerializedName("total_price")
    private double total_price;


    @SerializedName("product_price")
    private double product_price;  // Optional if your Laravel returns it

    @SerializedName("cart")
    private List<CartItem> cart;

    // For product/category lists
    @SerializedName("products")
    private List<Product> products;

    @SerializedName("productSingle")
    private Product productSingle;

    @SerializedName("product")
    private Product product;

    @SerializedName("categories")
    private List<Category> categories;

    // ✅ Add coordinates
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    // --- Getters ---
    public String getStatus() { return Status; }

    public String getMessage() { return message; }

    public boolean isSuccess() { return success; }

    public int getQuantity() { return quantity; }

    public double getTotal_price() { return total_price; }

    public double getProduct_price() { return product_price; }

    public List<CartItem> getCart() { return cart; }

    public List<Product> getProducts() { return products; }

    public Product getProductSingle() { return productSingle; }

    public Product getProduct() { return product; }

    public List<Category> getCategories() { return categories; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
