package com.example.myapplication.data

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ConditionFromFileJson(
    @SerializedName("code")
    @Expose
    val code: Int,
    @SerializedName("day")
    @Expose
    val day: String,
    @SerializedName("night")
    @Expose
    val night: String,
    @SerializedName("icon")
    @Expose
    val icon: Int,
    @SerializedName("languages")
    @Expose
    val languages: List<LanguageJson>
)

data class LanguageJson(
    @SerializedName("lang_name")
    @Expose
    val lang_name: String,
    @SerializedName("lang_iso")
    @Expose
    val lang_iso: String,
    @SerializedName("day_text")
    @Expose
    val day_text: String,
    @SerializedName("night_text")
    @Expose
    val night_text: String
)

data class ConditionsJson(
    val conditions: List<ConditionFromFileJson>
)