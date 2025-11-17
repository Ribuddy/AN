package net.ritirp.myapplication.data.model

import com.google.gson.annotations.SerializedName

/**
 * 친구 관련 데이터 모델
 */

// 친구 정보
data class FriendInfo(
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("nickname") val nickname: String? = null,
    @SerializedName("ribuddyId") val ribuddyId: String,
    @SerializedName("profileImage") val profileImage: String? = null,
    @SerializedName("oneLineIntroduction") val oneLineIntroduction: String? = null,
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
)

// 친구 추가 요청 (라이버디 ID로)
data class AddFriendByRibuddyIdRequest(
    @SerializedName("ribuddyId") val ribuddyId: String,
)

// 친구 삭제 요청
data class DeleteFriendRequest(
    @SerializedName("friendUserId") val friendUserId: String,
)

// 친구 즐겨찾기 설정 요청
data class EditFriendStatusRequest(
    @SerializedName("toUserId") val toUserId: String,
    @SerializedName("isFavorite") val isFavorite: Boolean,
)

// 친구 목록 응답
data class FriendListResponse(
    @SerializedName("favorites") val favorites: List<FriendInfo>,
    @SerializedName("friends") val friends: List<FriendInfo>,
)
