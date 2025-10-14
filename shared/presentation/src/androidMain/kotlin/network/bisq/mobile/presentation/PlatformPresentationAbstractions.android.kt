package network.bisq.mobile.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.view.View
import android.os.Build

import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.presentation.ui.helpers.AndroidCurrentTimeProvider
import network.bisq.mobile.presentation.ui.helpers.TimeProvider
import kotlin.math.roundToInt

actual fun getPlatformCurrentTimeProvider(): TimeProvider = AndroidCurrentTimeProvider()

actual fun moveAppToBackground(view: Any?) {
    val activity: Activity? = findActivity(view)
    if (activity != null) {
        activity.moveTaskToBack(true)
    } else if (view is Context) {
        // fallback: launch Home intent
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        view.startActivity(homeIntent)
    } else {
        getLogger("moveAppToBackground").e("Cannot move app to background: unknown type $view")
    }
}

fun findActivity(view: Any?): Activity? {
    return when (view) {
        is Activity -> view
        is View -> view.context.findActivity()
        is Context -> view.findActivity()
        else -> null
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

actual fun getScreenWidthDp(): Int {
    val displayMetrics = Resources.getSystem().displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).roundToInt()
}

actual fun isAffectedBottomSheetDevice(): Boolean {
    // Motorola devices on Android 14+ have been observed to crash when dialog-based sheets update
    // window flags. Add other particular devices if needed.
    // when we do compose upgrade to next stable version we might getaway removing this
    return Build.VERSION.SDK_INT >= 34 &&
            Build.MANUFACTURER.equals("motorola", ignoreCase = true)
}
