# Demo Script

Use this script to explain and demonstrate the assignment.

## Preparation

1. Start the external simulator.
   - Mac: open `Start MFDemo ISO8583 Simulator - Mac.command`.
   - Windows: open `Start MFDemo ISO8583 Simulator - Windows.bat`.
2. Confirm the simulator is listening on port `8001`.
3. Open the POS app.
4. Connect the device from the top menu.
5. Let the app check services, network, and key readiness.

## Explain The Architecture

The app is single-activity MVVM.

- Fragments handle screens.
- ViewModels hold screen state.
- Repositories isolate storage, network, logs, keys, and device calls.
- The device adapter can be mock or real.
- The simulator is external, not embedded in the app.

## Demo Scenario A: Approved Sale

1. Tap the plus button.
2. Enter base amount `12.34`.
3. Leave tip and tax as zero, or enter values if needed.
4. Continue to card.
5. Present/read card.
6. Confirm the simulator receives `0200`.
7. Confirm response `0210` with `RC=00`.
8. Confirm app shows approved voucher.
9. Return home and show the transaction in the list.
10. Tap the transaction and show receipt details.

Expected result:

- Transaction status: `APPROVED`.
- Response code: `00`.
- Transaction is stored.

## Demo Scenario B: Declined Sale

1. Start another sale.
2. Enter base amount `99.99`.
3. Continue through card and processing.
4. Confirm simulator returns `RC=05`.
5. Confirm app shows declined voucher.
6. Return home and show the declined transaction in the list.
7. Tap it and show receipt details.

Expected result:

- Transaction status: `DECLINED`.
- Response code: `05`.
- Transaction is stored, as required.

## Demo Scenario C: Cancel During Card Search

1. Start a sale with a valid amount.
2. Continue to card screen.
3. Tap cancel before card read completes.

Expected result:

- Sale moves to canceled state.
- No approved/declined transaction is stored because no host authorization happened.
- UI returns safely without invalid transitions.

## Demo Scenario D: Keys Not Ready

1. Disconnect the terminal from the menu.
2. Confirm warning dialog explains that transactions, logs, and keys will be cleared.
3. Try to start a sale without connecting/preparing keys.

Expected result:

- Sale is blocked.
- App shows actionable key readiness message.

## What To Point Out

- Key readiness is checked before sale.
- Field 55 is not printed on the customer voucher, but it is available in technical logs/summaries.
- ISO request and response summaries are stored with the transaction.
- Sensitive values are redacted in logs.
- Declined transactions are stored, not discarded.
- Simulator is external and can run on another computer with Java 17+.

## Useful Commands

App tests:

```bash
./gradlew testMockDebugUnitTest testRealDebugUnitTest
```

Build app:

```bash
./gradlew assembleRealDebug assembleMockDebug
```

Simulator build:

```bash
cd /Users/macbookpromax/Desktop/iso8583-java-simulator
./gradlew clean build
```
