package com.example.tongji.auth

import android.content.Context

class AuthRecoveryManager(private val context: Context) {

    private val tongjiCoordinator = TongjiAuthCoordinator(context)

    suspend fun attemptSilentRenew(): Boolean {
        return try {
            tongjiCoordinator.startFreshInteractiveLogin().isSuccess
        } catch (_: Exception) {
            false
        }
    }

    suspend fun attemptPasswordRelogin(username: String, password: String): Boolean {
        return try {
            tongjiCoordinator.startFreshInteractiveLogin().isSuccess
        } catch (_: Exception) {
            false
        }
    }

    fun destroy() {
        tongjiCoordinator.destroy()
    }
}
