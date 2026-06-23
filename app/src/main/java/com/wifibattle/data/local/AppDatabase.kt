package com.wifibattle.data.local

import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 历史对战记录（可选功能）
 */
@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey val matchId: String,
    val gameId: String,
    val roomName: String,
    val hostName: String,
    val playerCount: Int,
    val winnerId: String? = null,
    val startedAt: Long,
    val endedAt: Long = 0L
)

@Dao
interface MatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MatchHistoryEntity)

    @Query("SELECT * FROM match_history ORDER BY startedAt DESC LIMIT 50")
    fun observeRecent(): Flow<List<MatchHistoryEntity>>

    @Query("DELETE FROM match_history WHERE endedAt > 0 AND endedAt < :threshold")
    suspend fun cleanup(threshold: Long)
}

@Database(entities = [MatchHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchHistoryDao(): MatchHistoryDao
}
