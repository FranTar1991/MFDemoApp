package com.franktardencilla.mfdemoapp

import com.franktardencilla.mfdemoapp.data.transaction.TransactionDao
import com.franktardencilla.mfdemoapp.data.transaction.TransactionEntity
import com.franktardencilla.mfdemoapp.repository.StanRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class StanRepositoryTest {
    @Test
    fun nextStan_returnsOneWhenThereIsNoPreviousTransaction() {
        val repository = StanRepository(
            transactionDao = fakeTransactionDao(latestStan = null)
        )

        assertEquals("000001", repository.nextStan())
    }

    @Test
    fun nextStan_usesLatestTransactionStanFromDatabase() {
        val repository = StanRepository(
            transactionDao = fakeTransactionDao(latestStan = "000123")
        )

        assertEquals("000124", repository.nextStan())
    }

    @Test
    fun nextStan_wrapsAfterLastSixDigitStan() {
        val repository = StanRepository(
            transactionDao = fakeTransactionDao(latestStan = "999999")
        )

        assertEquals("000001", repository.nextStan())
    }

    private fun fakeTransactionDao(latestStan: String?): TransactionDao {
        return object : TransactionDao {
            override fun getRecent(limit: Int): MutableList<TransactionEntity> {
                return mutableListOf()
            }

            override fun getLatestStan(): String? {
                return latestStan
            }

            override fun getById(id: String): TransactionEntity? {
                return null
            }

            override fun insert(transaction: TransactionEntity) = Unit

            override fun deleteAll() = Unit
        }
    }
}
