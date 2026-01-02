package com.example.myapplication;

import androidx.annotation.DrawableRes;

public class Product {
    private final String name;
    private final String price;
    private final @DrawableRes int imageResId;

    public Product(String name, String price, int imageResId) {
        this.name = name;
        this.price = price;
        this.imageResId = imageResId;
    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public int getImageResId() {
        return imageResId;
    }
}

