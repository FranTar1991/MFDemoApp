package com.franktardencilla.mfdemoapp.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.franktardencilla.mfdemoapp.app.AppContainer
import com.franktardencilla.mfdemoapp.ui.home.HomeViewModel
import com.franktardencilla.mfdemoapp.ui.keys.KeyManagementViewModel
import com.franktardencilla.mfdemoapp.ui.logs.LogsViewModel
import com.franktardencilla.mfdemoapp.ui.result.ResultViewModel
import com.franktardencilla.mfdemoapp.ui.sale.SaleViewModel

class AppViewModelFactory(
    private val appContainer: AppContainer
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    appContainer.deviceRepository,
                    appContainer.keyRepository
                )
            }
            modelClass.isAssignableFrom(KeyManagementViewModel::class.java) -> {
                KeyManagementViewModel(
                    appContainer.keyRepository,
                    appContainer.appLogRepository
                )
            }
            modelClass.isAssignableFrom(SaleViewModel::class.java) -> {
                SaleViewModel(
                    appContainer.deviceRepository,
                    appContainer.keyRepository,
                    appContainer.saleRepository,
                    appContainer.appLogRepository
                )
            }
            modelClass.isAssignableFrom(ResultViewModel::class.java) -> {
                ResultViewModel()
            }
            modelClass.isAssignableFrom(LogsViewModel::class.java) -> {
                LogsViewModel(appContainer.appLogRepository)
            }
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
