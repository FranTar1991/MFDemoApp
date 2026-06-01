# Architecture

The app uses MVVM with a single activity, Navigation library, repositories, and small domain classes.

## Main Layers

### UI Layer

Fragments render the operator workflow and delegate behavior to ViewModels.

- `MainActivity` owns app-level navigation and terminal connection status.
- `HomeFragment` shows recent transactions.
- Sale fragments split the workflow into amount, card, processing, and voucher steps.
- `LogsFragment`, `KeyManagementFragment`, and `HostSettingsFragment` expose support screens.

### ViewModel Layer

ViewModels hold screen state and coordinate use cases/repositories.

- `SaleViewModel` owns the sale flow and sale state machine.
- `HomeViewModel` loads recent transactions.
- `TransactionReceiptViewModel` loads a stored transaction receipt.
- `HostSettingsViewModel` validates and saves simulator host settings.

### Domain Layer

Domain classes model payment concepts and enforce important rules.

- `SaleStateMachine` prevents invalid transaction transitions.
- `SaleIsoRequestBuilder` creates the ISO8583 authorization request.
- `TrackAKeyReadinessValidator` decides whether keys are ready for sale.
- `MoneyAmount` and `SaleAmountBreakdown` keep amount handling explicit.

### Repository Layer

Repositories hide persistence, Android services, and device integration details.

- `TransactionRepository` stores approved and declined transactions.
- `KeyRepository` exposes key injection/readiness operations.
- `NetworkRepository` checks Android network availability.
- `HostConfigRepository` stores simulator host settings.
- `AppLogRepository` stores redacted logs and mirrors them to Logcat.

### Device Layer

The app can run through mock or real device adapters.

- Mock mode simulates a POS device while still using the external ISO8583 simulator.
- Real mode uses the MoreFun/YSDK jar for PED/PinPad, EMV, beeper, and printer operations.
- Both modes return the same domain result types so the UI does not care which backend is active.

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

## Simulator

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
