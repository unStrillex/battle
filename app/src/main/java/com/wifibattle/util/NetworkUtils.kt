package com.wifibattle.util

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/**
 * 网络工具
 */
object NetworkUtils {

    /**
     * 获取本机在 WiFi 下的 IPv4 地址
     */
    fun getWifiIpAddress(context: Context): String? {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        val ipInt = wm?.connectionInfo?.ipAddress ?: 0
        if (ipInt == 0) return getLocalIpAddress()
        return String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }

    /**
     * 遍历网卡获取非回环 IPv4
     */
    fun getLocalIpAddress(): String? {
        try {
            Collections.list(NetworkInterface.getNetworkInterfaces()).forEach { intf ->
                Collections.list(intf.inetAddresses).forEach { addr ->
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(':') == false) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * 是否连接到 WiFi
     */
    fun isOnWifi(context: Context): Boolean {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        @Suppress("DEPRECATION")
        return wm?.connectionInfo?.networkId != -1
    }

    /**
     * ping 主机（简单 TCP 探活）
     * @return 延迟（毫秒），-1 表示不可达
     */
    fun pingTcp(host: String, port: Int, timeoutMs: Int = 1500): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(InetAddress.getByName(host), port), timeoutMs)
            socket.close()
            System.currentTimeMillis() - start
        } catch (_: Exception) {
            -1L
        }
    }
}
