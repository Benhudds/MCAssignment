package com.example.ben.currencyconvertor;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Currency class
// Internal model of a Currency
// Implements Comparable to allow sorting
// Implements Parcelable to allow passing to and from the IntentService;
final class Currency implements Comparable, Parcelable {

    // The name of the currency
    private String name;

    // The index the currency is going to be stored at
    // -1 = main currency
    // 0 = non-favourite currency
    // non-zero = favourite index of the currency
    private int favIndex;

    // BigDecimal value storing the value currently displayed on screen
    // BigDecimal used over double to simplify rounding
    private BigDecimal currentValue;

    // Date at which the currnecy was last updated
    // Prevents updating the currenecy multiple times per day
    private Date lastUpdate;

    // A list of currency conversion rates that are used to calculate values
    private List<CurrencyRate> conversionRates;

    // Getter for the favourite index data member
    int getFavIndex() {
        return this.favIndex;
    }

    // Setter for the favourite index data member
    void setFavIndex(int index) {
        this.favIndex = index;
    }

    // Getter for the current value data member
    BigDecimal getCurrentValue() {
        return this.currentValue;
    }

    // Setter for the current value data member
    // Rounds to two decimal places
    void setCurrentValue(BigDecimal value) {
        this.currentValue = value.setScale(2, RoundingMode.HALF_UP);
    }

    // Getter for the last update date data member
    Date getLastUpdate() {
        return this.lastUpdate;
    }

    // Getter for the conversion rates data member
    List<CurrencyRate> getConversionRates() {
        return this.conversionRates;
    }

    // Setter for the conversion rates data member
    void setConversionRates(List<CurrencyRate> rates) {
        this.conversionRates = rates;
    }

    // Getter for a single conversion rate with a given currency name
    BigDecimal getConversionRate(String currencyName) {
        for (CurrencyRate pair : conversionRates) {
            if (pair.getCurrencyName().equals(currencyName)) {
                return pair.getConversionRate();
            }
        }

        return BigDecimal.ZERO;
    }

    // Getter for a resource identifier of a flag drawable
    int getFlagId(Context context) {
        return context.getResources().getIdentifier("flag_" +
                this.name.toLowerCase(), "drawable", context.getPackageName());
    }

    // Public getter for the name data member
    public String getName() {
        return this.name;
    }

    // Public setter for the name data member
    public void setName(String name) {
        this.name = name;
    }

    // Constructor taking name and date parameters
    // Date is stripped of time values
    // FavIndex is set to 0 by default
    Currency(String name, Date date) {
        this.setName(name);
        this.favIndex = 0;
        this.setCurrentValue(BigDecimal.ZERO);

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        this.lastUpdate = date;
    }

    // Constructor taking name, data and favIndex parameters
    // Date is not stripped of time values as this will
    // only be called when retrieved from the database
    Currency(String name, Date date, int favIndex) {
        this.setName(name);
        this.lastUpdate = date;
        this.favIndex = favIndex;
        this.setCurrentValue(BigDecimal.ZERO);
    }

    // Private Parcel constructor
    private Currency(Parcel in) {
        this.name = in.readString();
        this.favIndex = in.readInt();
        this.lastUpdate = new Date(in.readLong());
        this.conversionRates = in.readArrayList(CurrencyRate.class.getClassLoader());
    }

    // Public comparator function
    // Orders based on favourite index or name if they are identical
    @Override
    public int compareTo(@NonNull Object comp) {
        Currency compCurrency = (Currency) comp;

        if (this.favIndex > compCurrency.favIndex) {
            return -1;
        } else if (this.favIndex < compCurrency.favIndex) {
            return 1;
        } else {
            return this.name.compareTo(compCurrency.getName());
        }
    }

    // Parcel writer
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.name);
        out.writeInt(this.favIndex);
        out.writeLong(this.lastUpdate.getTime());
        out.writeArray(this.conversionRates.toArray());
    }

    // Parcelable descriptor
    @Override
    public int describeContents() {
        return 0;
    }

    // Static Parcel creator
    public static final Parcelable.Creator<Currency> CREATOR
            = new Parcelable.Creator<Currency>() {
        @Override
        public Currency createFromParcel(Parcel in) {
            return new Currency(in);
        }

        @Override
        public Currency[] newArray(int size) {
            return new Currency[size];
        }
    };
}
