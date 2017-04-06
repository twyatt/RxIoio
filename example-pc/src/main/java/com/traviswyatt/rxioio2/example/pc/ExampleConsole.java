package com.traviswyatt.rxioio2.example.pc;

import com.traviswyatt.rxioio2.RxIoio;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.pc.IOIOPcApplicationHelper;
import java.util.concurrent.TimeUnit;

public class ExampleConsole implements IOIOLooperProvider {
    private final IOIOPcApplicationHelper helper = new IOIOPcApplicationHelper(this);
    private volatile boolean done;

    public static void main(String[] args) throws Exception {
        new ExampleConsole().go();
    }

    private synchronized void go() throws Exception {
        helper.start();
        try {
            while (!done) {
                wait();
            }
        } finally {
            helper.stop();
        }
    }

    /** Stop IOIO and shutdown application. */
    private synchronized void done() {
        done = true;
        notifyAll();
    }

    @Override
    public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
        return new IOIOLooper() {
            private static final int STAT_LED_PIN = 0;

            @Override
            public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
                System.out.println("connected");
                RxIoio rxIoio = RxIoio.create(ioio);

                // Basic example that blinks IOIO stat LED 5 times.
                Observable
                        .range(1, 10)
                        .map(new Function<Integer, Boolean>() {
                            @Override
                            public Boolean apply(@NonNull Integer integer) throws Exception {
                                return integer % 2 == 0;
                            }
                        })
                        .zipWith(Observable.interval(1000L, TimeUnit.MILLISECONDS), new BiFunction<Boolean, Long, Boolean>() {
                            @Override
                            public Boolean apply(@NonNull Boolean aBoolean, @NonNull Long aLong) throws Exception {
                                return aBoolean;
                            }
                        })
                        .lift(rxIoio.digitalOutput(STAT_LED_PIN))
                        .blockingSubscribe(new Observer<Boolean>() {
                            @Override public void onSubscribe(Disposable d) {
                                // no-op
                            }

                            @Override
                            public void onNext(Boolean aBoolean) {
                                System.out.println("Setting stat LED pin to: " + aBoolean);
                            }

                            @Override
                            public void onError(Throwable t) {
                                System.err.println("Error: " + t);
                            }

                            @Override
                            public void onComplete() {
                                System.out.println("Done");
                            }
                        });

                done();
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                Thread.sleep(1000L);
            }

            @Override
            public void disconnected() {
                System.out.println("disconnected");
            }

            @Override
            public void incompatible() {
                System.out.println("incompatible");
            }

            @Override
            public void incompatible(IOIO ioio) {
                incompatible();
            }
        };
    }
}
