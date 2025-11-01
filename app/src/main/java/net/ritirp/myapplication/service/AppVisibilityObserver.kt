package net.ritirp.myapplication.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * 앱 포어그라운드/백그라운드 상태 관찰자
 * ProcessLifecycleOwner를 사용하여 전체 앱의 가시성 추적
 */
class AppVisibilityObserver(
    private val onForeground: () -> Unit,
    private val onBackground: () -> Unit,
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppVisibilityObserver"
    }

    fun observe() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d(TAG, "Observer registered")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "📱 App entered FOREGROUND")
        onForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "🏠 App entered BACKGROUND")
        onBackground()
    }
}
