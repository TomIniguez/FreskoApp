package com.example.freskoapp.data.mapper

import com.example.freskoapp.data.local.ProductEntity
import com.example.freskoapp.domain.model.Location
import com.example.freskoapp.domain.model.Product
import com.example.freskoapp.domain.model.ProductStatus
import java.time.LocalDate

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    expirationDate = LocalDate.ofEpochDay(expirationEpochDay),
    quantity = quantity,
    location = Location.valueOf(location),
    status = ProductStatus.valueOf(status),
    createdAt = createdAt,
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    expirationEpochDay = expirationDate.toEpochDay(),
    quantity = quantity,
    location = location.name,
    status = status.name,
    createdAt = createdAt,
)
