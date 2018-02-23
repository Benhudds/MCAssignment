package com.example.ben.currencyconvertor;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigDecimal;

// CurrencyRate class that contains a target currency and conversion rate
// Source currency is derived through reference
// Implements Parcelable to allow passing to and from IntentService
final class CurrencyRate implements Parcelable {

    // Currency name data member
    private String currencyName;

    // Conversion Rate data member
    // BigDecimal to simplify rounding
    private BigDecimal conversionRate;

    // Getter for the currencyName data member
    String getCurrencyName() { return this.currencyName; }

    // Getter for the conversionRate data member
    BigDecimal getConversionRate() { return this.conversionRate; }

    // Constructor
    CurrencyRate(String currency, BigDecimal conversionRate)
    {
        this.currencyName = currency;
        this.conversionRate = conversionRate;
    }

    // Private Parcel constructor
    private CurrencyRate(Parcel in) {
        this.currencyName = in.readString();
        this.conversionRate = BigDecimal.valueOf(in.readDouble());
    }

    // Parcel writer
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.currencyName);
        out.writeDouble(this.conversionRate.doubleValue());
    }

    // Parcelable descriptor
    @Override
    public int describeContents() {
        return 0;
    }

    // Static Parcel creator
    public static final Parcelable.Creator<CurrencyRate> CREATOR
            = new Parcelable.Creator<CurrencyRate>() {
        @Override
        public CurrencyRate createFromParcel(Parcel in) {
            return new CurrencyRate(in);
        }

        @Override
        public CurrencyRate[] newArray(int size) {
            return new CurrencyRate[size];
        }
    };
}
