package com.franktardencilla.mfdemoapp.ui.common

import androidx.fragment.app.Fragment
import com.franktardencilla.mfdemoapp.app.DemoApplication

fun Fragment.appViewModelFactory(): AppViewModelFactory {
    val application = requireActivity().application as DemoApplication
    return AppViewModelFactory(application.appContainer)
}
