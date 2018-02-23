package com.example.ben.currencyconvertor;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import common.Receiver;

// CurrencyReceiver class used to allow results to be passed back from an IntentService
final class CurrencyReciever extends ResultReceiver implements Receiver {

    // Receiver data member
    private Receiver reciever;

    // Setter for the reciever data member
    void setReciever(Receiver reciever) {
        this.reciever = reciever;
    }

    // Constructor
    CurrencyReciever(Handler handler){
        super(handler);
    }

    // Calls the recievers onReceiveResult when this is called
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        if (this.reciever != null) {
            this.reciever.onReceiveResult(resultCode, resultData);
        }
    }
}
