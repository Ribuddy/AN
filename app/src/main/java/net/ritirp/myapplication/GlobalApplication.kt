package net.ritirp.myapplication

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

/**
 * Application 레벨 초기화.
 * - KakaoMapSdk.init 은 Application onCreate 에서 1회만 호출.
 */
class GlobalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // TODO: 운영 배포시 키는 NDK / CI Secret 등으로 분리 권장
        KakaoMapSdk.init(this, "45b6314dd164865c07d22932a73b65b0")
    }
}
