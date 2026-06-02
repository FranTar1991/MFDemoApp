# Requirements Traceability

This document maps the assessment requirements to the implemented demo app and simulator.

## 1. Device Service Session

Status: Implemented.

- The app opens and validates a terminal session before sale.
- The connection flow checks required modules such as PED/PinPad, EMV, printer, beeper, and network.
- The app shows actionable connection failures instead of freezing the UI.
- The connection state is remembered and restored on app launch.

Key files:

- `app/src/main/java/com/franktardencilla/mfdemoapp/domain/terminal/TerminalSessionUseCase.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/device/morefun/RealYsdkPosDeviceAdapter.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/MainActivity.kt`

## 2. Key Injection And Key Readiness

Status: Implemented for real MoreFun/YSDK mode.

- The real device path uses the MoreFun PinPad KEK/MK/SK style flow aligned with the SDK demo.
- The demo injects a KEK, an encrypted master key, a PIN working key, and a MAC working key.
- Readiness checks verify the master and MAC key slots before allowing sale.
- Disconnect warns the operator and clears transactions, logs, and loaded key state.
- Logs redact sensitive values.

Key files:

- `app/src/main/java/com/franktardencilla/mfdemoapp/device/morefun/RealYsdkPosDeviceAdapter.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/repository/AppLogRepository.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/domain/terminal/TrackAKeyReadinessValidator.kt`

## 3. EMV Card Reading And Transaction State Machine

Status: Implemented.

- Sale flow checks readiness before amount entry.
- Operator flow: amount, card, processing, voucher.
- Real mode uses YSDK EMV/card handling methods and supports contact, contactless, and magstripe paths.
- The app uses a state machine to prevent invalid sale transitions.
- Cancel is supported during card/processing flow.

Key files:

- `app/src/main/java/com/franktardencilla/mfdemoapp/ui/sale/SaleViewModel.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/domain/model/SaleStateMachine.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/device/morefun/RealYsdkPosDeviceAdapter.kt`

## 4. ISO8583 Pack/Unpack And Host Simulator

Status: Implemented.

- The app builds `0200` authorization requests and parses `0210` responses.
- Required fields include processing code, amount, transmission date/time, STAN, entry mode, NII, terminal, merchant, currency, Field 55, and Field 64 MAC.
- The simulator runs outside the app and supports sale approval, sale decline, and connection testing.
- Field 55 is logged as decoded EMV TLV in the simulator.
- MAC support is present on both app and simulator paths.

Key files:

- `app/src/main/java/com/franktardencilla/mfdemoapp/domain/model/Iso8583Packager.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/domain/model/SaleIsoRequestBuilder.kt`
- External simulator: `/Users/macbookpromax/Desktop/iso8583-java-simulator`

## 5. UI Requirements

Status: Implemented.

- Single activity with Navigation library and multiple fragments.
- Bottom navigation with center plus action for sale.
- Home screen lists recent transactions.
- Transaction detail opens the receipt for a selected transaction.
- Key, logs, host settings, sale, voucher, and transaction detail screens are separated by responsibility.
- Receipt can be printed.

Key files:

- `app/src/main/res/navigation/nav_graph.xml`
- `app/src/main/java/com/franktardencilla/mfdemoapp/ui/home/HomeFragment.kt`
- `app/src/main/java/com/franktardencilla/mfdemoapp/ui/sale`
- `app/src/main/java/com/franktardencilla/mfdemoapp/ui/transaction`

## 6. Mandatory Demo Scenarios

Status: Implemented.

- Approved sale: amount `12.34`, simulator response `RC=00`.
- Declined sale: amount `99.99`, simulator response `RC=05`.
- Cancel during card search: supported by sale state flow.
- Keys not injected: sale is blocked with an actionable key readiness message.

## 7. Tests

Status: Implemented and expanding.

- ISO8583 pack/unpack tests.
- ISO8583 malformed frame tests.
- Required bitmap/Field 55 tests.
- Amount breakdown tests.
- STAN generation tests.
- Key readiness tests.
- Sale state machine tests.
- Declined transaction persistence test.

Run:

```bash
./gradlew testDebugUnitTest
```

## Known Demo Boundaries

- Demo keys are not production keys.
- Real acquirer certification is out of scope.
- The host simulator is intentionally limited to the assignment scenarios.
- The packaged simulator requires Java 17 or newer on the computer that runs it.
