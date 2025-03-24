package kr.co.langquence.common.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kr.co.langquence.data.repository.CorrectRepositoryImpl
import kr.co.langquence.model.repository.CorrectRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
	@Singleton
	@Binds
	abstract fun bindCorrectRepository(
		correctRepositoryImpl: CorrectRepositoryImpl
	): CorrectRepository
}