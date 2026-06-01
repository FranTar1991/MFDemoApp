package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.data.transaction.TransactionDao
import com.franktardencilla.mfdemoapp.data.transaction.TransactionEntity
import com.franktardencilla.mfdemoapp.domain.model.CardEntryMode
import com.franktardencilla.mfdemoapp.domain.model.EmvTagSummary
import com.franktardencilla.mfdemoapp.domain.model.IsoMessageSummary
import com.franktardencilla.mfdemoapp.domain.model.MaskedPan
import com.franktardencilla.mfdemoapp.domain.model.MoneyAmount
import com.franktardencilla.mfdemoapp.domain.model.SaleAmountBreakdown
import com.franktardencilla.mfdemoapp.domain.model.SaleResult
import com.franktardencilla.mfdemoapp.domain.model.TransactionStatus
import com.franktardencilla.mfdemoapp.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionRepositoryTest {
    @Test
    fun saveSaleResult_persistsDeclinedTransactionWithHostDetails() = runBlocking {
        val dao = InMemoryTransactionDao()
        val repository = TransactionRepository(
            transactionDao = dao,
            ioDispatcher = Dispatchers.Unconfined
        )
        val breakdown = SaleAmountBreakdown.fromParts(
            baseAmount = MoneyAmount(minorUnits = 9_999),
            tipAmount = MoneyAmount(minorUnits = 0),
            taxAmount = MoneyAmount(minorUnits = 0)
        )

        val saved = repository.saveSaleResult(
            SaleResult(
                status = TransactionStatus.DECLINED,
                amount = breakdown.totalAmount,
                amountBreakdown = breakdown,
                stan = "000999",
                entryMode = CardEntryMode.CONTACT,
                maskedPan = MaskedPan("************1111"),
                responseCode = "05",
                authCode = "780271",
                emvTagSummary = EmvTagSummary(),
                isoRequest = IsoMessageSummary(
                    mti = "0200",
                    stan = "000999",
                    responseCode = null,
                    authCode = null,
                    redactedMessage = "F004=000000009999"
                ),
                isoResponse = IsoMessageSummary(
                    mti = "0210",
                    stan = "000999",
                    responseCode = "05",
                    authCode = "780271",
                    redactedMessage = "F039=05"
                ),
                message = "Host declined sale. Response code: 05"
            )
        )

        val stored = repository.getTransaction(saved.id)

        assertNotNull(stored)
        requireNotNull(stored)
        assertEquals(TransactionStatus.DECLINED, stored.status)
        assertEquals(9_999, stored.amount.minorUnits)
        assertEquals(9_999, stored.amountBreakdown.baseAmount.minorUnits)
        assertEquals("000999", stored.stan)
        assertEquals("05", stored.responseCode)
        assertEquals("780271", stored.authCode)
        assertTrue(stored.isoResponseSummary.orEmpty().contains("RC: 05"))
    }

    private class InMemoryTransactionDao : TransactionDao {
        private val transactions = linkedMapOf<String, TransactionEntity>()

        override fun getRecent(limit: Int): MutableList<TransactionEntity> {
            return transactions.values
                .sortedByDescending { it.createdAtMillis }
                .take(limit)
                .toMutableList()
        }

        override fun getById(id: String): TransactionEntity? {
            return transactions[id]
        }

        override fun getLatestStan(): String? {
            return transactions.values
                .maxByOrNull { it.createdAtMillis }
                ?.stan
        }

        override fun insert(transaction: TransactionEntity) {
            transactions[transaction.id] = transaction
        }

        override fun deleteAll() {
            transactions.clear()
        }
    }
}
