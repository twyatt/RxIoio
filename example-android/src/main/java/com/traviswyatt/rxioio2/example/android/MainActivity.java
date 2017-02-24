package com.traviswyatt.rxioio2.example.android;

import android.os.Bundle;

import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MainActivity extends IOIOActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new IOIOLooper() {
            @Override
            public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {

            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                Thread.sleep(1000L);
            }

            @Override
            public void disconnected() {

            }

            @Override
            public void incompatible() {

            }

            @Override
            public void incompatible(IOIO ioio) {

            }
        };
    }

}
