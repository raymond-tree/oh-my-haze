package com.ohmyhaze.debug

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Release builds contain neither the hidden gesture nor the debug test notification. */
@Composable
fun Modifier.withHiddenTestNotificationGesture(onTrigger: () -> Unit): Modifier = this

fun postDebugTestNotification(context: Context, locationLabel: String): Boolean = false
