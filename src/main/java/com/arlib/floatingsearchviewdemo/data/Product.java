package com.arlib.floatingsearchviewdemo.data;

import android.os.Parcel;
import android.util.Log;

import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by Operator on 25.01.2017.
 */

public class Product implements SearchSuggestion, Comparable<Product> {

    private Map<String, Object> keyWordsList;
    private String productName;
    private String productKey;

    private Boolean confirmed = true;



    private boolean mIsHistory = false;
    public static String PRODUCT_SEARCH_TAG = "ProductSearch";

    public Product(String suggestion, String productKey, Map<String, Object> keyWordsList) {
        this.productKey = productKey;
        this.productName = suggestion.toLowerCase();
        this.keyWordsList = keyWordsList;
    }

    public Product(String suggestion) {

        this.productName = suggestion.toLowerCase();

    }

    public Product(Parcel source) {
        this.productName = source.readString();
        this.mIsHistory = source.readInt() != 0;
    }


    public Product(){
    }


    public boolean isNameValid (String userKeyName){

        for (String productKeyName : keyWordsList.keySet())
        {
            /*Log.d(PRODUCT_SEARCH_TAG, "isNameValid: "
                    + " searchKeyName -> "+productKeyName
                    + " userKeyName -> "+ userKeyName
                    + " is contain -> " + Pattern.compile(Pattern.quote(userKeyName), Pattern.CASE_INSENSITIVE).matcher(productKeyName).find()
            );*/
            if(Pattern.compile(Pattern.quote(userKeyName), Pattern.CASE_INSENSITIVE).matcher(productKeyName).find()) return true;
        }

        return false;
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


    public Map<String, Object> getKeyWordsList() {
        return keyWordsList;
    }

    public void setKeyWordsList(Map<String, Object> keyWordsList) {
        this.keyWordsList = keyWordsList;
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

    public Boolean getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }
}
