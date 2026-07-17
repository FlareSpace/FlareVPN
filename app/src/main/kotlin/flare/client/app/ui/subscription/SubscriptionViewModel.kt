package flare.client.app.ui.subscription

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import flare.client.app.data.SettingsManager
import flare.client.app.data.api.FlareBackendApi
import flare.client.app.data.api.SubscriptionInfoResponse
import flare.client.app.data.auth.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import flare.client.app.data.api.CreatePaymentResponse

import flare.client.app.ui.i18n.I18n

sealed class SubscriptionState {
    object Loading : SubscriptionState()
    data class Success(val info: SubscriptionInfoResponse) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
    object NotLoggedIn : SubscriptionState()
}

sealed class PaymentState {
    object Idle : PaymentState()
    object Loading : PaymentState()
    data class Ready(val txUuid: String, val addressIn: String, val coinAmount: Double, val usdAmount: Double) : PaymentState()
    object Completed : PaymentState()
    data class Error(val message: String) : PaymentState()
}

class SubscriptionViewModel(
    application: Application,
    private val authManager: AuthManager,
    private val settingsManager: SettingsManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<SubscriptionState>(SubscriptionState.NotLoggedIn)
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    init {
        refresh()
    }

    fun refresh(force: Boolean = false) {
        if (!authManager.isLoggedIn()) {
            _state.value = SubscriptionState.NotLoggedIn
            return
        }

        val token = authManager.getToken()
        if (token == null) {
            _state.value = SubscriptionState.NotLoggedIn
            return
        }

        viewModelScope.launch {
            if (force || _state.value !is SubscriptionState.Success) {
                _state.value = SubscriptionState.Loading
            } else {
                _isRefreshing.value = true
            }
            try {
                val info = FlareBackendApi.getSubInfo(token)
                if (info != null) {
                    if (info.has_telegram) {
                        authManager.clearAnonymousFlag()
                    }
                    _state.value = SubscriptionState.Success(info)
                } else {
                    if (_state.value !is SubscriptionState.Success) {
                        _state.value = SubscriptionState.Error(I18n.strings.sub_err_fetch)
                    } else {
                        _actionError.value = I18n.strings.sub_err_fetch
                    }
                }
            } catch (e: Exception) {
                if (_state.value !is SubscriptionState.Success) {
                    _state.value = SubscriptionState.Error(e.message ?: I18n.strings.sub_err_unknown)
                } else {
                    _actionError.value = e.message ?: I18n.strings.sub_err_unknown
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun createKey(onSuccess: (String) -> Unit) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val response = FlareBackendApi.createKey(token)
                if (response != null && response.status == "success" && response.sub_link.isNotEmpty()) {
                    onSuccess(response.sub_link)
                    refresh()
                } else {
                    _actionError.value = I18n.strings.sub_err_create_key
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_create_key_fail
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun addDevice(subId: Int? = null, onSuccess: (String) -> Unit) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val hwid = settingsManager.getHardwareId()
                val name = "${Build.MANUFACTURER} ${Build.MODEL}"
                val response = FlareBackendApi.addDevice(token, hwid, name, subId)
                
                if (response != null && response.sub_link.isNotEmpty()) {
                    onSuccess(response.sub_link)
                    refresh() 
                } else {
                    _actionError.value = I18n.strings.sub_err_link
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_add_device
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun removeDevice(hwid: String) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val success = FlareBackendApi.removeDevice(token, hwid)
                if (success) {
                    refresh()
                } else {
                    _actionError.value = I18n.strings.sub_err_remove_device
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_remove_device_unknown
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun revokeKey(subId: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val response = FlareBackendApi.revokeKey(token, subId)
                if (response != null && response.status == "success") {
                    refresh()
                } else {
                    _actionError.value = I18n.strings.sub_err_revoke_key
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_revoke_key
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun renameKey(subId: Int, newName: String) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val response = FlareBackendApi.renameKey(token, subId, newName)
                if (response != null && response.status == "success") {
                    refresh()
                } else {
                    _actionError.value = I18n.strings.sub_err_unknown 
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_unknown
            } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun deleteKey(subId: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _actionError.value = I18n.strings.sub_err_unauth
            return
        }

        viewModelScope.launch {
            _isActionLoading.value = true
            _actionError.value = null
            try {
                val response = FlareBackendApi.deleteKey(token, subId)
                if (response != null && response.status == "success") {
                    refresh()
                } else {
                    _actionError.value = I18n.strings.sub_err_unknown 
                }
            } catch (e: Exception) {
                _actionError.value = e.message ?: I18n.strings.sub_err_unknown
            } finally {
                _isActionLoading.value = false
            }
        }
    }



    fun resetPaymentState() {
        _paymentState.value = PaymentState.Idle
    }

    fun createTopupPayment(coin: String, qty: Int) {
        val token = authManager.getToken()
        if (token == null) {
            _paymentState.value = PaymentState.Error(I18n.strings.sub_err_unauth)
            return
        }
        viewModelScope.launch {
            _paymentState.value = PaymentState.Loading
            val response = FlareBackendApi.createPayment(token, type = "topup", coin = coin, qty = qty)
            response.onSuccess {
                _paymentState.value = PaymentState.Ready(it.tx_uuid, it.address_in, it.coin_amount, it.usd_amount)
                pollPaymentStatus(it.tx_uuid)
            }.onFailure {
                _paymentState.value = PaymentState.Error(it.message ?: "Payment error")
            }
        }
    }

    private fun pollPaymentStatus(txUuid: String) {
        val token = authManager.getToken() ?: return
        viewModelScope.launch {
            while (_paymentState.value is PaymentState.Ready) {
                delay(5000)
                val response = FlareBackendApi.checkPaymentStatus(token, txUuid)
                response.onSuccess {
                    if (it.status == "completed") {
                        _paymentState.value = PaymentState.Completed
                        refresh(force = true)
                    }
                }
            }
        }
    }
}
