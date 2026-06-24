package com.cafeminsu.data.mapper

import com.cafeminsu.core.AppResult
import com.cafeminsu.core.DomainError
import com.cafeminsu.data.remote.Item
import com.cafeminsu.data.remote.ItemRes
import com.cafeminsu.data.remote.OptionRes
import com.cafeminsu.data.remote.OrderCreateReq
import com.cafeminsu.data.remote.OrderCreateRes
import com.cafeminsu.data.remote.OrderDetailRes
import com.cafeminsu.data.remote.OrderListItemRes
import com.cafeminsu.domain.model.Cart
import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption

fun Cart.toOrderCreateReq(storeId: Long): AppResult<OrderCreateReq> {
    val requestItems = items.map { item ->
        val menuId = item.menuItemId.toLongOrNull()
            ?: return AppResult.Failure(DomainError.Validation("cart"))
        if (item.quantity <= 0) {
            return AppResult.Failure(DomainError.Validation("quantity"))
        }
        val optionIds = item.selectedOptions.map { selectedOption ->
            selectedOption.optionId.toLongOrNull()
                ?: return AppResult.Failure(DomainError.Validation("options"))
        }

        Item(
            menuId = menuId,
            quantity = item.quantity,
            optionIds = optionIds,
        )
    }

    return AppResult.Success(
        OrderCreateReq(
            storeId = storeId,
            orderType = ServerOrderTypeMobile,
            orderMethod = ServerOrderMethodManual,
            items = requestItems,
        ),
    )
}

fun OrderCreateRes.toOrder(
    cartItems: List<CartItem>,
    createdAtMillis: Long,
): AppResult<Order> {
    val id = orderId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toOrderStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        Order(
            id = id.toString(),
            orderNumber = orderNumber.orEmpty(),
            items = cartItems,
            totalAmount = totalAmount ?: DefaultAmount,
            status = mappedStatus,
            createdAtMillis = createdAtMillis,
        ),
    )
}

fun OrderDetailRes.toOrder(): AppResult<Order> {
    val id = orderId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toOrderStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        Order(
            id = id.toString(),
            orderNumber = orderNumber.orEmpty(),
            items = items.orEmpty().toCartItems(orderId = id),
            totalAmount = totalAmount ?: DefaultAmount,
            status = mappedStatus,
            createdAtMillis = createdAt.toEpochMillis(),
        ),
    )
}

fun List<OrderListItemRes>.toOrders(): AppResult<List<Order>> {
    val mapped = map { item ->
        when (val result = item.toOrder()) {
            is AppResult.Success -> result.data
            is AppResult.Failure -> return result
        }
    }
    return AppResult.Success(mapped)
}

private fun OrderListItemRes.toOrder(): AppResult<Order> {
    val id = orderId ?: return AppResult.Failure(DomainError.Unknown)
    val mappedStatus = status.toOrderStatus()
        ?: return AppResult.Failure(DomainError.Unknown)

    return AppResult.Success(
        Order(
            id = id.toString(),
            orderNumber = orderNumber.orEmpty(),
            items = emptyList(),
            totalAmount = totalAmount ?: DefaultAmount,
            status = mappedStatus,
            createdAtMillis = createdAt.toEpochMillis(),
        ),
    )
}

private fun List<ItemRes>.toCartItems(orderId: Long): List<CartItem> =
    mapIndexedNotNull { index, item ->
        val menuId = item.menuId ?: return@mapIndexedNotNull null
        val quantity = item.quantity ?: DefaultQuantity
        CartItem(
            id = "$orderId-$menuId-${index + 1}",
            menuItemId = menuId.toString(),
            name = item.menuName.orEmpty(),
            unitPrice = item.unitPrice ?: item.unitPriceFromSubtotal(quantity),
            selectedOptions = item.options.orEmpty().toSelectedOptions(),
            quantity = quantity,
        )
    }

private fun List<OptionRes>.toSelectedOptions(): List<SelectedOption> =
    mapNotNull { option -> option.toSelectedOption() }

private fun OptionRes.toSelectedOption(): SelectedOption? {
    val id = optionId ?: return null
    val groupName = optionGroup.normalizedOptionGroup()
    return SelectedOption(
        groupId = groupName,
        optionId = id.toString(),
        name = optionName.orEmpty(),
        extraPrice = optionPrice ?: DefaultAmount,
    )
}

private fun ItemRes.unitPriceFromSubtotal(quantity: Int): Int =
    if (quantity > 0) {
        (subtotal ?: DefaultAmount) / quantity
    } else {
        DefaultAmount
    }

private fun String?.toOrderStatus(): OrderStatus? =
    when (this) {
        "PENDING" -> OrderStatus.PendingPayment
        "ACCEPTED" -> OrderStatus.Accepted
        "READY" -> OrderStatus.Ready
        "DONE" -> OrderStatus.Completed
        "CANCELLED" -> OrderStatus.Cancelled
        else -> null
    }

internal fun String?.toEpochMillis(): Long = parseServerEpochMillis(this)

private fun String?.normalizedOptionGroup(): String =
    this?.trim()?.takeIf { it.isNotEmpty() } ?: DefaultOptionGroupName

private const val ServerOrderTypeMobile = "MOBILE"
private const val ServerOrderMethodManual = "MANUAL"
private const val DefaultAmount = 0
private const val DefaultQuantity = 1
private const val DefaultOptionGroupName = "옵션"
