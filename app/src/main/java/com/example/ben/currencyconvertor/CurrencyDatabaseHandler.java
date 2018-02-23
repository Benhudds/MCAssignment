package com.example.ben.currencyconvertor;

import android.content.Context;
import android.database.Cursor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import common.DatabaseHandler;
import common.DateFormatter;

// CurrencyDatabaseHandler class
// Handles the interactions between the currency classes used within the code and the DatabaseHandler
final class CurrencyDatabaseHandler {

    // DatabaseHandler object
    private DatabaseHandler database;

    // Context reference used to get column ids
    private Context context;

    // Constructor
    CurrencyDatabaseHandler(Context context, String databasePath, String databaseName) {
        this.database = DatabaseHandler.getInstance(
                context,
                databasePath,
                databaseName);
        this.context = context;
    }

    // Method to load all the currency rates from the database for the given currency
    private List<CurrencyRate> getCurrencyRates(String currency) {
        List<CurrencyRate> retList = new ArrayList<>();

        // Replace the formatting character sequence in the string with the currnecy
        String query = String.format(context.getString(R.string.query_rate_selector), currency);

        Cursor cursor = this.database.query(query);
        cursor.moveToFirst();

        // Move the cursor through the internal result set to get each currency
        while (!cursor.isAfterLast()) {
            retList.add(new CurrencyRate(
                    cursor.getString(context.getResources().getInteger(R.integer.field_to_code)),
                    BigDecimal.valueOf(
                            cursor.getDouble(context.getResources().getInteger(R.integer.field_rate)))));

            cursor.moveToNext();
        }

        return retList;
    }

    // Method to save a single currency to the database
    private void saveCurrency(Currency currency) {
        // Format the query
        String query = String.format(context.getString(R.string.query_insert_currency),
                currency.getName(),
                DateFormatter.getString(currency.getLastUpdate()),
                currency.getFavIndex());
        this.database.exexQuery(query);
    }

    // Method to save a list of currency rates for a given currency to the database
    private void saveCurrencyRates(String fromCurrency, List<CurrencyRate> rates) {
        for (CurrencyRate r : rates) {
            this.saveCurrencyRate(fromCurrency, r);
        }
    }

    // Method to save a single currency rate for the given currency to the database
    private void saveCurrencyRate(String fromCurrency, CurrencyRate rate) {
        // Format the query
        String query = String.format(context.getString(R.string.query_insert_rate),
                fromCurrency,
                rate.getCurrencyName(),
                rate.getConversionRate());
        this.database.exexQuery(query);
    }

    // Method to load all the currencies from the database
    ArrayList<Currency> getCurrencies() {
        String query = context.getString(R.string.query_currency_selector);

        Cursor cursor = this.database.query(query);
        cursor.moveToFirst();

        ArrayList<Currency> retList = new ArrayList<>();

        // Move the cursor through the internal result set to get each currency
        while (!cursor.isAfterLast()) {

            // Need a valid date and this could returnnull if not formatted correctly so checking date first
            Date date = DateFormatter.getDate(
                    cursor.getString(context.getResources().getInteger(R.integer.field_last_updated)));

            if (date != null) {
                Currency newCurrency = new Currency(
                        cursor.getString(context.getResources().getInteger(R.integer.field_currency)),
                        date,
                        cursor.getInt(context.getResources().getInteger(R.integer.field_fav_index)));

                // Get the rates for this currency
                newCurrency.setConversionRates(this.getCurrencyRates(newCurrency.getName()));
                retList.add(newCurrency);
            }

            cursor.moveToNext();
        }

        return retList;
    }

    // Method to save a list of currencies to the database
    void saveCurrencies(List<Currency> currencies) {
        for (Currency c : currencies) {
            this.saveCurrency(c);
            this.saveCurrencyRates(c.getName(), c.getConversionRates());
        }
    }
}
