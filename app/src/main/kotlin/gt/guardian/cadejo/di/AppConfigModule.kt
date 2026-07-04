package gt.guardian.cadejo.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gt.guardian.cadejo.BuildConfig
import gt.guardian.cadejo.core.data.daily.LeaderboardConfig
import javax.inject.Singleton

/** App-level config providers that read from BuildConfig (populated from secrets). */
@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Singleton
    fun provideLeaderboardConfig(): LeaderboardConfig =
        LeaderboardConfig(baseUrl = BuildConfig.SUPABASE_URL, anonKey = BuildConfig.SUPABASE_ANON_KEY)
}
