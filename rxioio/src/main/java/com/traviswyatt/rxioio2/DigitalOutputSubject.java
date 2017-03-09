package com.traviswyatt.rxioio2;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;

class DigitalOutputSubject extends FlowableProcessor<Boolean> implements Disposable {

    private final IOIO ioio;
    private final DigitalOutput.Spec spec;
    private final boolean startValue;

    private final PublishProcessor<Boolean> subject = PublishProcessor.create();
    private DigitalOutput output;
    private Subscription s;

    DigitalOutputSubject(IOIO ioio, DigitalOutput.Spec spec, boolean startValue) {
        this.ioio = ioio;
        this.spec = spec;
        this.startValue = startValue;
    }

    @Override
    public boolean hasSubscribers() {
        return subject.hasSubscribers();
    }

    @Override
    public boolean hasThrowable() {
        return subject.hasThrowable();
    }

    @Override
    public boolean hasComplete() {
        return subject.hasComplete();
    }

    @Override
    public Throwable getThrowable() {
        return subject.getThrowable();
    }

    @Override
    protected void subscribeActual(Subscriber<? super Boolean> s) {
        System.out.println("DigitalOutputSubject subscribeActual");
        subject.subscribeActual(s);
    }

    @Override
    public void onSubscribe(Subscription s) {
        System.out.println("DigitalOutputSubject onSubscribe, pin=" + spec.pin);
        this.s = s;
        subject.onSubscribe(s);
        try {
            output = ioio.openDigitalOutput(spec, startValue);
        } catch (ConnectionLostException e) {
            subject.onError(e);
        }
    }

    @Override
    public void onNext(Boolean aBoolean) {
        try {
            System.out.println("DigitalOutputSubject " + this + " onNext pin = " + spec.pin + ", aBoolean = " + aBoolean);
            if (!subject.hasComplete() && !subject.hasThrowable()) {
                output.write(aBoolean);
            }
            subject.onNext(aBoolean);
        } catch (ConnectionLostException e) {
            subject.onError(e);
        }
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("DigitalOutputSubject onError pin = " + spec.pin + ", t = " + t);

        subject.onError(cleanupAfterError(t));
    }

    @Override
    public void onComplete() {
        System.out.println("DigitalOutputSubject onComplete pin = " + spec.pin);
        output.close();
        subject.onComplete();
    }

    @Override
    public void dispose() {
        System.out.println("DigitalOutputSubject " + this + " dispose");
        s.cancel();
        s = null;
    }

    @Override
    public boolean isDisposed() {
        return s == null;
    }

    /**
     * Only try to close the output if the error state isn't due to the
     * connection being lost. As attempting to close after a connection loss
     * doesn't work anyways, results in:
     * <pre><code>
     *     purejavacomm.PureJavaIllegalStateException: File descriptor is -1 < 0, maybe closed by previous error condition
     * </code></pre>
     */
    private Throwable cleanupAfterError(Throwable t) {
        if (!(t instanceof ConnectionLostException)) {
            try {
                output.close();
            } catch (Exception e) {
                return new CompositeException(t, e);
            }
        }
        return t;
    }
}
