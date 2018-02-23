package common;

import android.os.Bundle;


// Receiver interface used to allow communication between the UI thread and IntentService
public interface Receiver {
    void onReceiveResult(int resultCode, Bundle resultData);
}
