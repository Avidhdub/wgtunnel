package com.zaneschepke.wireguardautotunnel.module

import android.content.Context
import com.zaneschepke.wireguardautotunnel.data.repository.AppDataRepository
import com.zaneschepke.wireguardautotunnel.service.foreground.ServiceManager
import com.zaneschepke.wireguardautotunnel.service.tunnel.VpnService
import com.zaneschepke.wireguardautotunnel.service.tunnel.WireGuardTunnel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.amnezia.awg.backend.AwgQuickBackend
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.util.RootShell
import org.amnezia.awg.util.ToolsInstaller
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class TunnelModule {
    @Provides
    @Singleton
    fun provideRootShell(@ApplicationContext context: Context): RootShell {
        return RootShell(context)
    }

    @Provides
    @Singleton
    @Userspace
    fun provideUserspaceBackend(@ApplicationContext context: Context): Backend {
        return GoBackend(context)
    }

    @Provides
    @Singleton
    @Kernel
    fun provideKernelBackend(@ApplicationContext context: Context, rootShell: RootShell): Backend {
        return AwgQuickBackend(context, rootShell, ToolsInstaller(context, rootShell))
    }

    @Provides
    @Singleton
    fun provideVpnService(
        @Userspace userspaceBackend: Backend,
        @Kernel kernelBackend: Backend,
        appDataRepository: AppDataRepository
    ): VpnService {
        return WireGuardTunnel(userspaceBackend, kernelBackend, appDataRepository)
    }

    @Provides
    @Singleton
    fun provideServiceManager(appDataRepository: AppDataRepository): ServiceManager {
        return ServiceManager(appDataRepository)
    }
}
