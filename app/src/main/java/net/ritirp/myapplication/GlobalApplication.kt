package net.ritirp.myapplication

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.kakao.vectormap.KakaoMapSdk
import net.ritirp.myapplication.utils.LogFilter

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // GL 에러 로그 필터링 - 가장 먼저 실행
        LogFilter.setupLogFiltering()
        LogFilter.suppressNativeLogs()
        LogFilter.forceSupressGLLogs() // 새로 추가된 강력한 차단 메서드

        // StrictMode 설정으로 GL 에러 최소화
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectNonSdkApiUsage()
                    .penaltyLog()
                    .build()
            )
        }

        try {
            // 카카오맵 SDK 초기화
            KakaoMapSdk.init(this, "45b6314dd164865c07d22932a73b65b0")
            Log.d("GlobalApplication", "카카오맵 SDK 초기화 완료")
        } catch (e: Exception) {
            Log.e("GlobalApplication", "카카오맵 SDK 초기화 실패: ${e.message}")
        }

        // OpenGL 컨텍스트 최적화를 위한 시스템 속성 설정
        try {
            System.setProperty("debug.egl.hw", "0")
            System.setProperty("ro.kernel.qemu.gles", "1")
            System.setProperty("ro.opengles.version", "131072") // OpenGL ES 2.0
        } catch (e: Exception) {
            Log.w("GlobalApplication", "OpenGL 시스템 속성 설정 실패: ${e.message}")
        }
    }
}
