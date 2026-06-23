package flare.client.app.singbox

import android.content.Context
import android.util.Log
import flare.client.app.data.SettingsManager
import flare.client.app.data.db.AppDatabase
import flare.client.app.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object SingBoxConfigChainer {

    private const val TAG = "SingBoxConfigChainer"

    fun findPrimaryProxyTag(outbounds: JSONArray): String {
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            if (type == "urltest" || type == "selector") {
                val tag = ob.optString("tag", "")
                if (tag.isNotEmpty()) return tag
            }
        }
        
        for (i in 0 until outbounds.length()) {
            val ob = outbounds.optJSONObject(i) ?: continue
            val type = ob.optString("type", "")
            val tag = ob.optString("tag", "")
            if (tag.isNotEmpty() && type != "direct" && type != "block" && type != "dns" && type != "dns-out") {
                return tag
            }
        }
        return "proxy"
    }

    fun getPrimaryOutbound(configJson: String): JSONObject? {
        try {
            val obj = JSONObject(configJson)
            val outbounds = obj.optJSONArray("outbounds") ?: return null
            val primaryTag = findPrimaryProxyTag(outbounds)
            for (i in 0 until outbounds.length()) {
                val ob = outbounds.optJSONObject(i) ?: continue
                if (ob.optString("tag") == primaryTag) {
                    return ob
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPrimaryOutbound failed: ${e.message}", e)
        }
        return null
    }

    fun generateChainedConfig(primaryConfig: String, chainedConfigs: List<String>): String {
        try {
            val obj = JSONObject(primaryConfig)
            val primaryOutbounds = obj.optJSONArray("outbounds") ?: return primaryConfig
            val primaryTag = findPrimaryProxyTag(primaryOutbounds)

            val newOutboundsList = JSONArray()

            val hop0Outbounds = mutableListOf<JSONObject>()
            for (i in 0 until primaryOutbounds.length()) {
                val ob = primaryOutbounds.optJSONObject(i) ?: continue
                hop0Outbounds.add(JSONObject(ob.toString()))
            }
            
            renameAndDetourHop(
                outbounds = hop0Outbounds,
                hopIndex = 0,
                targetMainTag = "chain_node_0",
                detourTag = null
            )
            
            for (ob in hop0Outbounds) {
                newOutboundsList.put(ob)
            }

            for (h in chainedConfigs.indices) {
                val chainedJson = chainedConfigs[h]
                val hopObj = JSONObject(chainedJson)
                val hopOutboundsArr = hopObj.optJSONArray("outbounds") ?: continue
                
                val hopOutbounds = mutableListOf<JSONObject>()
                for (i in 0 until hopOutboundsArr.length()) {
                    val ob = hopOutboundsArr.optJSONObject(i) ?: continue
                    hopOutbounds.add(JSONObject(ob.toString()))
                }
                
                val hopIndex = h + 1
                val isExitNode = (h == chainedConfigs.size - 1)
                
                val targetTag = if (isExitNode) primaryTag else "chain_node_$hopIndex"
                val detourTag = "chain_node_$h"
                
                renameAndDetourHop(
                    outbounds = hopOutbounds,
                    hopIndex = hopIndex,
                    targetMainTag = targetTag,
                    detourTag = detourTag
                )
                
                for (ob in hopOutbounds) {
                    newOutboundsList.put(ob)
                }
            }

            obj.put("outbounds", newOutboundsList)
            return obj.toString().replace("\\/", "/")
        } catch (e: Exception) {
            Log.e(TAG, "generateChainedConfig failed: ${e.message}", e)
            return primaryConfig
        }
    }

    private fun renameAndDetourHop(
        outbounds: List<JSONObject>,
        hopIndex: Int,
        targetMainTag: String,
        detourTag: String?
    ): String {
        val originalMainTag = findPrimaryProxyTag(JSONArray().apply { outbounds.forEach { put(it) } })
        
        val tagsToRename = mutableSetOf<String>()
        for (ob in outbounds) {
            val tag = ob.optString("tag")
            if (tag.isNotEmpty() && tag != "direct" && tag != "block" && tag != "dns" && tag != "dns-out") {
                tagsToRename.add(tag)
            }
        }
        
        val renamedMap = mutableMapOf<String, String>()
        for (tag in tagsToRename) {
            if (tag == originalMainTag) {
                renamedMap[tag] = targetMainTag
            } else {
                renamedMap[tag] = "${tag}_hop$hopIndex"
            }
        }
        
        val nonPhysicalTypes = setOf("direct", "block", "dns", "dns-out", "selector", "urltest")
        
        for (ob in outbounds) {
            val tag = ob.optString("tag")
            val type = ob.optString("type")
            
            if (renamedMap.containsKey(tag)) {
                ob.put("tag", renamedMap[tag])
            }
            
            if (detourTag != null && !nonPhysicalTypes.contains(type)) {
                ob.put("detour", detourTag)
            } else {
                ob.remove("detour")
            }
            
            val subOutbounds = ob.optJSONArray("outbounds")
            if (subOutbounds != null) {
                val newSubOutbounds = JSONArray()
                for (j in 0 until subOutbounds.length()) {
                    val subTag = subOutbounds.optString(j)
                    if (renamedMap.containsKey(subTag)) {
                        newSubOutbounds.put(renamedMap[subTag])
                    } else {
                        newSubOutbounds.put(subTag)
                    }
                }
                ob.put("outbounds", newSubOutbounds)
            }
        }
        
        return targetMainTag
    }

    suspend fun prepareConfigWithChaining(
        context: Context,
        baseConfigJson: String,
        settings: SettingsManager
    ): String = withContext(Dispatchers.IO) {
        val idsStr = settings.chainedProfileIdsString
        if (idsStr.isBlank()) {
            return@withContext baseConfigJson
        }
        
        val ids = idsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (ids.isEmpty()) {
            return@withContext baseConfigJson
        }
        
        try {
            val db = AppDatabase.getInstance(context)
            val repository = ProfileRepository(db.profileDao(), db.subscriptionDao())
            val profiles = repository.getProfilesByIds(ids)
            val profilesMap = profiles.associateBy { it.id }
            val orderedConfigs = ids.mapNotNull { id ->
                profilesMap[id]?.configJson
            }
            
            if (orderedConfigs.isEmpty()) {
                return@withContext baseConfigJson
            }
            
            return@withContext generateChainedConfig(baseConfigJson, orderedConfigs)
        } catch (e: Exception) {
            Log.e(TAG, "prepareConfigWithChaining failed: ${e.message}", e)
            return@withContext baseConfigJson
        }
    }
}
