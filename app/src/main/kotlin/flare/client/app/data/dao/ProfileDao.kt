package flare.client.app.data.dao

import androidx.room.*
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.ProfileSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT id, name, uri, serverDescription, subscriptionId, isSelected, protocol FROM profiles ORDER BY id ASC")
    fun getAllProfiles(): Flow<List<ProfileSummary>>

    @Query("SELECT * FROM profiles WHERE subscriptionId IS NULL ORDER BY id ASC")
    fun getStandaloneProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE uri = 'internal://json' OR uri = '' OR uri LIKE 'internal://json%'")
    suspend fun getJsonProfiles(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE subscriptionId = :subId ORDER BY id ASC")
    fun getProfilesBySubscription(subId: Long): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(profiles: List<ProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity): Long

    @Query("UPDATE profiles SET isSelected = 0")
    suspend fun clearSelection()

    @Query("UPDATE profiles SET isSelected = 1 WHERE id = :id")
    suspend fun selectProfile(id: Long)

    @Query("SELECT * FROM profiles WHERE isSelected = 1 LIMIT 1")
    suspend fun getSelectedProfile(): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id IN (:ids)")
    suspend fun getProfilesByIds(ids: List<Long>): List<ProfileEntity>

    @Query("UPDATE profiles SET configJson = :configJson WHERE id = :id")
    suspend fun updateConfigJson(id: Long, configJson: String)

    @Query("UPDATE profiles SET name = :name, configJson = :configJson, protocol = :protocol, serverDescription = :serverDescription WHERE id = :id")
    suspend fun updateProfile(id: Long, name: String, configJson: String, protocol: String?, serverDescription: String?)

    @Update
    suspend fun updateProfileFull(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE subscriptionId = :subId")
    suspend fun deleteBySubscriptionId(subId: Long)

    @Transaction
    suspend fun replaceSubscriptionProfiles(subId: Long, profiles: List<ProfileEntity>) {
        deleteBySubscriptionId(subId)
        insertAll(profiles.map { it.copy(subscriptionId = subId) })
    }

    @Query("DELETE FROM profiles WHERE subscriptionId IS NULL")
    suspend fun deleteStandaloneProfiles()
}
