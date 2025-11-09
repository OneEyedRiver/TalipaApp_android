package com.example.talipaapp.models;

public class Product {

    private int id;
    private int seller_id;

    private String product_name;
    private String product_category;

    private double product_qty;
    private String product_unit;

    private String product_freshness;
    private double product_price;

    private String harvest_date;
    private String product_image;

    private int deliver_availability;   // 0/1 from API
    private int pick_up_availability;   // 0/1 from API
    private int is_available;           // 0/1 from API

    private String product_description;

    // ✅ Store coordinates from API
    private double storeLatitude;
    private double storeLongitude;

    // --- Getters ---
    public int getId() { return id; }
    public int getSeller_id() { return seller_id; }

    public String getProduct_name() { return product_name; }
    public String getProduct_category() { return product_category; }

    public double getProduct_qty() { return product_qty; }
    public String getProduct_unit() { return product_unit; }

    public String getProduct_freshness() { return product_freshness; }
    public double getProduct_price() { return product_price; }

    public String getHarvest_date() { return harvest_date; }
    public String getProduct_image() { return product_image; }

    public boolean isDeliver_availability() { return deliver_availability == 1; }
    public boolean isPick_up_availability() { return pick_up_availability == 1; }
    public boolean isIs_available() { return is_available == 1; }

    private int store_id;

    public void setAvailable(boolean available) {
        this.is_available = available ? 1 : 0;
    }
    public String getProduct_description() { return product_description; }

    // --- Coordinates ---
    public double getStoreLatitude() { return storeLatitude; }
    public double getStoreLongitude() { return storeLongitude; }

    public void setStoreLatitude(double lat) { this.storeLatitude = lat; }
    public void setStoreLongitude(double lng) { this.storeLongitude = lng; }
    public int getStore_id() { return store_id; }
    public void setStore_id(int store_id) { this.store_id = store_id; }
    }
