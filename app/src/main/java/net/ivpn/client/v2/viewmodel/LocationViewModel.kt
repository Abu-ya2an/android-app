package net.ivpn.client.v2.viewmodel

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import net.ivpn.client.common.dagger.ApplicationScope
import net.ivpn.client.common.prefs.ServersRepository
import net.ivpn.client.common.prefs.Settings
import net.ivpn.client.rest.HttpClientFactory
import net.ivpn.client.rest.IVPNApi
import net.ivpn.client.rest.RequestListener
import net.ivpn.client.rest.data.model.ServerLocation
import net.ivpn.client.rest.data.proofs.LocationResponse
import net.ivpn.client.rest.requests.common.Request
import net.ivpn.client.ui.connect.ConnectionState
import net.ivpn.client.v2.map.model.Location
import net.ivpn.client.vpn.OnProtocolChangedListener
import net.ivpn.client.vpn.ProtocolController
import net.ivpn.client.vpn.controller.VpnBehaviorController
import net.ivpn.client.vpn.controller.VpnStateListener
import net.ivpn.client.vpn.controller.VpnStateListenerImpl
import org.slf4j.LoggerFactory
import javax.inject.Inject

@ApplicationScope
class LocationViewModel @Inject constructor(
        private val serversRepository: ServersRepository,
        settings: Settings,
        httpClientFactory: HttpClientFactory,
        protocolController: ProtocolController,
        vpnBehaviorController: VpnBehaviorController
) : ViewModel() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(LocationViewModel::class.java)
    }

    val dataLoading = ObservableBoolean()
    val locations = ObservableField<List<ServerLocation>?>()
    private val homeLocation = ObservableField<Location>()
    var state: ConnectionState? = null

    val ip = ObservableField<String>()
    val location = ObservableField<String>()
    val isp = ObservableField<String>()

    private var locationListeners = arrayListOf<CheckLocationListener>()

    private var request: Request<LocationResponse> = Request(settings, httpClientFactory, serversRepository, Request.Duration.SHORT)

    init {
        vpnBehaviorController.addVpnStateListener(getVpnStateListener())
        checkLocation()
        protocolController.addOnProtocolChangedListener(getOnProtocolChangeListener())
    }

    fun addLocationListener(listener: CheckLocationListener) {
        locationListeners.add(listener)
        val location = homeLocation.get()
        if (location != null && state != null && state == ConnectionState.NOT_CONNECTED) {
            listener.onSuccess(location)
        }
    }

    fun removeLocationListener(listener: CheckLocationListener) {
        locationListeners.remove(listener)
    }

    private fun checkLocation() {
        dataLoading.set(true)
        val location = homeLocation.get()
        if (location != null && state != null && state == ConnectionState.NOT_CONNECTED) {
            for (listener in locationListeners) {
                listener.onSuccess(location)
            }
        }
        request.start({ obj: IVPNApi -> obj.location }, object : RequestListener<LocationResponse?> {
            override fun onSuccess(response: LocationResponse?) {
                LOGGER.info(response.toString())
                this@LocationViewModel.onSuccess(response)
            }

            override fun onError(throwable: Throwable) {
                LOGGER.error("Error while updating location ", throwable)
                for (listener in locationListeners) {
                    listener.onError()
                }
                this@LocationViewModel.onError()
            }

            override fun onError(string: String) {
                LOGGER.error("Error while updating location ", string)
                for (listener in locationListeners) {
                    listener.onError()
                }
                this@LocationViewModel.onError()
            }
        })
    }

    private fun onSuccess(response: LocationResponse?) {
        dataLoading.set(false)
        response?.let {
            if (state != null && state == ConnectionState.NOT_CONNECTED) {
                val newLocation = Location(it.longitude.toFloat(), it.latitude.toFloat(), false, "${it.city}, ${it.country}", it.countryCode)
                homeLocation.set(newLocation)
                for (listener in locationListeners) {
                    listener.onSuccess(newLocation)
                }
            }

            ip.set(it.ipAddress)
            if (it.city != null && it.city.isNotEmpty()) {
                location.set(it.getLocation())
            } else {
                location.set(it.country)
            }
            isp.set(it.isp)
        }
    }

    private fun onError() {
        dataLoading.set(false)
    }

    private fun getOnProtocolChangeListener(): OnProtocolChangedListener {
        return OnProtocolChangedListener { locations.set(serversRepository.locations) }
    }

    private fun getVpnStateListener(): VpnStateListener {
        return object : VpnStateListenerImpl() {
            override fun onConnectionStateChanged(state: ConnectionState) {
                this@LocationViewModel.state = state
                when (state) {
                    ConnectionState.NOT_CONNECTED -> {
                        checkLocation()
                    }
                    ConnectionState.DISCONNECTING -> {
                        val location = homeLocation.get()
                        if (location != null) {
                            for (listener in locationListeners) {
                                listener.onSuccess(location)
                            }
                        }
                    }
                    ConnectionState.CONNECTED -> {
                        checkLocation()
                    }
                    else -> {
                    }
                }
            }
        }
    }

    interface CheckLocationListener {
        fun onSuccess(location: Location)

        fun onError()
    }
}