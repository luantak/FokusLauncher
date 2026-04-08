package com.lu4p.fokuslauncher.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/** Registers [receiver] with [ContextCompat.RECEIVER_NOT_EXPORTED] on all API levels. */
fun Context.registerBroadcastReceiverNotExported(
        receiver: BroadcastReceiver,
        filter: IntentFilter,
) {
    ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
}

/** For sticky broadcasts such as [Intent.ACTION_BATTERY_CHANGED] with a null receiver. */
fun Context.registerStickyBroadcastReceiverNotExported(filter: IntentFilter): Intent? =
        ContextCompat.registerReceiver(
                this,
                null,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
        )
