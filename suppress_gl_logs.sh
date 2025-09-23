#!/bin/bash

# GL 에러 로그를 adb logcat 레벨에서 필터링하는 스크립트

# GL 관련 태그들을 완전히 차단
adb shell setprop log.tag.emuglGLESv2_enc S
adb shell setprop log.tag.GLESv2_enc S
adb shell setprop log.tag.goldfish-opengl S
adb shell setprop log.tag.GL2Encoder S
adb shell setprop log.tag.GLES20 S
adb shell setprop log.tag.OpenGLRenderer S
adb shell setprop log.tag.libEGL S
adb shell setprop log.tag.libGLESv2 S

# OpenGL 디버그 속성들 비활성화
adb shell setprop debug.egl.trace 0
adb shell setprop debug.egl.hw 0
adb shell setprop debug.opengl.trace 0
adb shell setprop debug.egl.callstack 0
adb shell setprop debug.egl.force_msaa false
adb shell setprop debug.egl.profiler 0

# 에뮬레이터 GL 설정
adb shell setprop ro.kernel.qemu.gles 1

echo "GL 에러 로그 차단 완료"
