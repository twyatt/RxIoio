package com.traviswyatt.rxioio2;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.FlowableOperator;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

class DigitalOutputOperator implements FlowableOperator<Boolean, Boolean> {

    private final IOIO ioio;
    private final DigitalOutput.Spec spec;
    private final boolean startValue;

    DigitalOutputOperator(IOIO ioio, DigitalOutput.Spec spec, boolean startValue) {
        this.ioio = ioio;
        this.spec = spec;
        this.startValue = startValue;
    }

    @Override
    public Subscriber<? super Boolean> apply(Subscriber<? super Boolean> downstream) throws Exception {
        return new Op(downstream);
    }

    private final class Op implements Subscriber<Boolean>, Subscription {

        private Subscription upstream;
        private final Subscriber<? super Boolean> downstream;

        private DigitalOutput output;

        Op(Subscriber<? super Boolean> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Subscription s) {
            upstream = s;

            try {
                output = ioio.openDigitalOutput(spec, startValue);
                downstream.onSubscribe(s);
            } catch (ConnectionLostException e) {
                upstream.cancel();
                downstream.onError(e);
            }
        }

        @Override
        public void onNext(Boolean aBoolean) {
            try {
                output.write(aBoolean);
                downstream.onNext(aBoolean);
            } catch (ConnectionLostException e) {
                upstream.cancel();
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

        @Override
        public void request(long n) {
            upstream.request(n);
        }

        @Override
        public void cancel() {
            upstream.cancel();
        }

    }

}
