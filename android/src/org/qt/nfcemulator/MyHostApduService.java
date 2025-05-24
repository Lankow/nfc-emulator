package org.qtproject.example.nfcemulator;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

public class MyHostApduService extends HostApduService {

    private static final String TAG = "MyHostApduService";

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        Log.d(TAG, "Received APDU: " + Arrays.toString(apdu));

        // Example: respond with OK
        return new byte[]{(byte) 0x90, 0x00};
    }

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated: reason = " + reason);
    }
}