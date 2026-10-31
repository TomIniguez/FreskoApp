package com.example.freskoapp.data.mapper

import com.example.freskoapp.data.remote.ProductDto
import com.example.freskoapp.data.remote.ProductInfoDto
import com.example.freskoapp.domain.model.ProductInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductInfoMapperTest {
    private fun dto(name: String?, brands: String? = null, status: Int = 1) =
        ProductInfoDto(status = status, product = ProductDto(productName = name, brands = brands))

    @Test
    fun foundWithBrand() {
        assertEquals(ProductInfo("Nutella", "Nutella, Ferrero"), dto("Nutella", "Nutella, Ferrero").toDomain())
    }

    @Test
    fun nullOrBlankBrandIsNull() {
        assertNull(dto("Nutella", null).toDomain()!!.brand)
        assertNull(dto("Nutella", "  ").toDomain()!!.brand)
    }

    @Test
    fun nameAndBrandAreTrimmed() {
        assertEquals(ProductInfo("Nutella", "Ferrero"), dto("  Nutella ", " Ferrero ").toDomain())
    }

    @Test
    fun noUsableNameIsNotFound() {
        assertNull(dto("").toDomain())
        assertNull(dto("   ").toDomain())
        assertNull(dto(null).toDomain())
        assertNull(ProductInfoDto(status = 1, product = null).toDomain())
    }

    @Test
    fun statusZeroIsNotFoundEvenWithAName() {
        assertNull(dto("Nutella", status = 0).toDomain())
    }
}
