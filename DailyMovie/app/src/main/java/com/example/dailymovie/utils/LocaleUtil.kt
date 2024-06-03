package com.example.dailymovie.utils

import java.util.Locale

object LocaleUtil {
    fun getDeviceLanguage(): String {
        return Locale.getDefault().language
    }

    fun getDeviceCountry(): String {
        return Locale.getDefault().country
    }

    fun getLanguageAndCountry(): String {
        return "${getDeviceLanguage()}-${getDeviceCountry()}"
    }
}