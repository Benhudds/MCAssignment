package com.example.ben.currencyconvertor;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import common.DateFormatter;
import common.Receiver;

import static com.example.ben.currencyconvertor.R.id.mainCurrencyValue;

// Main Class
public class Main extends AppCompatActivity implements Receiver {

    // Constant final values used as indexes
    private int mainCurrencyIndex;
    private int deselected;
    private int nonFavIndex;

    // Main currency object used to calculate conversion values and determine which rates to use
    private Currency mainCurrency = null;

    // Boolean used to determind if converted values or rates should be displayed
    private boolean displayValues = false;

    // Main currency EditText used when the onTextChanged method is called for it
    private EditText mainCurrencyText;

    // List of all the currencies
    private ArrayList<Currency> currencies;

    // CurrencyAdapter derived from ArrayAdapter used to display currencies in a currency_view format
    private CurrencyAdapter currencyAdapter;

    // CurrencyReciever object for passing currency objects back to this thread, the UI thread
    public CurrencyReciever reciever;

    // Getter for the main currency data member
    public Currency getMainCurrency() {
        return this.mainCurrency;
    }

    // Setter for the main currency data member
    private void setMainCurrency(Currency newCurrency) {
        // Set the favourite index of the old currency to 0
        if (this.mainCurrency != null) {
            this.mainCurrency.setFavIndex(0);
        }

        // Set the main currency to the new one
        this.mainCurrency = newCurrency;

        // Set the favourite index of the new main currency to -1
        this.mainCurrency.setFavIndex(this.mainCurrencyIndex);

        // Set the resources so that the flag and text update
        this.setMainResources();

        // Set the currency adapter main currency so that the user can display rates
        this.currencyAdapter.setMainCurrency(this.mainCurrency);
    }

    // Override onCreate method
    // Starting point for the application
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise the values used from the resources
        mainCurrencyIndex = getResources().getInteger(R.integer.main_currency);
        deselected = getResources().getInteger(R.integer.deselected);
        nonFavIndex = getResources().getInteger(R.integer.non_favourite_currency);

        // Initialise the currencies list
        currencies = new ArrayList<>();

        // Create the receiver to handle results returned from the CurrencyIntentService service
        this.reciever = new CurrencyReciever(new Handler());
        this.reciever.setReciever(this);

        // Initialise the currencyAdapter so that currencies will now be populated on screen
        currencyAdapter = new CurrencyAdapter(this, currencies);

        // Set the onItemClickListener for the listView
        ListView listView = (ListView) this.findViewById(R.id.listView);
        listView.setOnItemClickListener(listViewClickListener);
        listView.setAdapter(currencyAdapter);

        // Start IntentService to retrieve currencies from the database
        Intent intent = new Intent(this, CurrencyIntentService.class);
        intent.setAction(this.getString(R.string.action_retrieve_from_db));
        intent.putExtra(getString(R.string.param_receiver), this.reciever);
        startService(intent);

        // Get the EditText containing the user input value for the main currency
        mainCurrencyText = (EditText) findViewById(mainCurrencyValue);

        // Point the text changed event listener to the TextWatcher
        mainCurrencyText.addTextChangedListener(mainCurrencyWatcher);
    }

    // Method to remove the reciever for the IntentService as the link won't remain when the activity is destroyed
    // Saves the currencies to the database
    private void onPauseOrStop() {
        this.saveCurrency(this.mainCurrency);
        this.saveCurrencies(this.currencies);
        this.reciever.setReciever(null);
    }

    // Method to add a currency to the currencies list or replace the one already there
    private void addOrReplace(Currency currency) {
        // Return if the currency is null for whatever reason
        if (currency == null) {
            return;
        }

        // First case, adding the main currency from the database (FAV_INDEX == -1)
        if (currency.getFavIndex() == mainCurrencyIndex) {
            this.setMainCurrency(currency);
            this.updateCurrencies();
            return;
        }

        // Second case, mainCurrency is being updated and needs to be replaced
        if (this.mainCurrency != null && this.mainCurrency.getName().equals(currency.getName())) {
            currency.setFavIndex(mainCurrencyIndex);
            this.setMainCurrency(currency);

            // As main has been updated, it should now have a list of conversionRates
            // Get new currencies from the API using these rates
            this.getNewCurrenciesFromRates(currency.getConversionRates());
            return;
        }

        // If this is not the main currency we need to update the current value in case the user has
        // changed the main currency value before data callback made
        if (this.mainCurrency != null) {
            currency.setCurrentValue(mainCurrency.getCurrentValue().multiply(
                    mainCurrency.getConversionRate(currency.getName())));
        }

        // Third case, the currency already exists in the list of currencies and needs to be replaced
        int index = this.getIndexOfCurrency(currency.getName());
        if (index > mainCurrencyIndex) {
            currency.setFavIndex(this.currencies.get(index).getFavIndex());
            this.currencies.remove(index);
            this.currencies.add(currency);
            this.sortAndDisplayCurrencies();
            return;
        }

        // Fourth case, the currency needs to be added to the list of currencies
        this.currencies.add(currency);
    }

    // Method to get new currency objects through the API using the rates
    // Performs network operations on the IntentService, not this thread
    private void getNewCurrenciesFromRates(List<CurrencyRate> rates) {
        for (CurrencyRate cr : rates) {
            if (!currencyExists(cr.getCurrencyName())) {
                updateCurrency(cr.getCurrencyName());
            }
        }
    }

    // Method to check if the currency with a given name exists in the list of currencies
    private boolean currencyExists(String currencyName) {
        for (Currency c : this.currencies) {
            if (c.getName().equals(currencyName)) {
                return true;
            }
        }

        return false;
    }

    // Method to get the index of the currency in the list
    // Returns -1 if not found
    private int getIndexOfCurrency(String currencyName) {
        for (int i = 0; i < this.currencies.size(); i++) {
            if (this.currencies.get(i).getName().equals(currencyName)) {
                return i;
            }
        }

        return -1;
    }

    // Method to sort the currencies and notify a data change in the adapter to refresh the view
    private void sortAndDisplayCurrencies() {
        // Using Collection.sort() as Currency is Comparable
        Collections.sort(this.currencies);

        // Refresh the listView contents through the currencyAdapter
        this.currencyAdapter.notifyDataSetChanged();
    }

    // Method to queue work on the IntentService to update the given currency
    private void updateCurrency(String currencyName) {
        // Use todays date to get the latest information
        Date date = new Date();
        String url = "http://api.fixer.io/" + DateFormatter.getString(date) + "?base=" + currencyName;

        Intent intent = new Intent(this, CurrencyIntentService.class);
        intent.setAction(this.getString(R.string.action_retrieve_from_url));
        intent.putExtra(getString(R.string.param_url), url);
        intent.putExtra(getString(R.string.param_receiver), this.reciever);
        startService(intent);
    }

    // Method to update the currencies from the API using the IntentService
    private void updateCurrencies() {
        if (this.mainCurrency == null) {
            return;
        }

        // Update the main currency
        this.updateCurrency(this.mainCurrency.getName());

        // Update the other currencies
        for (Currency c : this.currencies) {
            this.updateCurrency(c.getName());
        }
    }

    // Method to save the currencies to the database
    private void saveCurrencies(ArrayList<Currency> c) {
        Intent intent = new Intent(this, CurrencyIntentService.class);
        intent.setAction(this.getString(R.string.action_save_to_db));
        intent.putParcelableArrayListExtra(
                this.getString(R.string.parcel_currency_list),
                c);
        startService(intent);
    }

    // Method to save a single currency to the database
    private void saveCurrency(Currency currency) {
        ArrayList<Currency> temp = new ArrayList<>(1);
        temp.add(currency);
        this.saveCurrencies(temp);
    }

    // Method to set the resources of the main currency
    private void setMainResources() {
        // Set the flag resource
        ImageView img = (ImageView) findViewById(R.id.mainCurrencyFlag);
        img.setImageResource(getMainCurrency().getFlagId(this));

        // Set the text resource
        TextView txt = (TextView) findViewById(R.id.mainCurrencyLabel);
        txt.setText(getMainCurrency().getName());

        // Reset the edit text value
        mainCurrencyText.setText(this.getString(R.string.edit_text_default_value));
    }

    // Method to show the button panel
    private void showButtonPanel() {
        // Use the linearWrapper around the listView as a parent so that
        // the main currency section is not altered
        ViewGroup parent = (ViewGroup) findViewById(R.id.linearWrapper);

        // Inflate the button panel
        ViewGroup buttonPanel = (ViewGroup) getLayoutInflater().inflate(R.layout.button_panel, parent, false);

        // Get the listView and add some padding for aesthetics
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setPadding(0, 0, 0, getResources().getDimensionPixelOffset(R.dimen.listView_bottom_margin));

        // Set parameters of the button panel and add it to the linearWrapper parent
        buttonPanel.setLayoutParams(new LinearLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT));
        parent.addView(buttonPanel);

        // Set the onClickListeners for the buttons
        Button setMain = (Button) buttonPanel.findViewById(R.id.setMainCurrency);
        setMain.setOnClickListener(setMainListener);
        Button setFav = (Button) buttonPanel.findViewById(R.id.setNewFavourite);
        setFav.setOnClickListener(setFavListener);
    }

    // Method to hide the button panel
    private void hideButtonPanel() {
        // Get the buttonPanel ViewGroup and its parent
        ViewGroup buttonPanel = (ViewGroup) findViewById(R.id.buttonPanel);
        ViewGroup parent = (ViewGroup) findViewById(R.id.linearWrapper);

        // Remove the padding from the listView
        ListView lv = (ListView) findViewById(R.id.listView);
        lv.setPadding(0, 0, 0, 0);

        // Remove the buttonPanel from the parent
        parent.removeView(buttonPanel);
    }

    // Method called when an item in the listView is clicked
    private void onListViewItemClick(int position) {
        // If an item is not currently selected then show the buttonPanel
        if (currencyAdapter.getSelectedPosition() == deselected) {
            showButtonPanel();
        }

        // If the item is currently selected then hide the button panel and deselect it
        // Otherwise, set the new selected item and the text of the favourite button if necessary
        if (currencyAdapter.getSelectedPosition() == position) {
            currencyAdapter.setSelectedPosition(deselected);
            hideButtonPanel();
        } else {
            currencyAdapter.setSelectedPosition(position);
            if (currencies.get(currencyAdapter.getSelectedPosition()).getFavIndex() != nonFavIndex) {
                Button setFav = (Button) findViewById(R.id.setNewFavourite);
                setFav.setText(getResources().getText(R.string.button_unsetFav));
            }
        }

        // Refresh the listView
        currencyAdapter.notifyDataSetChanged();
    }

    // Method to update the values of the currencies
    private void updateCurrentValues() {
        for (Currency curr : currencies) {
            curr.setCurrentValue(getMainCurrency().getCurrentValue().multiply(
                    getMainCurrency().getConversionRate(curr.getName())));
        }

        currencyAdapter.notifyDataSetChanged();
    }

    // ListView onItemClickListener
    private final AdapterView.OnItemClickListener listViewClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // Remove the soft keyboard if the user clicks on a item in the ListView.
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.
                    INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

            // Clear the focus from the main currency EditText
            mainCurrencyText.clearFocus();

            // Process the click of this individual item
            onListViewItemClick(position);
        }
    };

    // TextWatcher for the mainCurrency EditText
    private final TextWatcher mainCurrencyWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        // Read the text as it changes
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            try {
                // Read the text into a BigDecimal
                BigDecimal value = new BigDecimal(mainCurrencyText.getText().toString());

                // Set the current value then update the other values in the list
                getMainCurrency().setCurrentValue(value);
                updateCurrentValues();
            } catch (Exception ex) {
                Log.e(getResources().getString(R.string.app_name), ex.getMessage());
            }
        }

        public void afterTextChanged(Editable s) {
        }
    };

    // SetFavourite button click listener
    private final View.OnClickListener setFavListener = new View.OnClickListener() {
        public void onClick(View view) {
            // Change the favIndex of the currency to place it at the top of the list
            // If it is already a favourite, remove it as a favourite
            Currency currency = currencies.get(currencyAdapter.getSelectedPosition());
            if (currency.getFavIndex() == nonFavIndex) {
                currency.setFavIndex(currencies.get(0).getFavIndex() + 1);
            } else {
                currency.setFavIndex(nonFavIndex);
            }

            // Reset the selected position of the currency adapter as we are deselecting the item
            currencyAdapter.setSelectedPosition(deselected);

            // Hide the button panel
            hideButtonPanel();

            // Refresh the listView
            sortAndDisplayCurrencies();
        }
    };

    // SetMain button click listener
    private final View.OnClickListener setMainListener = new View.OnClickListener() {
        public void onClick(View view) {
            // Swap the two currencies
            int index = currencyAdapter.getSelectedPosition();
            Currency currency = currencies.get(index);

            currencies.remove(index);
            currencies.add(mainCurrency);

            setMainCurrency(currency);

            // Deselect the item
            currencyAdapter.setSelectedPosition(deselected);

            // Hide the button panel
            hideButtonPanel();

            // Refresh the listView
            sortAndDisplayCurrencies();
        }
    };

    // Method called when the display rates/values button is pressed
    public void onDisplayChange(View view) {
        Button btn = (Button) this.findViewById(R.id.swapDisplay);

        // Set the text
        if (this.displayValues) {
            btn.setText(this.getString(R.string.button_displayRates));
        } else {
            btn.setText(this.getString(R.string.button_displayValues));
        }

        // Set the currencyAdapter value
        this.currencyAdapter.setDisplayValues(this.displayValues);

        // Invert the displayValues boolean
        this.displayValues = !this.displayValues;

        // Refresh the listView using the currnecyAdapter
        this.currencyAdapter.notifyDataSetChanged();
    }

    // Overriden onReceiveResult method
    // Takes a Bundle of resultData from the IntentService and pulls out Currency object(s)
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        // Check if a single currency or list of currencies are being received
        // Add them to the currencies list
        if (resultData.containsKey(this.getString(R.string.parcel_currency))) {
            this.addOrReplace((Currency) resultData.getParcelable(
                    this.getString(R.string.parcel_currency)));

        } else if (resultData.containsKey(this.getString(R.string.parcel_currency_list))) {
            // Get the list of currencies and add them individually
            // Return immediately if the list is null
            List<Currency> newCurrencies = resultData.getParcelableArrayList(
                    this.getString(R.string.parcel_currency_list));

            if (newCurrencies == null) {
                return;
            }

            // Add all the currencies to the list
            for (Currency c : newCurrencies) {
                this.addOrReplace(c);
            }
        }

        // Finally, sort the displayed currencies
        this.sortAndDisplayCurrencies();
    }

    // Overriden onTouchEvent method
    // Removes the soft keyboard and clears the focus from the
    // main currency EditText when the user clicks outside of the EditText
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Hide the soft keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.
                INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        // Clear the focus
        mainCurrencyText.clearFocus();
        return true;
    }

    // Overriden onResume method
    @Override
    protected void onResume() {
        // Set the reciever for the IntentService
        this.reciever.setReciever(this);
        super.onResume();

        // Update all the currencies
        this.updateCurrencies();
    }

    // Overriden onPause method
    @Override
    protected void onPause() {

        this.onPauseOrStop();
        super.onPause();
    }

    // Overriden onStop method
    @Override
    protected void onStop() {
        this.onPauseOrStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.onPauseOrStop();
        super.onDestroy();
    }
}
