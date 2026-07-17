package flare.client.app.data.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

object FlareBackendApi {
    const val BOT_USERNAME = "flarevbot" 
    private const val BASE_URL = "https://api.flarev.net/api" 
    private val client = OkHttpClient()
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    suspend fun initAuth(): AuthInitResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/auth/init")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, AuthInitResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun checkAuthStatus(uuid: String, token: String? = null): AuthStatusResponse? = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url("$BASE_URL/auth/status?uuid=$uuid")
            .get()
            
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        
        val request = requestBuilder.build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, AuthStatusResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun getSubInfo(token: String): SubscriptionInfoResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/info")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, SubscriptionInfoResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun addDevice(token: String, hwid: String, name: String, subId: Int? = null): AddDeviceResponse? = withContext(Dispatchers.IO) {
        val reqBody = AddDeviceRequest(hwid, name, subId)
        val request = Request.Builder()
            .url("$BASE_URL/sub/device")
            .header("Authorization", "Bearer $token")
            .header("x-ver-os", android.os.Build.VERSION.RELEASE)
            .header("User-Agent", "Flare/1.3.5")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, AddDeviceResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun revokeKey(token: String, subId: Int): BaseResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/$subId/revoke")
            .header("Authorization", "Bearer $token")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, BaseResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun createKey(token: String): CreateKeyResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/create")
            .header("Authorization", "Bearer $token")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, CreateKeyResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun createFreeKeyAuthorized(token: String): Result<CreateKeyResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/free")
            .header("Authorization", "Bearer $token")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, CreateKeyResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun createFreeKeyAnonymous(hwid: String): Result<CreateKeyResponse> = withContext(Dispatchers.IO) {
        val reqBody = AnonymousFreeRequest(hwid)
        val request = Request.Builder()
            .url("$BASE_URL/sub/free/anonymous")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, CreateKeyResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun removeDevice(token: String, hwid: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/device/$hwid")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun renameKey(token: String, subId: Int, newName: String): BaseResponse? = withContext(Dispatchers.IO) {
        val reqBody = RenameKeyRequest(subId, newName)
        val request = Request.Builder()
            .url("$BASE_URL/sub/rename")
            .header("Authorization", "Bearer $token")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, BaseResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    suspend fun deleteKey(token: String, subId: Int): BaseResponse? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/sub/$subId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body.string().let {
                    return@withContext gson.fromJson(it, BaseResponse::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }
    suspend fun createAnonymousAccount(): Result<AnonymousCreateResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/auth/anonymous/create")
            .post("{}".toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, AnonymousCreateResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun loginAnonymous(authKey: String): Result<AuthStatusResponse> = withContext(Dispatchers.IO) {
        val reqBody = AnonymousLoginRequest(authKey)
        val request = Request.Builder()
            .url("$BASE_URL/auth/anonymous/login")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, AuthStatusResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun createPayment(token: String, type: String, coin: String, qty: Int? = null, plan: String? = null): Result<CreatePaymentResponse> = withContext(Dispatchers.IO) {
        val reqBody = CreatePaymentRequest(type, coin, qty, plan)
        val request = Request.Builder()
            .url("$BASE_URL/payment/cryptapi/create")
            .header("Authorization", "Bearer $token")
            .post(gson.toJson(reqBody).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, CreatePaymentResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    suspend fun checkPaymentStatus(token: String, txUuid: String): Result<PaymentStatusResponse> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/payment/cryptapi/status/$txUuid")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.string().let {
                        return@withContext Result.success(gson.fromJson(it, PaymentStatusResponse::class.java))
                    }
                } else {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
}

data class AuthInitResponse(val uuid: String, val bot_url: String)
data class AuthStatusResponse(val status: String, val token: String?)
data class AnonymousFreeRequest(val hwid: String)
data class AnonymousCreateResponse(val auth_key: String, val token: String)
data class AnonymousLoginRequest(val auth_key: String)

data class CreatePaymentRequest(val type: String, val coin: String, val qty: Int?, val plan: String?)
data class CreatePaymentResponse(val tx_uuid: String, val address_in: String, val coin_amount: Double, val usd_amount: Double)
data class PaymentStatusResponse(val tx_uuid: String, val status: String, val created_at: String?, val completed_at: String?)
