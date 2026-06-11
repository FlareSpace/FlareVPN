package flare.client.app.data.dao

import androidx.room.*
import flare.client.app.data.model.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY pinned > 0 DESC, pinned ASC, id ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity): Long

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Delete
    suspend fun delete(subscription: SubscriptionEntity)

    @Query("UPDATE subscriptions SET name = :name, url = :url WHERE id = :id")
    suspend fun updateSubscription(id: Long, name: String, url: String)
    @Query("UPDATE subscriptions SET pinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Long)
    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
