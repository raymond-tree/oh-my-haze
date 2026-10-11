package com.ohmyhaze.debug

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import com.ohmyhaze.MainActivity
import com.ohmyhaze.OhMyHazeApplication
import com.ohmyhaze.R
import com.ohmyhaze.notifications.AirQualityNotifier
import kotlinx.coroutines.withTimeoutOrNull

private const val REQUIRED_TWO_FINGER_TAPS = 3
private const val TAP_MAX_DURATION_MILLIS = 900L
private const val TAP_SEQUENCE_GAP_MILLIS = 3_500L
private const val TEST_NOTIFICATION_ID = 0x48415A45

/** Debug builds only: three two-finger taps on the AQI poster post a test alert. */
@Composable
fun Modifier.withHiddenTestNotificationGesture(onTrigger: () -> Unit): Modifier {
    val latestOnTrigger = rememberUpdatedState(onTrigger)
    val touchSlop = LocalViewConfiguration.current.touchSlop
    return pointerInput(touchSlop) {
        var validTapCount = 0
        var lastTapAt = Long.MIN_VALUE

        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val startedAt = firstDown.uptimeMillis
            var lastEventAt = startedAt
            val downPositions = mutableMapOf(firstDown.id to firstDown.position)
            var reachedTwoFingers = false
            var valid = true
            var allPointersUp = false

            withTimeoutOrNull(TAP_MAX_DURATION_MILLIS) {
                while (!allPointersUp && valid) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    lastEventAt = event.changes.maxOfOrNull { it.uptimeMillis } ?: lastEventAt

                    event.changes.forEach { change ->
                        if ((change.pressed || change.previousPressed) && change.id !in downPositions) {
                            downPositions[change.id] = change.position
                        }
                        val initialPosition = downPositions[change.id]
                        if ((change.pressed || change.previousPressed) && initialPosition != null &&
                            (change.position - initialPosition).getDistance() > touchSlop * 2f
                        ) {
                            valid = false
                        }
                    }

                    val pressedCount = event.changes.count { it.pressed }
                    if (pressedCount > 2 || downPositions.size > 2) valid = false
                    if (pressedCount == 2) reachedTwoFingers = true
                    if (event.changes.any { it.previousPressed && !it.pressed } && !reachedTwoFingers) {
                        valid = false
                    }
                    allPointersUp = pressedCount == 0
                }
            }

            val completedTwoFingerTap = valid && reachedTwoFingers && downPositions.size == 2 &&
                allPointersUp && lastEventAt - startedAt <= TAP_MAX_DURATION_MILLIS
            if (completedTwoFingerTap) {
                if (lastTapAt == Long.MIN_VALUE || lastEventAt - lastTapAt > TAP_SEQUENCE_GAP_MILLIS) {
                    validTapCount = 0
                }
                validTapCount += 1
                lastTapAt = lastEventAt
                if (validTapCount == REQUIRED_TWO_FINGER_TAPS) {
                    validTapCount = 0
                    lastTapAt = Long.MIN_VALUE
                    latestOnTrigger.value.invoke()
                }
            } else {
                validTapCount = 0
                lastTapAt = Long.MIN_VALUE
            }
        }
    }
}

/** Posts through the same channel/permission path as AQI alerts without touching AQI state. */
fun postDebugTestNotification(context: Context, locationLabel: String): Boolean {
    val appContext = context.applicationContext
    val app = appContext as? OhMyHazeApplication ?: return false
    app.airQualityNotifier.ensureChannel()
    if (!app.airQualityNotifier.isAvailable()) return false

    val manager = appContext.getSystemService(NotificationManager::class.java)
    val tapIntent = PendingIntent.getActivity(
        appContext,
        TEST_NOTIFICATION_ID,
        Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(appContext, AirQualityNotifier.CHANNEL_ID)
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(appContext)
    }
    val notification = builder
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("TEST: Unhealthy for Sensitive Groups air quality")
        .setContentText("TEST · AQI 125 at $locationLabel")
        .setContentIntent(tapIntent)
        .setAutoCancel(true)
        .build()

    return runCatching { manager.notify(TEST_NOTIFICATION_ID, notification) }.isSuccess
}
