package com.example.ben.currencyconvertor;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import common.DateFormatter;
import common.URLStringReader;

// CurrencyIntentService derived from IntentService
// Used to retrieve currency data from the local database or internet
// Used to save data to the database
final public class CurrencyIntentService extends IntentService {

    // Constructor
    public CurrencyIntentService() {
        // TODO
        super("CurrencyIntentService");
    }

    // Method to handler the intents passed to this service
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        // Execute code based on the action
        final String action = intent.getAction();
        if (action.equals(this.getString(R.string.action_retrieve_from_url))) {
            // Get data for a currency from the given URL
            final String url = intent.getStringExtra(getString(R.string.param_url));
            final ResultReceiver receiver = intent.getParcelableExtra(getString(R.string.param_receiver));
            handleActionRetrieveData(url, receiver);
        } else if (action.equals(this.getString(R.string.action_retrieve_from_db))) {
            // Get data from the database
            final ResultReceiver receiver = intent.getParcelableExtra(getString(R.string.param_receiver));
            handleActionLoadFromDatabase(receiver);
        } else if (action.equals(this.getString(R.string.action_save_to_db))) {
            // Save the currencies to the database
            final List<Currency> currencies = intent.getParcelableArrayListExtra(
                    this.getString(R.string.parcel_currency_list));
            handleActionSaveToDatabase(currencies);
        }
    }

    // Method to save the list of currencies to the database
    private void handleActionSaveToDatabase(List<Currency> currencies) {
        CurrencyDatabaseHandler handler = new CurrencyDatabaseHandler(
                this.getApplicationContext(),
                this.getString(R.string.database_path),
                this.getString(R.string.database_name));
        handler.saveCurrencies(currencies);
    }

    // Method to retrieve data from the given url, convert it into a currency
    // then return it to the result reciever
    private void handleActionRetrieveData(String url, ResultReceiver currencyReciever) {
        Bundle currencyBundle = new Bundle();
        currencyBundle.putParcelable(
                this.getString(R.string.parcel_currency),
                this.getCurrencyFromUrl(
                        this.getResources().getInteger(R.integer.data_retry_interval),
                        url));
        currencyReciever.send(0, currencyBundle);
    }

    // Method to load data from the database then return it to the result reciever
    private void handleActionLoadFromDatabase(ResultReceiver currencyReciever) {
        Bundle currencyBundle = new Bundle();
        CurrencyDatabaseHandler handler = new CurrencyDatabaseHandler(
                this.getApplicationContext(),
                this.getString(R.string.database_path),
                this.getString(R.string.database_name));

        currencyBundle.putParcelableArrayList(this.getString(R.string.parcel_currency_list),
                handler.getCurrencies());
        currencyReciever.send(0, currencyBundle);
    }

    // Method to get a currency from a supplied url
    public Currency getCurrencyFromUrl(int dataRetryInterval, String url) {
        String json = null;
        Currency currency = null;

        // Read the json and attempt to convert it to a Currency object
        // Repeat until successful sleeping a defined period between attempts
        while (json == null || currency == null) {
            try {
                json = URLStringReader.readUrl(url);
                currency = convertToCurrency(json);
            } catch (IOException | JSONException | NullPointerException ex) {
                Log.e(this.getResources().getString(R.string.app_name), ex.getMessage());
                try {
                    Thread.sleep(dataRetryInterval);
                } catch (InterruptedException ex2) {
                    Log.e(this.getResources().getString(R.string.app_name), ex2.getMessage());
                }
            }
        }

        return currency;
    }

    // Method to convert a JSON string to a Currency type object
    private static Currency convertToCurrency(String json) throws JSONException, NullPointerException {
        JSONObject jsonObj = new JSONObject(json);

        // Get the name and the date
        String currencyName = jsonObj.getString("base");
        Date currencyDate = DateFormatter.getDate(jsonObj.getString("date"));

        ArrayList<CurrencyRate> currencyRatePairs = new ArrayList<>();

        // Iterate throught the keys in the JSONObject
        // These are now all currency conversion rate pairs
        JSONObject rates = jsonObj.getJSONObject("rates");
        Iterator<String> keys = rates.keys();
        while (keys.hasNext()) {
            String name = keys.next();
            BigDecimal value = BigDecimal.valueOf(rates.getDouble(name));
            currencyRatePairs.add(new CurrencyRate(name, value));
        }

        Currency currency = new Currency(currencyName, currencyDate);
        currency.setConversionRates(currencyRatePairs);

        return currency;
    }


}
