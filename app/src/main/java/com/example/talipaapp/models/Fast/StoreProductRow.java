package com.example.talipaapp.models.Fast;

public class StoreProductRow {
    public String storeName;
    public String storeAddress; // optional if you have
    public String productName;
    public double price;
    public double latitude;   // add
    public double longitude;  // add

    public StoreFast store;
    public StoreProductRow(String storeName, String storeAddress, String productName, double price, double latitude, double longitude, StoreFast store) {
        this.storeName = storeName;
        this.storeAddress = storeAddress;
        this.productName = productName;
        this.price = price;
        this.latitude = latitude;
        this.longitude = longitude;
        this.store = store;
    }
}
