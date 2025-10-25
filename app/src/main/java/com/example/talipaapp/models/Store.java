package com.example.talipaapp.models;

import com.google.gson.annotations.SerializedName;

public class Store {

    @SerializedName("store_name")
    private String store_name;

    @SerializedName("store_phoneNumber")
    private String store_phoneNumber;

    @SerializedName("store_email")
    private String store_email;

    @SerializedName("store_address")
    private String store_address;

    @SerializedName("store_postalCode")
    private String store_postalCode;

    @SerializedName("store_city")
    private String store_city;

    @SerializedName("store_state")
    private String store_state;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;


    public Store() {
    }

    public Store(String store_name, String store_phoneNumber,String store_email,
                 String store_address, String store_postalCode, String store_city,
                 String store_state,  double latitude, double longitude) {
        this.store_name = store_name;
        this.store_phoneNumber = store_phoneNumber;
        this.store_email = store_email;
        this.store_address = store_address;


        this.store_postalCode = store_postalCode;
        this.store_city = store_city;
        this.store_state = store_state;

        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getStore_name() { return store_name; }
    public void setStore_name(String store_name) { this.store_name = store_name; }

    public String getStore_phoneNumber() { return store_phoneNumber; }
    public void setStore_phoneNumber(String store_phoneNumber) { this.store_phoneNumber = store_phoneNumber; }

    public String getStore_email() { return store_email; }
    public void setStore_email(String store_email) { this.store_email = store_email; }

    public String getStore_address() { return store_address; }
    public void setStore_address(String store_address) { this.store_address = store_address; }



    public String getStore_postalCode() { return store_postalCode; }
    public void setStore_postalCode(String store_postalCode) { this.store_postalCode = store_postalCode; }

    public String getStore_city() { return store_city; }
    public void setStore_city(String store_city) { this.store_city = store_city; }

    public String getStore_state() { return store_state; }


    public void setStore_state(String store_state) { this.store_state = store_state; }



    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
