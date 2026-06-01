# MFDemoApp

Android POS demo built for the YSDK engineering assessment.

The project demonstrates a real-shaped payment flow using MVVM, one Activity, Navigation, multiple Fragments, repositories, Room, external ISO8583 host communication, and a MoreFun/YSDK real-device adapter.

## Project Structure

- `app/src/main/java/com/franktardencilla/mfdemoapp/ui`: Fragments, ViewModels, adapters, and UI models.
- `app/src/main/java/com/franktardencilla/mfdemoapp/domain`: payment models, ISO8583 models, state machine, key readiness validation.
- `app/src/main/java/com/franktardencilla/mfdemoapp/repository`: persistence, host config, network, transactions, keys, and logs.
- `app/src/main/java/com/franktardencilla/mfdemoapp/device`: mock POS device, socket host client, and real MoreFun/YSDK adapter.
- `app/src/main/java/com/franktardencilla/mfdemoapp/data`: Room database entities and DAO interfaces.
- `docs`: architecture note, requirement traceability, and demo script.

## Prerequisites

- Android Studio with Android SDK installed.
- Java/Gradle support from the included Gradle wrapper.
- For real-device mode: a compatible MoreFun/YSDK terminal with the YSDK service available.
- For host authorization: run the external simulator from the simulator package.

## Build

From the project root:

```bash
./gradlew assembleMockDebug assembleRealDebug
```

Generated APKs:

```text
app/build/outputs/apk/mock/debug/app-mock-debug.apk
app/build/outputs/apk/real/debug/app-real-debug.apk
```

## Run

### Mock Variant

Use this when testing without the real YSDK hardware layer.

```bash
./gradlew installMockDebug
```

The mock variant still uses the external ISO8583 simulator for host authorization.

### Real Variant

Use this on the MoreFun/YSDK terminal.

```bash
./gradlew installRealDebug
```

Before a sale:

1. Start the external ISO8583 simulator.
2. Open the app.
3. Use the top connection menu to connect the terminal.
4. Let the app validate device services, network, and key readiness.
5. Tap the center plus button to start a sale.

## External Simulator

The simulator is a separate Java application located outside this Android repo:

```text
/Users/macbookpromax/Desktop/iso8583-java-simulator
```

A ready-to-run package is available at:

```text
/Users/macbookpromax/Desktop/MFDemo ISO8583 Simulator Package
```

It contains Mac and Windows launchers. The target computer needs Java 17 or newer.

Simulator behavior:

- `12.34` returns approved response `RC=00`.
- `99.99` returns declined response `RC=05`.
- TCP connection test is supported.
- ISO `0800 -> 0810` network test is supported.
- Field 55 is decoded in simulator logs.
- Field 64 MAC is validated when present.

## Tests

Run unit tests for both variants:

```bash
./gradlew testMockDebugUnitTest testRealDebugUnitTest
```

Current coverage includes:

- ISO8583 pack/unpack.
- ISO malformed frames.
- Required bitmap/Field 55 behavior.
- Amount breakdown.
- STAN generation.
- Key readiness.
- Sale state transitions.
- Declined transaction persistence.

## Delivery Documents

- `docs/ARCHITECTURE.md`: modules, transaction state machine, error/cancel strategy, and security considerations.
- `docs/DEMO_SCRIPT.md`: step-by-step demo scenarios and expected outcomes.
- `docs/REQUIREMENTS_TRACEABILITY.md`: mapping between requirements and implementation.
