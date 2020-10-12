package net.ivpn.client.ui.network

import android.content.Context
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.ObservableField
import net.ivpn.client.IVPNApplication
import net.ivpn.client.R
import net.ivpn.client.vpn.local.NetworkController
import net.ivpn.client.vpn.model.NetworkState
import net.ivpn.client.vpn.model.WifiItem
import javax.inject.Inject

open class NetworkItemViewModel @Inject constructor(
        private val networkController: NetworkController
) {

    var wifiItem = ObservableField<WifiItem>()
    var currentState: NetworkState? = null

    var defaultState: NetworkState? = null
    var selectedState: NetworkState? = null

    var networkStateListener = RadioGroup.OnCheckedChangeListener {
        _: RadioGroup?, checkedId: Int ->
        onCheckedChanged(checkedId)
    }

    private lateinit var context: Context

    fun setContext(context: Context) {
        this.context = context
    }

    fun setWifiItem(wifiItem: WifiItem) {
        this.wifiItem.set(wifiItem)
        currentState = wifiItem.networkState.get()
        selectedState = wifiItem.networkState.get()
    }

    fun getCurrentStateColor(): Int {
        return if (currentState == NetworkState.DEFAULT) {
            println("Return default color for ${wifiItem.get()} default = ${defaultState}")
            getColor(defaultState)
        } else {
            println("Return current color for ${wifiItem.get()} currentState = ${currentState}")
            getColor(currentState)
        }
    }

    fun getCurrentStateText(): String? {
        return if (currentState == NetworkState.DEFAULT) {
            defaultState?.let {
                context.getString(it.textRes)
            }
        } else {
            currentState?.let {
                context.getString(it.textRes)
            }
        }
    }

    fun getDefaultText(): String? {
        return defaultState?.let {
            context.getString(it.textRes)
        }
    }

    fun getColor(state: NetworkState?): Int {
        return when (state) {
            NetworkState.TRUSTED -> {
                ResourcesCompat.getColor(context.resources, R.color.color_trusted_text, null)
            }
            NetworkState.UNTRUSTED -> {
                ResourcesCompat.getColor(context.resources, R.color.color_untrusted_text, null)
            }
            NetworkState.NONE -> {
                ResourcesCompat.getColor(context.resources, R.color.color_none_text, null)
            }
            NetworkState.DEFAULT -> {
                ResourcesCompat.getColor(context.resources, R.color.color_default_text, null)
            }
            else -> {
                ResourcesCompat.getColor(context.resources, R.color.color_none_text, null)
            }
        }
    }

    private fun onCheckedChanged(checkedId: Int) {
        val networkState = NetworkState.getById(checkedId)
        if (networkState == this.selectedState) {
            return
        }

        this.selectedState = networkState
    }

    open fun applyState() {
        wifiItem.get()?.let {
            networkController.changeMarkFor(it.ssid, currentState, selectedState)
            it.networkState.set(selectedState)
        }

        currentState = selectedState
    }

    fun setDefaultStateV(defaultState: NetworkState) {
        this.defaultState = defaultState
    }

    fun setCurrentStateV(currentState: NetworkState) {
        this.currentState = currentState
        selectedState = currentState
    }

    val title: String
        get() = wifiItem.get()!!.title

}