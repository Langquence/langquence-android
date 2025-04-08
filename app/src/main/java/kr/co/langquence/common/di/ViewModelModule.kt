package kr.co.langquence.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kr.co.langquence.model.usecase.CorrectUseCase
import kr.co.langquence.presentation.viewmodel.home.CorrectionItemViewModel

@Module
@InstallIn(ActivityRetainedComponent::class)
class ViewModelModule {
    @Provides
    @ActivityRetainedScoped
    fun provideCorrectionItemViewModel(
        correctUseCase: CorrectUseCase
    ): CorrectionItemViewModel {
        return CorrectionItemViewModel(correctUseCase)
    }
}