package gt.guardian.cadejo.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single progression row. Sensitive fields (coins, unlocks, adsRemoved) are
 * covered by [signature] — an HMAC over their canonical form (see the domain
 * integrity package). [selectedSkin] and the stats are not economy-relevant and
 * are intentionally left out of the signed payload.
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val coins: Long,
    val unlocksCsv: String,
    val selectedSkin: String?,
    val adsRemoved: Boolean,
    val totalRuns: Int,
    val bestScore: Int,
    val signature: ByteArray,
) {
    // Room entities with a ByteArray need value-based equals/hashCode.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProfileEntity) return false
        return id == other.id &&
            coins == other.coins &&
            unlocksCsv == other.unlocksCsv &&
            selectedSkin == other.selectedSkin &&
            adsRemoved == other.adsRemoved &&
            totalRuns == other.totalRuns &&
            bestScore == other.bestScore &&
            signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + coins.hashCode()
        result = 31 * result + unlocksCsv.hashCode()
        result = 31 * result + (selectedSkin?.hashCode() ?: 0)
        result = 31 * result + adsRemoved.hashCode()
        result = 31 * result + totalRuns
        result = 31 * result + bestScore
        result = 31 * result + signature.contentHashCode()
        return result
    }

    companion object {
        const val SINGLETON_ID = 0
    }
}

/** One finished run, for history and stats. */
@Entity(tableName = "run_history")
data class RunHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seed: Long,
    val mode: String,
    val reachedLevel: Int,
    val score: Int,
    val outcome: String,
    val durationMs: Long,
    val finishedAt: Long,
)
