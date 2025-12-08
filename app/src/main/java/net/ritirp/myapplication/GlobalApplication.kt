package net.ritirp.myapplication

import android.app.Application
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.kakao.vectormap.KakaoMapSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.ritirp.myapplication.data.model.SensitivityLevel
import net.ritirp.myapplication.data.repository.AuthRepository
import net.ritirp.myapplication.data.repository.CrashSettingsRepository
import net.ritirp.myapplication.data.repository.FriendRepository
import net.ritirp.myapplication.data.repository.MapRepository
import net.ritirp.myapplication.data.repository.RidingStatisticsRepository
import net.ritirp.myapplication.data.repository.TeamRepository
import net.ritirp.myapplication.data.repository.UserRepository
import net.ritirp.myapplication.service.AppVisibilityObserver
import net.ritirp.myapplication.service.CrashDetector
import net.ritirp.myapplication.service.LocationUpdateManager
import net.ritirp.myapplication.service.RidingMetricsTracker

/**
 * Application 레벨 초기화.
 * - KakaoMapSdk.init 은 Application onCreate 에서 1회만 호출.
 * - CrashDetector 인스턴스를 전역으로 관리
 */
class GlobalApplication : Application() {
    lateinit var crashDetector: CrashDetector
        private set

    lateinit var ridingMetricsTracker: RidingMetricsTracker
        private set

    lateinit var crashSettingsRepository: CrashSettingsRepository
        private set

    lateinit var teamRepository: TeamRepository
        private set

    lateinit var friendRepository: FriendRepository
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var drivingRepository: net.ritirp.myapplication.data.repository.DrivingRepository
        private set

    lateinit var mapRepository: MapRepository
        private set

    lateinit var userRepository: UserRepository
        private set

    lateinit var ridingRecordRepository: net.ritirp.myapplication.data.repository.RidingRecordRepository
        private set

    lateinit var localRidingRecordRepository: net.ritirp.myapplication.data.repository.LocalRidingRecordRepository
        private set

    lateinit var ridingStatisticsRepository: RidingStatisticsRepository
        private set

    lateinit var leanAngleSensorManager: net.ritirp.myapplication.service.LeanAngleSensorManager
        private set

    private lateinit var locationUpdateManager: LocationUpdateManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // TODO: 운영 배포시 키는 NDK / CI Secret 등으로 분리 권장
        KakaoMapSdk.init(this, "45b6314dd164865c07d22932a73b65b0")

        // RetrofitClient 초기화 (인증 토큰 인터셉터를 위해)
        net.ritirp.myapplication.data.api.RetrofitClient.init(this)

        // 설정 저장소 초기화
        crashSettingsRepository = CrashSettingsRepository(this)

        // 팀 저장소 초기화
        teamRepository = TeamRepository(this)

        // 친구 저장소 초기화
        friendRepository = FriendRepository(this)

        // 인증 저장소 초기화
        authRepository = AuthRepository(this)

        // 라이딩 저장소 초기화
        drivingRepository =
            net.ritirp.myapplication.data.repository.DrivingRepository(
                net.ritirp.myapplication.data.api.RetrofitClient.drivingApi,
                this,
            )

        // Map 저장소 초기화
        mapRepository = MapRepository(
            LocationServices.getFusedLocationProviderClient(this),
            net.ritirp.myapplication.data.api.RetrofitClient.mapApi
        )

        // 사용자 저장소 초기화
        userRepository = UserRepository(this)

        // 주행 기록 저장소 초기화
        val database = net.ritirp.myapplication.data.local.RidingRecordDatabase.getDatabase(this)
        ridingRecordRepository = net.ritirp.myapplication.data.repository.RidingRecordRepository(database.ridingRecordDao())

        // 로컬 주행 기록 저장소 초기화 (Room DB)
        localRidingRecordRepository = net.ritirp.myapplication.data.repository.LocalRidingRecordRepository(this)

        // 주행 통계 저장소 초기화 (API 기반)
        ridingStatisticsRepository = RidingStatisticsRepository(
            net.ritirp.myapplication.data.api.RetrofitClient.drivingApi
        )

        // 기울기 각도 센서 매니저 초기화
        leanAngleSensorManager = net.ritirp.myapplication.service.LeanAngleSensorManager(this)

        // 주행 통계 추적기 초기화 (LocationUpdateManager보다 먼저 초기화)
        ridingMetricsTracker = RidingMetricsTracker(this)

        // 위치 업데이트 매니저 초기화 (Application Scope에서 실행)
        locationUpdateManager =
            LocationUpdateManager(
                applicationScope,
                LocationServices.getFusedLocationProviderClient(this),
                drivingRepository,
                mapRepository,
                ridingMetricsTracker,
            )

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
            },
        ).observe()
    }

    companion object {
        fun getCrashDetector(context: Context): CrashDetector {
            return (context.applicationContext as GlobalApplication).crashDetector
        }

        fun getRidingMetricsTracker(context: Context): RidingMetricsTracker {
            return (context.applicationContext as GlobalApplication).ridingMetricsTracker
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

        fun getDrivingRepository(context: Context): net.ritirp.myapplication.data.repository.DrivingRepository {
            return (context.applicationContext as GlobalApplication).drivingRepository
        }

        fun getMapRepository(context: Context): MapRepository {
            return (context.applicationContext as GlobalApplication).mapRepository
        }

        fun getUserRepository(context: Context): UserRepository {
            return (context.applicationContext as GlobalApplication).userRepository
        }

        fun getRidingRecordRepository(context: Context): net.ritirp.myapplication.data.repository.RidingRecordRepository {
            return (context.applicationContext as GlobalApplication).ridingRecordRepository
        }

        fun getLocalRidingRecordRepository(context: Context): net.ritirp.myapplication.data.repository.LocalRidingRecordRepository {
            return (context.applicationContext as GlobalApplication).localRidingRecordRepository
        }

        fun getRidingStatisticsRepository(context: Context): RidingStatisticsRepository {
            return (context.applicationContext as GlobalApplication).ridingStatisticsRepository
        }

        fun getLeanAngleSensorManager(context: Context): net.ritirp.myapplication.service.LeanAngleSensorManager {
            return (context.applicationContext as GlobalApplication).leanAngleSensorManager
        }
    }
}
