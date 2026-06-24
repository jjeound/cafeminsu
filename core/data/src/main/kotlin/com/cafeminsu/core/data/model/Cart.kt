package com.cafeminsu.core.data.model

import com.cafeminsu.core.database.model.entity.cart.CartItemEntity
import com.cafeminsu.core.model.cart.Cart
import com.cafeminsu.core.model.cart.CartInvalidReason
import com.cafeminsu.core.model.cart.CartItem
import com.cafeminsu.core.model.cart.CartValidation
import com.cafeminsu.core.model.cart.SelectedOption
import com.cafeminsu.core.model.media.ImageSource
import org.json.JSONArray
import org.json.JSONObject

fun CartItemEntity.asExternalModel(): CartItem =
    CartItem(
        id = id,
        menuId = menuId,
        name = name,
        image = imageUrl?.let(ImageSource::Remote) ?: ImageSource.None,
        price = price,
        selectedOptions = selectedOptionsJson.toSelectedOptions(),
        quantity = quantity,
    )

fun Cart.toEntities(storeId: Long): List<CartItemEntity> =
    items.mapNotNull { item ->
        CartItemEntity(
            id = item.id,
            storeId = storeId,
            menuId = item.menuId,
            name = item.name,
            imageUrl = when (val image = item.image) {
                is ImageSource.Remote -> image.url
                ImageSource.None,
                is ImageSource.Local,
                -> null
            },
            price = item.price,
            selectedOptionsJson = item.selectedOptions.toJsonString(),
            quantity = item.quantity,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

fun List<CartItemEntity>.asExternalModel(): Cart =
    Cart(
        items = map(CartItemEntity::asExternalModel),
        subtotal = sumOf { item -> item.price * item.quantity },
        validation = if (isEmpty()) {
            CartValidation.Invalid(listOf(CartInvalidReason.Empty))
        } else {
            CartValidation.Valid
        },
    )

fun List<SelectedOption>.toJsonString(): String =
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

fun String.toSelectedOptions(): List<SelectedOption> =
    runCatching {
        JSONArray(this).let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    add(
                        SelectedOption(
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
