package gt.guardian.cadejo.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gt.guardian.cadejo.core.data.db.CadejoDatabase
import gt.guardian.cadejo.core.data.db.ProfileDao
import gt.guardian.cadejo.core.data.db.RunHistoryDao
import javax.inject.Singleton

/** Provides the Room database and its DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CadejoDatabase =
        Room.databaseBuilder(context, CadejoDatabase::class.java, CadejoDatabase.NAME)
            // No destructive fallback: real migrations only, so progress is never wiped.
            .build()

    @Provides
    fun provideProfileDao(db: CadejoDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideRunHistoryDao(db: CadejoDatabase): RunHistoryDao = db.runHistoryDao()
}
