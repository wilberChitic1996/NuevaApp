package gt.guardian.cadejo.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gt.guardian.cadejo.core.data.session.SystemSeedSource
import gt.guardian.cadejo.domain.session.SeedSource
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer. Binding interfaces to implementations here
 * (rather than in feature modules) is what lets features depend only on the
 * domain abstractions — the classic Dependency Inversion of Clean Architecture.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSeedSource(impl: SystemSeedSource): SeedSource
}
