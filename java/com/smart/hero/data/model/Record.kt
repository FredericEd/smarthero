package com.smart.hero.data.model

import com.beust.klaxon.Json

data class Record(
    @Json(name = "id_especial_historial") val id_historial: Int,
    val tipo: String,
    val fecha: String,
    val detalle: String
    )