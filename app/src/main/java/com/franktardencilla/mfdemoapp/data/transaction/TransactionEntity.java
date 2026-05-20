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

    public long baseAmountMinorUnits;

    public long tipAmountMinorUnits;

    public long taxAmountMinorUnits;

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

    @Nullable
    public String isoRequestSummary;

    @Nullable
    public String isoResponseSummary;

    @Nullable
    public String emvTagSummary;

    public TransactionEntity(
            @NonNull String id,
            long amountMinorUnits,
            long baseAmountMinorUnits,
            long tipAmountMinorUnits,
            long taxAmountMinorUnits,
            @NonNull String currencyCode,
            @NonNull String currencySymbol,
            long createdAtMillis,
            @NonNull String status,
            @Nullable String stan,
            @Nullable String entryMode,
            @Nullable String maskedPan,
            @Nullable String responseCode,
            @Nullable String authCode,
            @Nullable String message,
            @Nullable String isoRequestSummary,
            @Nullable String isoResponseSummary,
            @Nullable String emvTagSummary
    ) {
        this.id = id;
        this.amountMinorUnits = amountMinorUnits;
        this.baseAmountMinorUnits = baseAmountMinorUnits;
        this.tipAmountMinorUnits = tipAmountMinorUnits;
        this.taxAmountMinorUnits = taxAmountMinorUnits;
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
        this.isoRequestSummary = isoRequestSummary;
        this.isoResponseSummary = isoResponseSummary;
        this.emvTagSummary = emvTagSummary;
    }
}
