package gt.guardian.cadejo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface RunHistoryDao {
    @Insert
    suspend fun insert(run: RunHistoryEntity)

    @Query("SELECT * FROM run_history ORDER BY finishedAt DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<RunHistoryEntity>>
}
