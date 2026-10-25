package com.example.freskoapp.domain.model

import java.time.LocalDate

data class Product(
    val id: Long = 0,
    val name: String,
    val expirationDate: LocalDate,
    val quantity: Int,
    val location: Location,
)
