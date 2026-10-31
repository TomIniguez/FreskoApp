package com.example.freskoapp.domain.model

import java.time.LocalDate

data class Product(
    val id: Long = 0,
    val name: String,
    val expirationDate: LocalDate,
    val quantity: Int,
    val location: Location,
    val status: ProductStatus = ProductStatus.ACTIVE,
)

// Closes `units` of an active product. All units -> the record closes with its quantity kept (>= 1);
// fewer -> it stays active with the rest. No schema change: the units live in `quantity`.
fun Product.closeUnits(units: Int, newStatus: ProductStatus): Product {
    require(newStatus != ProductStatus.ACTIVE)
    require(units in 1..quantity)
    return if (units == quantity) copy(status = newStatus) else copy(quantity = quantity - units)
}

// Inverse of closeUnits: reopens a closed record, or gives the units back to the one that stayed active.
fun Product.reopenUnits(units: Int, closed: Boolean): Product =
    if (closed) copy(status = ProductStatus.ACTIVE) else copy(quantity = quantity + units)

// Lot rule for registration: two ACTIVE products are the same lot if name (trim, collapsed whitespace, case-insensitive),
// expiration date and location match. shortcut: accents stay distinct ("leche" != "léche"), no schema change for a normalized column.
fun Product.isSameLotAs(other: Product): Boolean =
    status == ProductStatus.ACTIVE && other.status == ProductStatus.ACTIVE &&
        expirationDate == other.expirationDate && location == other.location &&
        name.normalizedName() == other.name.normalizedName()

private fun String.normalizedName() = trim().replace(Regex("\\s+"), " ").lowercase()
