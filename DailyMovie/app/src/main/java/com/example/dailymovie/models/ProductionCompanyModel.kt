package com.example.dailymovie.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
/**
 * Una productora de las que aparecen en la ficha ("Paramount Pictures").
 *
 * @property logoPath ruta del logo, o null: las productoras pequeñas no suelen tenerlo.
 * @property originCountry el pais en dos letras ("US").
 */
@Parcelize
data class ProductionCompanyModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("logo_path")
    val logoPath: String?,

    @SerializedName("name")
    val name: String,

    @SerializedName("origin_country")
    val originCountry: String
) : Parcelable
