package flare.client.app.data.repository

import androidx.room.Transaction
import flare.client.app.data.dao.ProfileDao
import flare.client.app.data.dao.SubscriptionDao
import flare.client.app.data.model.ProfileEntity
import flare.client.app.data.model.ProfileSummary
import flare.client.app.data.model.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val subscriptionDao: SubscriptionDao
) {

    fun getAllProfiles(): Flow<List<ProfileSummary>> = profileDao.getAllProfiles()
    fun getStandaloneProfiles(): Flow<List<ProfileEntity>> = profileDao.getStandaloneProfiles()
    suspend fun getJsonProfiles(): List<ProfileEntity> = profileDao.getJsonProfiles()
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()
    fun getProfilesBySubscription(subId: Long): Flow<List<ProfileEntity>> =
        profileDao.getProfilesBySubscription(subId)

    suspend fun getProfileById(id: Long): ProfileEntity? = profileDao.getProfileById(id)
    suspend fun getProfilesByIds(ids: List<Long>): List<ProfileEntity> = profileDao.getProfilesByIds(ids)

    suspend fun insertProfile(profile: ProfileEntity): Long = profileDao.insert(profile)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long = subscriptionDao.insert(subscription)

    @Transaction
    suspend fun insertSubscriptionWithProfiles(
        subscription: SubscriptionEntity,
        profiles: List<ProfileEntity>
    ) {
        val subId = subscriptionDao.insert(subscription)
        val withSubId = profiles.map { it.copy(subscriptionId = subId) }
        profileDao.insertAll(withSubId)
    }

    suspend fun deleteProfile(id: Long) = profileDao.deleteById(id)
    suspend fun deleteSubscription(subscription: SubscriptionEntity) =
        subscriptionDao.delete(subscription)

    suspend fun deleteSubscriptionById(id: Long) =
        subscriptionDao.deleteById(id)

    suspend fun selectProfile(id: Long) {
        profileDao.clearSelection()
        profileDao.selectProfile(id)
    }

    suspend fun getSelectedProfile(): ProfileEntity? = profileDao.getSelectedProfile()
    suspend fun updateProfileConfig(id: Long, configJson: String) =
        profileDao.updateConfigJson(id, configJson)

    suspend fun updateProfile(id: Long, name: String, configJson: String, protocol: String?, serverDescription: String?) =
        profileDao.updateProfile(id, name, configJson, protocol, serverDescription)

    suspend fun updateProfileFull(profile: ProfileEntity) =
        profileDao.updateProfileFull(profile)

    suspend fun updateSubscription(id: Long, name: String, url: String) =
        subscriptionDao.updateSubscription(id, name, url)

    suspend fun updateSubscriptionPinned(id: Long, pinned: Long) =
        subscriptionDao.updatePinned(id, pinned)

    suspend fun deleteProfilesBySubscription(subId: Long) =
        profileDao.deleteBySubscriptionId(subId)

    suspend fun replaceSubscriptionProfiles(subId: Long, profiles: List<ProfileEntity>) =
        profileDao.replaceSubscriptionProfiles(subId, profiles)

    suspend fun deleteStandaloneProfiles() =
        profileDao.deleteStandaloneProfiles()

    suspend fun updateSubscription(subscription: SubscriptionEntity) =
        subscriptionDao.update(subscription)
}
