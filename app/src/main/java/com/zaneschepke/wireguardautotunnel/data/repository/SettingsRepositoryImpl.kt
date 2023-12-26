package com.zaneschepke.wireguardautotunnel.data.repository

import com.zaneschepke.wireguardautotunnel.data.SettingsDao
import com.zaneschepke.wireguardautotunnel.data.model.Settings
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(private val settingsDoa: SettingsDao) : SettingsRepository {

    override suspend fun save(settings: Settings) {
        settingsDoa.save(settings)
    }

    override fun getSettings(): Flow<Settings> {
        return settingsDoa.getSettingsFlow()
    }

    override suspend fun getAll(): List<Settings> {
        return settingsDoa.getAll()
    }
}