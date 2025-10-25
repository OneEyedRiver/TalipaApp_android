package com.example.talipaapp;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("f_name")
    private String f_name;

    @SerializedName("l_name")
    private String l_name;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("phone_number")
    private String phone_number;

    @SerializedName("password")
    private String password;

    @SerializedName("password_confirmation")
    private String password_confirmation;

    // Constructors
     User(String f_name, String l_name, String username, String email,
                String phone_number, String password, String password_confirmation) {
        this.f_name = f_name;
        this.l_name = l_name;
        this.username = username;
        this.email = email;
        this.phone_number = phone_number;
        this.password = password;
        this.password_confirmation = password_confirmation;
    }

     User(String email, String password) {
        this.email = email;
        this.password = password;
    }



    public String getFname(){
        return f_name;
    }
    public String getLname(){
        return l_name;
    }
    public String getUname(){
        return username;
    }
    public String getEmail(){
        return email;
    }
    public String getPhone(){
        return phone_number;
    }
    public String getPassword(){
        return password;
    }
    public String getPassword_confirmation(){
        return password_confirmation;
    }



}
