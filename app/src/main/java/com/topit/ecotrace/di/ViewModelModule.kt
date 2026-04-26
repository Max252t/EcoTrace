package com.topit.ecotrace.di

import androidx.lifecycle.ViewModel
import com.topit.ecotrace.presentation.viewmodel.AddReportViewModel
import com.topit.ecotrace.presentation.viewmodel.AuthViewModel
import com.topit.ecotrace.presentation.viewmodel.MapViewModel
import com.topit.ecotrace.presentation.viewmodel.MyReportsViewModel
import com.topit.ecotrace.presentation.viewmodel.ReportDetailsViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
interface ViewModelModule {
    @Binds @IntoMap @ViewModelKey(MapViewModel::class)
    fun bindMapViewModel(viewModel: MapViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(AddReportViewModel::class)
    fun bindAddReportViewModel(viewModel: AddReportViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(AuthViewModel::class)
    fun bindAuthViewModel(viewModel: AuthViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(MyReportsViewModel::class)
    fun bindMyReportsViewModel(viewModel: MyReportsViewModel): ViewModel

    @Binds @IntoMap @ViewModelKey(ReportDetailsViewModel::class)
    fun bindReportDetailsViewModel(viewModel: ReportDetailsViewModel): ViewModel
}
