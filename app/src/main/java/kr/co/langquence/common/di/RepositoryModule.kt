package kr.co.langquence.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.langquence.data.remote.CorrectApi
import kr.co.langquence.data.repository.CorrectRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryModule {
	@Singleton
	@Provides
	fun provideCorrectRepository(api: CorrectApi): CorrectRepositoryImpl = CorrectRepositoryImpl(api)
}