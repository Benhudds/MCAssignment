package com.example.ben.currencyconvertor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// CurrencyAdapter class inherited from ArrayAdapter
// Converts Currency objects into custom views for displaying in ListViews
final class CurrencyAdapter extends ArrayAdapter<Currency> {

    // The currently selected index in the ListView
    // Required to display or remove the button panel and set backgrounds based on a currencies favIndex
    // -1 if an item is not selected
    private int selectedPosition = -1;

    // Boolean values that determines if the rates or the values of the currencies should be shown
    private boolean displayValues = true;

    // Main currency used to find the rates when they are to be shown
    private Currency mainCurrency;

    // Getter for the selectedPosition data member
    int getSelectedPosition() {
        return selectedPosition;
    }

    // Setter for the selectedPosition data member
    void setSelectedPosition(int value) {
        this.selectedPosition = value;
    }

    // Setter for the displayValues data member
    void setDisplayValues(boolean value) {
        this.displayValues = value;
    }

    // Setter for the mainCurrency data member
    void setMainCurrency(Currency currency) {
        this.mainCurrency = currency;
    }

    // Constructor
    // Calls parent constructor
    CurrencyAdapter(Context context, ArrayList<Currency> currencies) {
        super(context, 0, currencies);
    }

    // Method to set the currency data items
    private void setCurrencyValues(View convertView, Currency currency) {
        ImageView flagView = (ImageView) convertView.findViewById(R.id.currencyFlag);
        flagView.setImageResource(currency.getFlagId(getContext()));
        flagView.setScaleType(ImageView.ScaleType.FIT_XY);

        TextView currencyText = (TextView) convertView.findViewById(R.id.currencyLabel);
        currencyText.setText(currency.getName());

        TextView currencyValue = (TextView) convertView.findViewById(R.id.currencyValue);

        if (this.displayValues) {
            currencyValue.setText(String.valueOf(currency.getCurrentValue()));
        } else {
            currencyValue.setText(String.valueOf(this.mainCurrency.getConversionRate(currency.getName())));
        }
    }

    // Overriden getView method
    // Gets the currency associated with the position then inflates the currencyview xml
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Currency currency = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.currency_view, parent, false);
        }

        this.setCurrencyValues(convertView, currency);

        if (position == this.selectedPosition) {
            convertView.setBackgroundResource(R.drawable.pressed_background);
        } else if (currency != null && currency.getFavIndex() !=
                getContext().getResources().getInteger(R.integer.non_favourite_currency)) {
            convertView.setBackgroundResource(R.drawable.fav_background);
        } else {
            convertView.setBackgroundResource(R.drawable.default_background);
        }

        return convertView;
    }
}
