package com.traviswyatt.rxioio2;

import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

class DigitalOutputOperator implements ObservableOperator<Boolean, Boolean> {
    private final IOIO ioio;
    private final DigitalOutput.Spec spec;
    private final boolean startValue;

    DigitalOutputOperator(IOIO ioio, DigitalOutput.Spec spec, boolean startValue) {
        this.ioio = ioio;
        this.spec = spec;
        this.startValue = startValue;
    }

    @Override
    public Observer<? super Boolean> apply(Observer<? super Boolean> downstream) throws Exception {
        return new Op(downstream);
    }

    private final class Op implements Observer<Boolean> {
        private Disposable upstream;
        private final Observer<? super Boolean> downstream;

        private DigitalOutput output;

        Op(Observer<? super Boolean> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Disposable d) {
            upstream = d;

            try {
                output = ioio.openDigitalOutput(spec, startValue);
                downstream.onSubscribe(d);
            } catch (ConnectionLostException e) {
                upstream.dispose();
                downstream.onError(e);
            }
        }

        @Override
        public void onNext(Boolean aBoolean) {
            try {
                output.write(aBoolean);
                downstream.onNext(aBoolean);
            } catch (ConnectionLostException e) {
                upstream.dispose();
                downstream.onError(e);
            }
        }

        @Override
        public void onError(Throwable t) {
            downstream.onError(t);
        }

        @Override
        public void onComplete() {
            output.close();
            downstream.onComplete();
        }
    }
}
