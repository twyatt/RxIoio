package com.traviswyatt.rxioio2;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableOperator;
import io.reactivex.functions.Cancellable;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;

public class RxIoio {

    private final IOIO ioio;
    private final long interval;

    private RxIoio(IOIO ioio, long interval) {
        this.ioio = ioio;
        this.interval = interval;
    }

    public static RxIoio create(IOIO ioio) {
        return create(ioio, 0L);
    }

    public static RxIoio create(IOIO ioio, long interval) {
        if (interval < 0L) {
            throw new IllegalArgumentException("interval < 0: " + interval);
        }

        return new RxIoio(ioio, interval);
    }

    public Flowable<Boolean> digitalInput(final DigitalInput.Spec spec) {
        return Flowable
                .create(new FlowableOnSubscribe<Boolean>() {
                    @Override
                    public void subscribe(final FlowableEmitter<Boolean> emitter) throws Exception {
                        final DigitalInput input = ioio.openDigitalInput(spec);
                        while (!emitter.isCancelled() && ioio.getState() == IOIO.State.CONNECTED) {
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
                }, BackpressureStrategy.LATEST)
                .share();
    }

    public Flowable<Boolean> digitalInput(int pin, DigitalInput.Spec.Mode mode) {
        return digitalInput(new DigitalInput.Spec(pin, mode));
    }

    public Flowable<Boolean> digitalInput(int pin) {
        return digitalInput(new DigitalInput.Spec(pin));
    }

    public Flowable<Float> analogInput(final int pin) {
        return Flowable
                .create(new FlowableOnSubscribe<Float>() {
                    @Override
                    public void subscribe(final FlowableEmitter<Float> emitter) throws Exception {
                        final AnalogInput input = ioio.openAnalogInput(pin);
                        while (!emitter.isCancelled() && ioio.getState() == IOIO.State.CONNECTED) {
                            emitter.onNext(input.readSync());

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
                }, BackpressureStrategy.LATEST)
                .share();
    }

    public FlowableOperator<Boolean, Boolean> digitalOutput(DigitalOutput.Spec spec, boolean startValue) {
        return new DigitalOutputOperator(ioio, spec, startValue);
    }

    public FlowableOperator<Boolean, Boolean> digitalOutput(int pin, DigitalOutput.Spec.Mode mode, boolean startValue) {
        return digitalOutput(new DigitalOutput.Spec(pin, mode), startValue);
    }

    public FlowableOperator<Boolean, Boolean> digitalOutput(int pin, boolean startValue) {
        return digitalOutput(new DigitalOutput.Spec(pin), startValue);
    }

    public FlowableOperator<Boolean, Boolean> digitalOutput(int pin) {
        return digitalOutput(new DigitalOutput.Spec(pin), false);
    }

}
