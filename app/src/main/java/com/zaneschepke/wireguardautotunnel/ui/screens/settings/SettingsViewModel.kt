package com.zaneschepke.wireguardautotunnel.ui.screens.settings

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.util.RootShell
import com.zaneschepke.wireguardautotunnel.data.TunnelConfigDao
import com.zaneschepke.wireguardautotunnel.data.datastore.DataStoreManager
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import com.zaneschepke.wireguardautotunnel.data.repository.SettingsRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.util.WgTunnelException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val application: Application,
    private val tunnelRepo: TunnelConfigDao,
    private val settingsRepository: SettingsRepository,
    private val dataStoreManager: DataStoreManager,
    private val rootShell: RootShell,
    private val vpnService: VpnService
) : ViewModel() {

    val uiState = combine(
        settingsRepository.getSettings(),
        tunnelRepo.getAllFlow(),
        vpnService.state,
        dataStoreManager.locationDisclosureFlow,
    ){ settings, tunnels, tunnelState, locationDisclosure ->
        SettingsUiState(settings, tunnels, tunnelState, locationDisclosure ?: false, false)
    }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000L), SettingsUiState())

    fun onSaveTrustedSSID(ssid: String) {
        val trimmed = ssid.trim()
        if (!uiState.value.settings.trustedNetworkSSIDs.contains(trimmed)) {
            uiState.value.settings.trustedNetworkSSIDs.add(trimmed)
            saveSettings(uiState.value.settings)
        } else {
            throw WgTunnelException("SSID already exists.")
        }
    }

    suspend fun isLocationDisclosureShown() : Boolean {
        return dataStoreManager.getFromStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN) ?: false
    }

    fun setLocationDisclosureShown() = viewModelScope.launch {
            dataStoreManager.saveToDataStore(DataStoreManager.LOCATION_DISCLOSURE_SHOWN, true)
    }

    fun onToggleTunnelOnMobileData() {
        saveSettings(
            uiState.value.settings.copy(
                isTunnelOnMobileDataEnabled = !uiState.value.settings.isTunnelOnMobileDataEnabled
            )
        )
    }

    fun onDeleteTrustedSSID(ssid: String) {
        uiState.value.settings.trustedNetworkSSIDs.remove(ssid)
        saveSettings(uiState.value.settings)
    }

    private suspend fun getDefaultTunnelOrFirst() : String {
        return uiState.value.settings.defaultTunnel ?: tunnelRepo.getAll().first().wgQuick
    }

    fun toggleAutoTunnel() = viewModelScope.launch {
        val defaultTunnel = getDefaultTunnelOrFirst()
        if (uiState.value.settings.isAutoTunnelEnabled) {
            ServiceManager.stopWatcherService(application)
        } else {
            ServiceManager.startWatcherService(application, defaultTunnel)
        }
        saveSettings(
            uiState.value.settings.copy(
                isAutoTunnelEnabled = uiState.value.settings.isAutoTunnelEnabled,
                defaultTunnel = defaultTunnel
            )
        )
    }

    suspend fun onToggleAlwaysOnVPN() {
        val updatedSettings =
            uiState.value.settings.copy(
                isAlwaysOnVpnEnabled = !uiState.value.settings.isAlwaysOnVpnEnabled,
                defaultTunnel = getDefaultTunnelOrFirst()
            )
        saveSettings(updatedSettings)
    }

    private fun saveSettings(settings: Settings) = viewModelScope.launch {
        settingsRepository.save(settings)
    }

    fun onToggleTunnelOnEthernet() {
        saveSettings(uiState.value.settings.copy(
            isTunnelOnEthernetEnabled = !uiState.value.settings.isTunnelOnEthernetEnabled
        ))
    }

    private fun isLocationServicesEnabled(): Boolean {
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isLocationServicesNeeded(): Boolean {
        return (!isLocationServicesEnabled() && Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
    }

    fun onToggleShortcutsEnabled() {
        saveSettings(
            uiState.value.settings.copy(
                isShortcutsEnabled = !uiState.value.settings.isShortcutsEnabled
            )
        )
    }

    fun onToggleBatterySaver() {
        saveSettings(
            uiState.value.settings.copy(
                isBatterySaverEnabled = !uiState.value.settings.isBatterySaverEnabled
            )
        )
    }

    private fun saveKernelMode(on: Boolean) {
        saveSettings(
            uiState.value.settings.copy(
                isKernelEnabled = on
            )
        )
    }

    fun onToggleTunnelOnWifi() {
        saveSettings(
            uiState.value.settings.copy(
                isTunnelOnWifiEnabled = !uiState.value.settings.isTunnelOnWifiEnabled
            )
        )
    }

    fun onToggleKernelMode() = viewModelScope.launch {
        if (!uiState.value.settings.isKernelEnabled) {
            try {
                rootShell.start()
                Timber.d("Root shell accepted!")
                saveKernelMode(on = true)
            } catch (e: RootShell.RootShellException) {
                saveKernelMode(on = false)
                throw WgTunnelException("Root shell denied!")
            }
        } else {
            saveKernelMode(on = false)
        }
    }
}
