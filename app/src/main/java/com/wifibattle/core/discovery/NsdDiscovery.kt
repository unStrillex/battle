package com.wifibattle.core.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.wifibattle.data.model.NetworkRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 基于 Android NSD (Network Service Discovery) 的 mDNS 服务发现
 *
 * Host:
 *   - 注册 _wifibattle._tcp 服务，文本记录包含房间名、房主、人数
 * Client:
 *   - 浏览 _wifibattle._tcp 服务，解析得到房间信息
 *
 * 与 UDP 广播互为补充，广播兼容性广，NSD 速度更快。
 */
class NsdDiscovery(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    private val nsdManager: NsdManager? =
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _discoveredRooms = MutableStateFlow<List<NetworkRoom>>(emptyList())
    val discoveredRooms: StateFlow<List<NetworkRoom>> = _discoveredRooms.asStateFlow()

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "NSD service registered: ${serviceInfo.serviceName}")
        }
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "NSD register failed: $errorCode")
        }
        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {}
        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.i(TAG, "NSD discovery started: $regType")
        }
        override fun onServiceFound(service: NsdServiceInfo) {
            if (service.serviceType.contains(SERVICE_TYPE)) {
                nsdManager?.resolveService(service, resolveListener)
            }
        }
        override fun onServiceLost(service: NsdServiceInfo) {
            Log.i(TAG, "NSD service lost: ${service.serviceName}")
        }
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "NSD discovery start failed: $errorCode")
        }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "NSD resolve failed: $errorCode")
        }
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            try {
                val attrs = serviceInfo.attributes
                val raw = attrs["room"]?.let { String(it) } ?: return
                val room = json.decodeFromString<NetworkRoom>(raw)
                val updated = room.copy(hostIp = serviceInfo.host.hostAddress ?: room.hostIp)
                val current = _discoveredRooms.value.toMutableList()
                val idx = current.indexOfFirst { it.id == updated.id }
                if (idx >= 0) current[idx] = updated else current.add(updated)
                _discoveredRooms.value = current
            } catch (e: Exception) {
                Log.w(TAG, "Resolve parse error", e)
            }
        }
    }

    fun register(room: NetworkRoom, port: Int) {
        try {
            val info = NsdServiceInfo().apply {
                serviceName = "WFB-${room.name}"
                serviceType = SERVICE_TYPE
                this.port = port
                setAttribute("room", json.encodeToString(room))
            }
            nsdManager?.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD register error", e)
        }
    }

    fun unregister() {
        try { nsdManager?.unregisterService(registrationListener) } catch (_: Exception) {}
    }

    fun startDiscovery() {
        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.w(TAG, "NSD start error", e)
        }
    }

    fun stopDiscovery() {
        try { nsdManager?.stopServiceDiscovery(discoveryListener) } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "NsdDiscovery"
        private const val SERVICE_TYPE = "_wifibattle._tcp."
    }
}
