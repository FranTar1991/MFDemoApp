package com.franktardencilla.mfdemoapp.repository

import com.franktardencilla.mfdemoapp.data.transaction.TransactionDao

class StanRepository(
    private val transactionDao: TransactionDao
) {
    @Synchronized
    fun nextStan(): String {
        val latestStan = transactionDao.getLatestStan()
        val latestValue = latestStan
            ?.filter(Char::isDigit)
            ?.toIntOrNull()
            ?: 0
        val nextValue = if (latestValue >= LAST_STAN) FIRST_STAN else latestValue + 1
        return nextValue.toString().padStart(STAN_LENGTH, '0')
    }

    private companion object {
        const val FIRST_STAN = 1
        const val LAST_STAN = 999999
        const val STAN_LENGTH = 6
    }
}
