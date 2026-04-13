package com.topit.ecotrace.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topit.ecotrace.EcoTraceApp

@Composable
inline fun <reified VM : ViewModel> daggerViewModel(): VM {
    val app = LocalContext.current.applicationContext as EcoTraceApp
    return viewModel(factory = app.appComponent.viewModelFactory())
}
