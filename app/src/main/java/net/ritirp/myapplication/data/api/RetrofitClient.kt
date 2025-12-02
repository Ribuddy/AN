package net.ritirp.myapplication.data.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.ritirp.myapplication.data.local.DataStoreManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 인스턴스 생성 객체
 */
object RetrofitClient {
    private var applicationContext: Context? = null

    /**
     * Application Context 설정 (GlobalApplication에서 호출)
     */
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * 인증 토큰을 자동으로 추가하는 인터셉터
     */
    private val authInterceptor =
        Interceptor { chain ->
            val originalRequest = chain.request()

            // 토큰이 필요없는 엔드포인트 (로그인 등)
            val noAuthPaths = listOf("/v1/auth/google/login", "/v1/auth/google/callback")
            val requestPath = originalRequest.url.encodedPath

            if (noAuthPaths.any { requestPath.contains(it) }) {
                return@Interceptor chain.proceed(originalRequest)
            }

            // DataStore에서 토큰 가져오기
            val token =
                applicationContext?.let { context ->
                    runBlocking {
                        try {
                            val dataStore = DataStoreManager.getDataStore(context)
                            dataStore.data.first()[DataStoreManager.ACCESS_TOKEN_KEY]
                        } catch (e: Exception) {
                            Log.e("RetrofitClient", "토큰 가져오기 실패", e)
                            null
                        }
                    }
                }

            val newRequest =
                if (!token.isNullOrEmpty()) {
                    Log.d("RetrofitClient", "Authorization 헤더 추가: Bearer ${token.take(20)}...")
                    originalRequest.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    Log.w("RetrofitClient", "토큰이 없음, Authorization 헤더 없이 요청")
                    originalRequest
                }

            chain.proceed(newRequest)
        }

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 인증 인터셉터 추가
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(AuthApi.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val teamApi: TeamApi = retrofit.create(TeamApi::class.java)
    val friendApi: FriendApi = retrofit.create(FriendApi::class.java)
    val drivingApi: DrivingApi = retrofit.create(DrivingApi::class.java)
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val mapApi: MapApi = retrofit.create(MapApi::class.java)
}
