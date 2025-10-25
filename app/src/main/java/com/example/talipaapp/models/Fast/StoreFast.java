package com.example.talipaapp.models.Fast;

import java.util.List;

public class StoreFast {
    public int id;
    public String store_name;
    public String store_address;
    public double latitude;
    public double longitude;
    public double distance;


    public List<ProductFast> matched_products;
}
