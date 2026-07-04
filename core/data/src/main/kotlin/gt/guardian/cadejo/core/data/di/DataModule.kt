package gt.guardian.cadejo.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gt.guardian.cadejo.core.data.daily.DailyDateProvider
import gt.guardian.cadejo.core.data.daily.SupabaseLeaderboardRepository
import gt.guardian.cadejo.core.data.progress.RoomProgressRepository
import gt.guardian.cadejo.core.data.save.KeystoreHmacSigner
import gt.guardian.cadejo.core.data.session.SystemSeedSource
import gt.guardian.cadejo.core.data.settings.DataStoreSettingsRepository
import gt.guardian.cadejo.domain.daily.DateProvider
import gt.guardian.cadejo.domain.daily.LeaderboardRepository
import gt.guardian.cadejo.domain.integrity.Signer
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.session.SeedSource
import gt.guardian.cadejo.domain.settings.SettingsRepository
import javax.inject.Singleton

/**
 * Binds domain interfaces to their data-layer implementations. Doing this here —
 * not in feature modules — is what lets features depend only on the domain
 * abstractions (Dependency Inversion of Clean Architecture).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSeedSource(impl: SystemSeedSource): SeedSource

    @Binds
    @Singleton
    abstract fun bindSigner(impl: KeystoreHmacSigner): Signer

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: RoomProgressRepository): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindDateProvider(impl: DailyDateProvider): DateProvider

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(impl: SupabaseLeaderboardRepository): LeaderboardRepository
}
