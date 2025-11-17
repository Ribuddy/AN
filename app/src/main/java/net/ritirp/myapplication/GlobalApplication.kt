package net.ritirp.myapplication

import android.app.Application
import android.content.Context
import com.kakao.vectormap.KakaoMapSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.SensitivityLevel
import net.ritirp.myapplication.data.repository.CrashSettingsRepository
import net.ritirp.myapplication.data.repository.TeamRepository
import net.ritirp.myapplication.data.repository.FriendRepository
import net.ritirp.myapplication.data.repository.AuthRepository
import net.ritirp.myapplication.service.AppVisibilityObserver
import net.ritirp.myapplication.service.CrashDetector

/**
 * Application 레벨 초기화.
 * - KakaoMapSdk.init 은 Application onCreate 에서 1회만 호출.
 * - CrashDetector 인스턴스를 전역으로 관리
 */
class GlobalApplication : Application() {

    lateinit var crashDetector: CrashDetector
        private set

    lateinit var crashSettingsRepository: CrashSettingsRepository
        private set

    lateinit var teamRepository: TeamRepository
        private set

    lateinit var friendRepository: FriendRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // TODO: 운영 배포시 키는 NDK / CI Secret 등으로 분리 권장
        KakaoMapSdk.init(this, "45b6314dd164865c07d22932a73b65b0")

        // 설정 저장소 초기화
        crashSettingsRepository = CrashSettingsRepository(this)

        // 팀 저장소 초기화
        teamRepository = TeamRepository(this)

        // 친구 저장소 초기화
        friendRepository = FriendRepository(this)

        // 인증 저장소 초기화
        authRepository = AuthRepository(this)

        // 사고 감지기 초기화
        crashDetector = CrashDetector(this, SensitivityLevel.MEDIUM)

        // 설정 변경 감지 및 적용
        applicationScope.launch {
            crashSettingsRepository.sensitivityLevel.collectLatest { level ->
                crashDetector.updateSensitivity(level)
            }
        }

        // 앱 가시성 관찰자 등록
        AppVisibilityObserver(
            onForeground = {
                // 감지 활성화 상태 확인 후 시작
                applicationScope.launch {
                    crashSettingsRepository.isDetectionEnabled.collect { enabled ->
                        if (enabled) {
                            crashDetector.start()
                        }
                    }
                }
            },
            onBackground = {
                crashDetector.stop()
            }
        ).observe()
    }

    companion object {
        fun getCrashDetector(context: Context): CrashDetector {
            return (context.applicationContext as GlobalApplication).crashDetector
        }

        fun getCrashSettingsRepository(context: Context): CrashSettingsRepository {
            return (context.applicationContext as GlobalApplication).crashSettingsRepository
        }

        fun getTeamRepository(context: Context): TeamRepository {
            return (context.applicationContext as GlobalApplication).teamRepository
        }

        fun getFriendRepository(context: Context): FriendRepository {
            return (context.applicationContext as GlobalApplication).friendRepository
        }

        fun getAuthRepository(context: Context): AuthRepository {
            return (context.applicationContext as GlobalApplication).authRepository
        }
    }
}
