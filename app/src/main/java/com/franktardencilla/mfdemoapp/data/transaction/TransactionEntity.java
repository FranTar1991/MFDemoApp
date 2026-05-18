package com.franktardencilla.mfdemoapp.data.transaction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transaction_records")
public class TransactionEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public long amountMinorUnits;

    @NonNull
    public String currencyCode;

    @NonNull
    public String currencySymbol;

    public long createdAtMillis;

    @NonNull
    public String status;

    @Nullable
    public String stan;

    @Nullable
    public String entryMode;

    @Nullable
    public String maskedPan;

    @Nullable
    public String responseCode;

    @Nullable
    public String authCode;

    @Nullable
    public String message;

    public TransactionEntity(
            @NonNull String id,
            long amountMinorUnits,
            @NonNull String currencyCode,
            @NonNull String currencySymbol,
            long createdAtMillis,
            @NonNull String status,
            @Nullable String stan,
            @Nullable String entryMode,
            @Nullable String maskedPan,
            @Nullable String responseCode,
            @Nullable String authCode,
            @Nullable String message
    ) {
        this.id = id;
        this.amountMinorUnits = amountMinorUnits;
        this.currencyCode = currencyCode;
        this.currencySymbol = currencySymbol;
        this.createdAtMillis = createdAtMillis;
        this.status = status;
        this.stan = stan;
        this.entryMode = entryMode;
        this.maskedPan = maskedPan;
        this.responseCode = responseCode;
        this.authCode = authCode;
        this.message = message;
    }
}
