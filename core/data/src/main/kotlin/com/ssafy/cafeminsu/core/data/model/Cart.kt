package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.database.model.entity.cart.CartItemEntity
import com.ssafy.cafeminsu.core.model.cart.Cart
import com.ssafy.cafeminsu.core.model.cart.CartInvalidReason
import com.ssafy.cafeminsu.core.model.cart.CartItem
import com.ssafy.cafeminsu.core.model.cart.CartValidation
import com.ssafy.cafeminsu.core.model.cart.SelectedOption
import org.json.JSONArray
import org.json.JSONObject

fun CartItemEntity.asExternalModel(): com.ssafy.cafeminsu.core.model.cart.CartItem =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.CartItem(
        id = id,
        menuId = menuId,
        name = name,
        image = imageUrl.orEmpty(),
        price = price,
        selectedOptions = selectedOptionsJson.toSelectedOptions(),
        quantity = quantity,
    )

fun com.ssafy.cafeminsu.core.model.cart.Cart.toEntities(storeId: Long): List<CartItemEntity> =
    items.map { item ->
        CartItemEntity(
            id = item.id,
            storeId = storeId,
            menuId = item.menuId,
            name = item.name,
            imageUrl = item.image,
            price = item.price,
            selectedOptionsJson = item.selectedOptions.toJsonString(),
            quantity = item.quantity,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

fun List<CartItemEntity>.asExternalModel(): com.ssafy.cafeminsu.core.model.cart.Cart =
    _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.Cart(
        items = map(CartItemEntity::asExternalModel),
        subtotal = sumOf { item -> item.price * item.quantity },
        validation = if (isEmpty()) {
            _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.CartValidation.Invalid(
                listOf(
                    _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.CartInvalidReason.Empty
                )
            )
        } else {
            _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.CartValidation.Valid
        },
    )

fun List<com.ssafy.cafeminsu.core.model.cart.SelectedOption>.toJsonString(): String =
    JSONArray().apply {
        forEach { option ->
            put(
                JSONObject()
                    .put(GroupIdKey, option.groupId)
                    .put(OptionIdKey, option.optionId)
                    .put(NameKey, option.name)
                    .put(ExtraPriceKey, option.extraPrice),
            )
        }
    }.toString()

fun String.toSelectedOptions(): List<com.ssafy.cafeminsu.core.model.cart.SelectedOption> =
    runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        _root_ide_package_.com.ssafy.cafeminsu.core.model.cart.SelectedOption(
                            groupId = item.optString(GroupIdKey),
                            optionId = item.optString(OptionIdKey),
                            name = item.optString(NameKey),
                            extraPrice = item.optInt(ExtraPriceKey),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

private const val GroupIdKey = "groupId"
private const val OptionIdKey = "optionId"
private const val NameKey = "name"
private const val ExtraPriceKey = "extraPrice"
