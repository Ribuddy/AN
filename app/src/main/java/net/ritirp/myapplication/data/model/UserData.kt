package net.ritirp.myapplication.data.model

/**
 * 사용자 프로필 데이터
 */
data class UserProfile(
    val id: String,
    val name: String,
    val nickname: String?,
    val oneLineIntroduction: String?,
    val ribuddyId: String,
    val profileImage: String?,
)
