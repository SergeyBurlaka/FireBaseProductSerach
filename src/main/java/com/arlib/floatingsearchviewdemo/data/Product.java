package com.arlib.floatingsearchviewdemo.data;

import android.os.Parcel;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.Map;

/**
 * Created by Operator on 25.01.2017.
 */

public class Product implements SearchSuggestion, Comparable<Product> {

    private Map<String, Object> keyName;
    private String productName;
    private String productKey;



    private boolean mIsHistory = false;

    public Product(String suggestion, String productKey) {
        this.productKey = productKey;
        this.productName = suggestion.toLowerCase();
    }

    public Product(Parcel source) {
        this.productName = source.readString();
        this.mIsHistory = source.readInt() != 0;
    }


    public Product(){
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };


    public Map<String, Object> getKeyName() {
        return keyName;
    }

    public void setKeyName(Map<String, Object> keyName) {
        this.keyName = keyName;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    @Override
    public String getBody() {
        return productName;
    }

    public boolean ismIsHistory() {
        return mIsHistory;
    }

    public void setmIsHistory(boolean mIsHistory) {
        this.mIsHistory = mIsHistory;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int i) {
        dest.writeString(productName);
        dest.writeInt(mIsHistory ? 1 : 0);
    }

    @Override
    public int compareTo(Product product) {
        return productName.compareTo(product.productName);
    }

    public String getProductKey() {
        return productKey;
    }

    public void setProductKey(String productKey) {
        this.productKey = productKey;
    }
}
