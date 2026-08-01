package bo.org.siafco.app.core.debug

import android.util.Log
object MobileDiagnostics {
    private const val TAG = "SIAFCO-Mobile"

    fun home(layer: String, message: String) {
        runCatching {
            Log.i(TAG, "[$layer] $message")
        }.getOrElse {
            println("$TAG [$layer] $message")
        }
    }
}
