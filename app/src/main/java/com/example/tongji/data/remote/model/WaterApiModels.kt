package com.example.tongji.data.remote.model

import com.google.gson.annotations.SerializedName

data class WaterGroup(
    @SerializedName("ClassNo") val classNo: String?,
    @SerializedName("ClassName") val className: String?,
    @SerializedName("PosNum") val posNum: Int?,
    @SerializedName("WarnPosNum") val warnPosNum: Int?,
    @SerializedName("UseFreeRate") val useFreeRate: Int?,
    @SerializedName("BookRate") val bookRate: Int?,
    @SerializedName("Actkind") val actKind: Int?,
    @SerializedName("BookCode") val bookCode: String?
)

data class WaterController(
    @SerializedName("ClassNo") val classNo: String?,
    @SerializedName("ClassName") val className: String?,
    @SerializedName("PosNum") val posNum: Int?,
    @SerializedName("WarnPosNum") val warnPosNum: Int?,
    @SerializedName("UseFreeRate") val useFreeRate: Int?,
    @SerializedName("BookRate") val bookRate: Int?,
    @SerializedName("Actkind") val actKind: Int?,
    @SerializedName("BookCode") val bookCode: String?
)

data class WaterGroupResponse(
    @SerializedName("RetNo") val retNo: Int?,
    @SerializedName("RetDsp") val retDsp: String?,
    @SerializedName("List") val list: List<WaterGroup>?
)

data class WaterControllerResponse(
    @SerializedName("RetNo") val retNo: Int?,
    @SerializedName("RetDsp") val retDsp: String?,
    @SerializedName("List") val list: List<WaterController>?
)

data class WaterTokenResponse(
    @SerializedName("RetNo") val retNo: Int?,
    @SerializedName("RetDsp") val retDsp: String?,
    @SerializedName("Token") val token: String?
)

data class WaterSsoCheckResponse(
    @SerializedName("Ret") val ret: String?,
    @SerializedName("Msg") val msg: String?,
    @SerializedName("User") val user: WaterSsoUser?
)

data class WaterSsoUser(
    @SerializedName("ACCOUNT") val account: String?,
    @SerializedName("account") val accountLower: String?
)