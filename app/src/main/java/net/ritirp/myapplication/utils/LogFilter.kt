package net.ritirp.myapplication.utils

import android.util.Log
import java.io.PrintStream
import java.io.ByteArrayOutputStream

/**
 * GL 에러 로그를 필터링하는 유틸리티 클래스
 */
object LogFilter {

    private val glErrorPatterns = listOf(
        "emuglGLESv2_enc",
        "GL error 0x500",
        "GLESv2Validation::allowedCullFace",
        "s_glCullFace",
        "device/generic/goldfish-opengl",
        "GL2Encoder.cpp",
        "goldfish-opengl",
        "GLESv2_enc",
        "GL2Encoder",
        "condition [!GLESv2Validation::allowedCullFace(mode)]",
        "net.ritirp.myapplication.debug"
    )

    fun setupLogFiltering() {
        try {
            // 더 강력한 System.err 완전 차단
            val nullStream = object : PrintStream(ByteArrayOutputStream()) {
                override fun println(x: String?) {
                    if (x != null && !shouldFilterLog(x)) {
                        System.out.println(x) // GL 에러가 아니면 System.out으로 리다이렉트
                    }
                }

                override fun print(s: String?) {
                    if (s != null && !shouldFilterLog(s)) {
                        System.out.print(s)
                    }
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    val message = String(b, off, len)
                    if (!shouldFilterLog(message)) {
                        System.out.write(b, off, len)
                    }
                }

                override fun write(b: Int) {
                    // 바이트 단위로도 필터링
                    val char = b.toChar().toString()
                    if (!shouldFilterLog(char)) {
                        System.out.write(b)
                    }
                }
            }
            System.setErr(nullStream)

            // System.out도 더 강력하게 필터링
            val originalOut = System.out
            val filteredOut = object : PrintStream(originalOut) {
                override fun println(x: String?) {
                    if (x != null && !shouldFilterLog(x)) {
                        super.println(x)
                    }
                }

                override fun print(s: String?) {
                    if (s != null && !shouldFilterLog(s)) {
                        super.print(s)
                    }
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    val message = String(b, off, len)
                    if (!shouldFilterLog(message)) {
                        super.write(b, off, len)
                    }
                }

                override fun write(b: Int) {
                    val char = b.toChar().toString()
                    if (!shouldFilterLog(char)) {
                        super.write(b)
                    }
                }
            }
            System.setOut(filteredOut)

            Log.d("LogFilter", "강화된 GL 에러 로그 필터링 활성화됨")
        } catch (e: Exception) {
            Log.w("LogFilter", "로그 필터링 설정 실패: ${e.message}")
        }
    }

    private fun shouldFilterLog(message: String): Boolean {
        return glErrorPatterns.any { pattern ->
            message.contains(pattern, ignoreCase = true)
        }
    }

    // 네이티브 로그도 차단하려면 이 함수를 사용
    fun suppressNativeLogs() {
        try {
            // 더 많은 GL 관련 태그들을 억제
            System.setProperty("log.tag.emuglGLESv2_enc", "S")
            System.setProperty("log.tag.GLESv2_enc", "S")
            System.setProperty("log.tag.goldfish-opengl", "S")
            System.setProperty("log.tag.GL2Encoder", "S")
            System.setProperty("log.tag.GLES20", "S")
            System.setProperty("log.tag.OpenGLRenderer", "S")
            System.setProperty("log.tag.libEGL", "S")
            System.setProperty("log.tag.libGLESv2", "S")

            // 더 강력한 OpenGL 디버그 옵션 비활성화
            System.setProperty("debug.egl.trace", "0")
            System.setProperty("debug.egl.hw", "0")
            System.setProperty("debug.opengl.trace", "0")
            System.setProperty("ro.kernel.qemu.gles", "1")
            System.setProperty("debug.egl.callstack", "0")
            System.setProperty("debug.egl.force_msaa", "false")
            System.setProperty("debug.egl.profiler", "0")

            Log.d("LogFilter", "강화된 네이티브 GL 로그 억제 설정 완료")
        } catch (e: Exception) {
            Log.w("LogFilter", "네이티브 로그 억제 설정 실패: ${e.message}")
        }
    }

    // 추가: 런타임에 더 강력한 로그 차단
    fun forceSupressGLLogs() {
        try {
            // Android 로그 시스템 자체를 조작
            val logClass = Class.forName("android.util.Log")
            val isLoggableMethod = logClass.getDeclaredMethod("isLoggable", String::class.java, Int::class.javaPrimitiveType)

            // 리플렉션을 통해 로그 레벨을 조작하려 시도
            System.setProperty("log.tag", "SUPPRESS")
            System.setProperty("log.level", "SUPPRESS")

            Log.d("LogFilter", "강제 GL 로그 억제 시도 완료")
        } catch (e: Exception) {
            Log.w("LogFilter", "강제 로그 억제 실패: ${e.message}")
        }
    }
}
