package com.wifibattle.di

import android.content.Context
import com.wifibattle.core.discovery.NsdDiscovery
import com.wifibattle.core.discovery.RoomDiscovery
import com.wifibattle.core.network.NetworkTransport
import com.wifibattle.core.player.DefaultPlayerManager
import com.wifibattle.core.player.NetworkPlayerManager
import com.wifibattle.core.room.DefaultRoomManager
import com.wifibattle.core.room.NetworkRoomManager
import com.wifibattle.core.sync.DefaultMatchManager
import com.wifibattle.core.sync.MatchManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides @Singleton
    fun provideNetworkTransport(): NetworkTransport = NetworkTransport()

    @Provides @Singleton
    fun provideRoomDiscovery(): RoomDiscovery = RoomDiscovery()

    @Provides @Singleton
    fun provideNsdDiscovery(@ApplicationContext context: Context): NsdDiscovery =
        NsdDiscovery(context)

    @Provides @Singleton
    fun providePlayerManager(): DefaultPlayerManager = DefaultPlayerManager()

    @Provides
    fun providePlayerManagerInterface(impl: DefaultPlayerManager): NetworkPlayerManager = impl

    @Provides @Singleton
    fun provideRoomManager(playerManager: NetworkPlayerManager): DefaultRoomManager =
        DefaultRoomManager(playerManager)

    @Provides
    fun provideRoomManagerInterface(impl: DefaultRoomManager): NetworkRoomManager = impl

    @Provides @Singleton
    fun provideMatchManager(transport: NetworkTransport): DefaultMatchManager =
        DefaultMatchManager(transport)

    @Provides
    fun provideMatchManagerInterface(impl: DefaultMatchManager): MatchManager = impl
}
