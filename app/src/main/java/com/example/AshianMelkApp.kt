package com.example

import android.app.Application
import com.example.core.network.AndroidNetworkMonitor
import com.example.diagnostics.CrashDiagnostics
import com.example.core.network.NetworkMonitor
import com.example.data.local.AshianMelkDatabase
import com.example.data.repository.*
import com.example.domain.repository.*
import com.example.network.ApiClient
import com.example.network.AshianMelkApiService
import com.example.security.EncryptedDraftStorage
import com.example.security.EncryptedTokenStorage
import com.example.security.KeyStoreManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AshianMelkApp : Application() {

    lateinit var keyStoreManager: KeyStoreManager private set
    lateinit var tokenStorage: EncryptedTokenStorage private set
    lateinit var encryptedDraftStorage: EncryptedDraftStorage private set
    lateinit var database: AshianMelkDatabase private set
    lateinit var apiService: AshianMelkApiService private set
    lateinit var networkMonitor: NetworkMonitor private set
    lateinit var crashDiagnostics: CrashDiagnostics private set

    // Repositories
    lateinit var authRepository: AuthRepository private set
    lateinit var propertyRepository: PropertyRepository private set
    lateinit var demandRepository: DemandRepository private set
    lateinit var taskRepository: TaskRepository private set
    lateinit var notificationRepository: NotificationRepository private set
    lateinit var dashboardRepository: DashboardRepository private set

    // Session revocation event for clean logout / relogin navigation
    private val _sessionRevokedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionRevokedEvents: SharedFlow<Unit> = _sessionRevokedEvents.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this

        crashDiagnostics = CrashDiagnostics(this).also { it.install() }
        keyStoreManager = KeyStoreManager()
        tokenStorage = EncryptedTokenStorage(this, keyStoreManager)
        encryptedDraftStorage = EncryptedDraftStorage(this, keyStoreManager)
        database = AshianMelkDatabase.getInstance(this)
        networkMonitor = AndroidNetworkMonitor(this)

        apiService = ApiClient.createService(
            tokenStorage = tokenStorage,
            onSessionRevoked = {
                _sessionRevokedEvents.tryEmit(Unit)
            }
        )

        authRepository = AuthRepositoryImpl(apiService, tokenStorage)
        propertyRepository = PropertyRepositoryImpl(this, apiService, database.propertyDao(), encryptedDraftStorage)
        demandRepository = DemandRepositoryImpl(apiService, database.demandDao())
        taskRepository = TaskRepositoryImpl(apiService, database.taskDao())
        notificationRepository = NotificationRepositoryImpl(apiService, database.notificationDao())
        dashboardRepository = DashboardRepositoryImpl(
            apiService,
            propertyRepository,
            demandRepository,
            taskRepository,
            notificationRepository
        )
    }

    companion object {
        lateinit var instance: AshianMelkApp private set
    }
}
