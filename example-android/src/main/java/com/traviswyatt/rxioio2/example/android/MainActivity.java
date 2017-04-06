package com.traviswyatt.rxioio2.example.android;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.traviswyatt.rxioio2.RxIoio;
import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class MainActivity extends IOIOActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.status) TextView statusView;
    @BindView(R.id.stat_led) Button statLedView;
    @BindString(R.string.turn_stat_led_on) String turnStateLedOnText;

    private Observable<Boolean> statLedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Create Observable that emits true when user clicks to turn stat LED
        // on, or false when user clicks to turn stat LED off.
        Observable<Boolean> statLedToggle = RxView
                .clicks(statLedView)
                .map(new Function<Object, Boolean>() {
                    @Override
                    public Boolean apply(@NonNull Object o) throws Exception {
                        return turnStateLedOnText.equals(statLedView.getText());
                    }
                })
                .share();

        // Update stat LED button text based on last user requested state.
        statLedToggle
                .map(new Function<Boolean, Integer>() {
                    @Override
                    public Integer apply(@NonNull Boolean aBoolean) throws Exception {
                        return aBoolean ? R.string.turn_stat_led_off : R.string.turn_stat_led_on;
                    }
                })
                .subscribe(RxTextView.textRes(statLedView));

        // Create a Observable that inverts toggle state (stat LED: false = ON, true = OFF) and also
        // replays last seen state (in case of IOIO reconnect).
        statLedState = statLedToggle
                .map(new Function<Boolean, Boolean>() {
                    @Override
                    public Boolean apply(@NonNull Boolean aBoolean) throws Exception {
                        // Setting pin 0 to false turns stat LED on, so we
                        // invert our toggle state.
                        return !aBoolean;
                    }
                })
                .replay(1)
                .autoConnect();
    }

    private void setStatus(final String status) {
        Log.d(TAG, "setStatus() called with: status = [" + status + "]");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(status);
            }
        });
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        Log.d(TAG, "createIOIOLooper() called");

        return new IOIOLooper() {
            @Nullable private Disposable disposable;

            @Override
            public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
                setStatus("Connected");

                RxIoio rxIoio = RxIoio.create(ioio);
                disposable = statLedState
                        .lift(rxIoio.digitalOutput(0))
                        .subscribeWith(new DisposableObserver<Boolean>() {
                            @Override
                            public void onNext(Boolean aBoolean) {
                                Log.d(TAG, "onNext() called with: aBoolean = [" + aBoolean + "]");
                            }

                            @Override
                            public void onError(Throwable t) {
                                Log.d(TAG, "onError() called with: t = [" + t + "]");
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete() called");
                            }
                        });
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                Thread.sleep(1000L);
            }

            @Override
            public void disconnected() {
                setStatus("Disconnected");
                if (disposable != null) {
                    disposable.dispose();
                }
            }

            @Override
            public void incompatible() {
                setStatus("Incompatible");
                if (disposable != null) {
                    disposable.dispose();
                }
            }

            @Override
            public void incompatible(IOIO ioio) {
                incompatible();
            }
        };
    }
}
