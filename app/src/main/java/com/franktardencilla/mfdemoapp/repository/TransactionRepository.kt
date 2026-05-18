package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.data.transaction.TransactionDao
import com.franktardencilla.mfdemoapp.data.transaction.TransactionEntity
import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.MaskedPan
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.TransactionSummary
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun saveSaleResult(saleResult: SaleResult): TransactionSummary {
        return withContext(ioDispatcher) {
            val entity = saleResult.toEntity()
            transactionDao.insert(entity)
            entity.toSummary()
        }
    }

    suspend fun getRecentTransactions(limit: Int = DEFAULT_LIMIT): List<TransactionSummary> {
        return withContext(ioDispatcher) {
            transactionDao.getRecent(limit).map { entity ->
                entity.toSummary()
            }
        }
    }

    suspend fun clearTransactions() {
        withContext(ioDispatcher) {
            transactionDao.deleteAll()
        }
    }

    private fun SaleResult.toEntity(): TransactionEntity {
        return TransactionEntity(
            UUID.randomUUID().toString(),
            amount.minorUnits,
            amount.currencyCode,
            amount.currencySymbol,
            System.currentTimeMillis(),
            status.name,
            stan,
            entryMode?.name,
            maskedPan?.value,
            responseCode,
            authCode,
            message
        )
    }

    private fun TransactionEntity.toSummary(): TransactionSummary {
        return TransactionSummary(
            id = id,
            amount = MoneyAmount(
                minorUnits = amountMinorUnits,
                currencyCode = currencyCode,
                currencySymbol = currencySymbol
            ),
            createdAtMillis = createdAtMillis,
            status = runCatching { TransactionStatus.valueOf(status) }
                .getOrDefault(TransactionStatus.ERROR),
            stan = stan,
            entryMode = entryMode?.let { value ->
                runCatching { CardEntryMode.valueOf(value) }.getOrNull()
            },
            maskedPan = maskedPan?.let(::MaskedPan)
        )
    }

    private companion object {
        const val DEFAULT_LIMIT = 25
    }
}
