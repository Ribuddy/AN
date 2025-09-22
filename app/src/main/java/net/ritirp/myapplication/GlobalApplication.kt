package net.ritirp.myapplication

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 카카오맵 SDK 초기화
        KakaoMapSdk.init(this, "QtbEgPyWFAp9r2ESjRcUUU34onGKirm8")
    }
}
