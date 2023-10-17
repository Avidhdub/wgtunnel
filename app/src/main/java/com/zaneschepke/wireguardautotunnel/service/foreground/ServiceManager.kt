package com.zaneschepke.wireguardautotunnel.service.foreground

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import com.zaneschepke.wireguardautotunnel.R
import timber.log.Timber

object ServiceManager {
    @Suppress("DEPRECATION")
    private // Deprecated for third party Services.
    fun <T> Context.isServiceRunning(service: Class<T>) =
        (getSystemService(ACTIVITY_SERVICE) as ActivityManager)
            .getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == service.name }

    fun <T : Service> getServiceState(context: Context, cls : Class<T>): ServiceState {
        val isServiceRunning = context.isServiceRunning(cls)
        return if(isServiceRunning) ServiceState.STARTED else ServiceState.STOPPED
    }

    private fun <T : Service> actionOnService(action: Action, context: Context, cls : Class<T>, extras : Map<String,String>? = null) {
        if (getServiceState(context, cls) == ServiceState.STOPPED && action == Action.STOP) return
        if (getServiceState(context, cls) == ServiceState.STARTED && action == Action.START) return
        val intent = Intent(context, cls).also {
            it.action = action.name
            extras?.forEach {(k, v) ->
                it.putExtra(k, v)
            }
        }
        intent.component?.javaClass
        try {
            when(action) {
                Action.START_FOREGROUND -> {
                    context.startForegroundService(intent)
                }
                Action.START -> {
                    context.startService(intent)
                }
                Action.STOP -> context.startService(intent)
            }
        } catch (e : Exception) {
            Timber.e(e.message)
        }
    }

    fun startVpnService(context : Context, tunnelConfig : String) {
        actionOnService(
            Action.START,
            context,
            WireGuardTunnelService::class.java,
            mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig))
    }
    fun stopVpnService(context : Context) {
        actionOnService(
            Action.STOP,
            context,
            WireGuardTunnelService::class.java
        )
    }

    fun startVpnServiceForeground(context : Context, tunnelConfig : String) {
        actionOnService(
            Action.START_FOREGROUND,
            context,
            WireGuardTunnelService::class.java,
            mapOf(context.getString(R.string.tunnel_extras_key) to tunnelConfig))
    }

    private fun startWatcherServiceForeground(context : Context, tunnelConfig : String) {
        actionOnService(
            Action.START, context,
            WireGuardConnectivityWatcherService::class.java, mapOf(context.
            getString(R.string.tunnel_extras_key) to
                    tunnelConfig))
    }

    fun startWatcherService(context : Context, tunnelConfig : String) {
        actionOnService(
            Action.START, context,
            WireGuardConnectivityWatcherService::class.java, mapOf(context.
            getString(R.string.tunnel_extras_key) to
                    tunnelConfig))
    }

    fun stopWatcherService(context : Context) {
        actionOnService(
            Action.STOP, context,
            WireGuardConnectivityWatcherService::class.java)
    }

    fun toggleWatcherService(context: Context, tunnelConfig : String) {
        when(getServiceState( context,
            WireGuardConnectivityWatcherService::class.java,)) {
            ServiceState.STARTED -> stopWatcherService(context)
            ServiceState.STOPPED -> startWatcherService(context, tunnelConfig)
        }
    }

    fun toggleWatcherServiceForeground(context: Context, tunnelConfig : String) {
        when(getServiceState( context,
            WireGuardConnectivityWatcherService::class.java,)) {
            ServiceState.STARTED -> stopWatcherService(context)
            ServiceState.STOPPED -> startWatcherServiceForeground(context, tunnelConfig)
        }
    }
}