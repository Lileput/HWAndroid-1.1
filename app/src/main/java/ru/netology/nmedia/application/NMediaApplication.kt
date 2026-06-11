package ru.netology.nmedia.application

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import dagger.hilt.android.HiltAndroidApp
import ru.netology.nmedia.util.MapKitInit

@HiltAndroidApp
class NMediaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MapKitInit.setApiKeyOnce()
    }

    override fun registerReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        }
        return super.registerReceiver(receiver, filter)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        flags: Int,
    ): Intent? {
        return super.registerReceiver(receiver, filter, ensureReceiverExportFlag(flags))
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
    ): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return super.registerReceiver(
                receiver,
                filter,
                broadcastPermission,
                scheduler,
                Context.RECEIVER_EXPORTED,
            )
        }
        return super.registerReceiver(receiver, filter, broadcastPermission, scheduler)
    }

    override fun registerReceiver(
        receiver: BroadcastReceiver?,
        filter: IntentFilter?,
        broadcastPermission: String?,
        scheduler: Handler?,
        flags: Int,
    ): Intent? {
        return super.registerReceiver(
            receiver,
            filter,
            broadcastPermission,
            scheduler,
            ensureReceiverExportFlag(flags),
        )
    }

    private fun ensureReceiverExportFlag(flags: Int): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return flags
        val hasExportFlag = (flags and Context.RECEIVER_EXPORTED) != 0 ||
            (flags and Context.RECEIVER_NOT_EXPORTED) != 0
        return if (hasExportFlag) flags else flags or Context.RECEIVER_EXPORTED
    }
}
