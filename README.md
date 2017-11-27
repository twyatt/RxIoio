# RxIoio

Reactive APIs for [IOIO].
Designed to work alongside the standard [IOIOLooper paradigm].

Inspired by libraries such as [RxBinding] and [Rx Preferences].

## Usage

```java
// ...
@Override
public IOIOLooper createIOIOLooper(String connectionType, Object extra) {
    return new IOIOLooper() {
        @Override
        public void setup(IOIO ioio) throws ConnectionLostException, InterruptedException {
            RxIoio rxIoio = RxIoio.create(ioio);
            // ...
        }

        @Override
        public void disconnected() {
            disposable.dispose();
        }

        // ...
    }
}
// ...
```

### Input
```java
// ...
RxIoio rxIoio = RxIoio.create(ioio);
disposable = rxIoio
    .digitalInput(BUTTON_INPUT_PIN)
    .subscribeWith(new DisposableSubscriber<Boolean>() {
        @Override
        public void onNext(Boolean aBoolean) {
            System.out.println("Button pressed: " + aBoolean);
        }
        // ...
    }
// ...
```

### Output
```java
// ...
RxIoio rxIoio = RxIoio.create(ioio);

disposable = rxFlowable // Whereas rxFlowable provides a stream of Boolean objects.
    .lift(rxIoio.digitalOutput(STAT_LED_PIN))
    .subscribeWith(new DisposableSubscriber<Boolean>() {
        @Override
        public void onNext(Boolean aBoolean) {
            System.out.println("Setting stat LED pin to: " + aBoolean);
        }
        // ...
    }
// ...
```

_See [Android example] or [Desktop example] for more details._


[IOIO]: https://github.com/ytai/ioio
[IOIOLooper paradigm]: https://github.com/ytai/ioio/wiki/IOIOLib-Application-Framework
[RxBinding]: https://github.com/JakeWharton/RxBinding
[Rx Preferences]: https://github.com/f2prateek/rx-preferences
[Android example]: example-android
[Desktop example]: example-pc