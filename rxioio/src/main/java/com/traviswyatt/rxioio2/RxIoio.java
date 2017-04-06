package com.traviswyatt.rxioio2;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableOperator;
import io.reactivex.functions.Cancellable;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;

import static com.traviswyatt.rxioio2.internal.Preconditions.checkArgument;
import static com.traviswyatt.rxioio2.internal.Preconditions.checkNotNull;

public class RxIoio {

    private final IOIO ioio;

    /**
     * Creates an {@link RxIoio} wrapper around a {@link IOIO} that may be used to open
     * input/outputs.
     */
    public static RxIoio create(IOIO ioio) {
        return new RxIoio(ioio);
    }

    private RxIoio(IOIO ioio) {
        this.ioio = ioio;
    }

    /**
     * Creates an {@link Observable} that opens a digital input when subscribed to. The
     * {@code interval} specifies the delay between each digital input request.
     */
    public Observable<Boolean> digitalInput(final DigitalInput.Spec spec, final long interval) {
        checkNotNull(spec, "spec cannot be null");
        checkArgument(interval >= 0L, "interval must be >= 0, was " + interval);

        return Observable
            .create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                    final DigitalInput input = ioio.openDigitalInput(spec);
                    while (ioio.getState() == IOIO.State.CONNECTED && !emitter.isDisposed()) {
                        emitter.onNext(input.read());

                        if (interval != 0L) {
                            Thread.sleep(interval);
                        }
                    }

                    emitter.setCancellable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            input.close();
                        }
                    });
                }
            });
    }

    /**
     * Creates an {@link Observable} that opens a digital input when subscribed to and polls the
     * input as quickly as possible (with an {@code interval} of {@code 0L}).
     *
     * @see #digitalInput(DigitalInput.Spec, long)
     */
    public Observable<Boolean> digitalInput(final DigitalInput.Spec spec) {
        return digitalInput(spec, 0L);
    }

    /**
     * @see #digitalInput(DigitalInput.Spec, long)
     */
    public Observable<Boolean> digitalInput(int pin, DigitalInput.Spec.Mode mode, long interval) {
        return digitalInput(new DigitalInput.Spec(pin, mode), interval);
    }

    /**
     * @see #digitalInput(DigitalInput.Spec)
     */
    public Observable<Boolean> digitalInput(int pin, DigitalInput.Spec.Mode mode) {
        return digitalInput(pin, mode, 0L);
    }

    /**
     * @see #digitalInput(DigitalInput.Spec, long)
     */
    public Observable<Boolean> digitalInput(int pin, long interval) {
        return digitalInput(new DigitalInput.Spec(pin));
    }

    /**
     * @see #digitalInput(DigitalInput.Spec)
     */
    public Observable<Boolean> digitalInput(int pin) {
        return digitalInput(pin, 0L);
    }

    /**
     * Creates an {@link Observable} that opens an analog input when subscribed to. The
     * {@code interval} specifies the delay between each analog input request.
     * <p/>
     * When an {@code interval} of {@code 0L} is specified, then the {@link AnalogInput#readSync()}
     * is used, resulting in a polling rate that is throttled by the speed at which the IOIO can
     * obtain <b>new</b> analog readings.
     * <p/>
     * A non-zero {@code interval} will poll at the specified {@code interval} by using
     * {@link AnalogInput#read()} which returns the most recent value available to the IOIO and may
     * be stale (duplicate of a previously polled value).
     */
    public Observable<Float> analogInput(final int pin, final long interval) {
        checkArgument(interval >= 0L, "interval must be >= 0, was " + interval);

        return Observable
            .create(new ObservableOnSubscribe<Float>() {
                @Override public void subscribe(ObservableEmitter<Float> emitter) throws Exception {
                    final AnalogInput input = ioio.openAnalogInput(pin);

                    while (ioio.getState() == IOIO.State.CONNECTED && !emitter.isDisposed()) {
                        final float value;
                        if (interval != 0L) {
                            value = input.read();
                            Thread.sleep(interval);
                        } else {
                            value = input.readSync();
                        }
                        emitter.onNext(value);
                    }

                    emitter.setCancellable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            input.close();
                        }
                    });
                }
            });
    }

    /**
     * Creates an {@link Observable} that opens an analog input when subscribed to and polls the
     * input as quickly as possible (with an {@code interval} of {@code 0L}).
     *
     * @see #analogInput(int, long)
     */
    public Observable<Float> analogInput(final int pin) {
        return analogInput(pin, 0L);
    }

    /**
     * Creates an {@link ObservableOperator} that opens a digital output when subscribed to.
     * Receives {@link Boolean}s from upstream and writes value to digital output before passing
     * through to downstream.
     */
    public ObservableOperator<Boolean, Boolean> digitalOutput(DigitalOutput.Spec spec,
                                                              boolean startValue) {
        checkNotNull(spec, "spec cannot be null");
        return new DigitalOutputOperator(ioio, spec, startValue);
    }

    /**
     * @see #digitalOutput(DigitalOutput.Spec, boolean)
     */
    public ObservableOperator<Boolean, Boolean> digitalOutput(int pin, DigitalOutput.Spec.Mode mode,
                                                              boolean startValue) {
        return digitalOutput(new DigitalOutput.Spec(pin, mode), startValue);
    }

    /**
     * @see #digitalOutput(DigitalOutput.Spec, boolean)
     */
    public ObservableOperator<Boolean, Boolean> digitalOutput(int pin, boolean startValue) {
        return digitalOutput(new DigitalOutput.Spec(pin), startValue);
    }

    /**
     * @see #digitalOutput(DigitalOutput.Spec, boolean)
     */
    public ObservableOperator<Boolean, Boolean> digitalOutput(int pin) {
        return digitalOutput(new DigitalOutput.Spec(pin), false);
    }
}
