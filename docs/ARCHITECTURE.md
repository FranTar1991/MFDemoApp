# Architecture Note

The app uses MVVM with a single Activity, Android Navigation, multiple Fragments, ViewModels, repositories, Room persistence, and an external ISO8583 simulator. The goal is to keep the operator workflow simple while keeping payment, device, storage, and host concerns separated enough to switch between mock and real device modes.

## Modules And Responsibilities

### UI

The UI layer is built around `MainActivity` and Fragments. `MainActivity` owns app-level navigation, bottom navigation, the center sale action, and terminal connection state. Each screen is a Fragment: home, key management, host settings, logs, sale amount, card entry, processing, voucher, and transaction receipt.

ViewModels expose screen state through observable values and delegate business work to repositories/use cases. For example, `SaleViewModel` coordinates the sale flow, but it does not talk directly to Room or Android services.

### Domain

The domain layer contains the payment models and rules:

- `SaleStateMachine` controls legal sale transitions.
- `SaleIsoRequestBuilder` builds ISO8583 `0200` messages.
- `TrackAKeyReadinessValidator` decides whether key state is sufficient for sale.
- `MoneyAmount` and `SaleAmountBreakdown` keep amount, tip, tax, and total handling explicit.
- `SaleResult` and `TransactionSummary` represent the completed transaction data used by storage and UI.

### Repositories

Repositories isolate persistence, device state, logs, network checks, and host configuration:

- `TransactionRepository` stores approved and declined transactions in Room.
- `KeyRepository` exposes key injection, readiness, and clearing operations.
- `HostConfigRepository` stores simulator host IP, fallback host, port, and timeout.
- `NetworkRepository` validates Android network availability.
- `AppLogRepository` stores redacted logs and mirrors them to Logcat.

### Device Adapters

The app can run through mock or real device adapters. Both return the same domain result types, so the UI flow remains the same.

- Mock mode simulates card/EMV/PED behavior while still using the external ISO8583 simulator.
- Real mode uses the MoreFun/YSDK jar for PinPad/PED, EMV card reading, beeper, printer, and MAC calculation.

## Transaction State Machine

The sale flow is state-driven to prevent invalid jumps, such as going from idle directly to host authorization.

```text
IDLE
 -> CHECKING_READINESS
 -> WAITING_FOR_CARD
 -> CARD_DETECTED
 -> READING_EMV
 -> EMV_DATA_READY
 -> WAITING_FOR_HOST
 -> APPROVED / DECLINED
```

`ERROR` and `CANCELED` are allowed from active states so the app can safely react to hardware failure, host failure, or operator cancellation. After a terminal state, the state machine can reset to `IDLE`.

## Sale Flow

```text
Check key readiness
 -> enter amount
 -> wait for card
 -> read card / EMV
 -> build ISO8583 0200
 -> calculate MAC
 -> send to host simulator
 -> receive 0210
 -> store transaction
 -> show/print voucher
```

The result is stored only when a host authorization completes and the device adapter returns `SaleDeviceResult.Completed`. Both approved and declined transactions are stored. Canceled sales and technical failures are shown to the operator but are not stored as approved/declined host transactions.

## Error And Cancel Strategy

The app separates business declines from technical failures:

- Host response `RC=00` becomes `APPROVED`.
- Host response other than `00`, such as `05`, becomes `DECLINED` and is stored.
- Device/service/network/key failures become actionable errors and block or stop the sale.
- Operator cancellation moves the flow to `CANCELED`.
- If the terminal disconnects, the app warns the user and clears local transactions, logs, and key state as requested.

The connection flow validates device services, Android network, and key readiness before sale. If a required module is unavailable, the message names the failing area instead of leaving the operator with a generic failure.

## Security Considerations

This is a demo implementation, not a production payment application.

- Demo keys are used; no production keys are included.
- Key readiness is checked through metadata/KCV-style status, not by exposing key values.
- Plaintext key material, full PAN, PIN blocks, Field 55, Field 64, and track data are redacted from app logs.
- The real YSDK path uses the secure PinPad/PED APIs for key loading and MAC calculation.
- Room simulates local transaction/log storage; sensitive payment data is masked.
- The external simulator validates MAC when Field 64 is present and can require MAC with `-DrequireMac=true`.

## External Simulator

The simulator is a separate Java application. It does not depend on the Android app.

It supports:

- TCP connection test.
- ISO `0800 -> 0810` network test.
- ISO `0200 -> 0210` sale approval/decline.
- Field 55 TLV logging.
- Field 64 MAC validation.

Packaged simulator files are on the Desktop in:

```text
/Users/macbookpromax/Desktop/MFDemo ISO8583 Simulator Package
```
