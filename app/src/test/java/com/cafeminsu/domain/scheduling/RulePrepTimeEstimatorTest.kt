package com.cafeminsu.domain.scheduling

import com.cafeminsu.domain.model.CartItem
import com.cafeminsu.domain.model.Order
import com.cafeminsu.domain.model.OrderStatus
import com.cafeminsu.domain.model.SelectedOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RulePrepTimeEstimatorTest {
    private val weights = SchedulingWeights()
    private val estimator = RulePrepTimeEstimator(weights)

    @Test
    fun singleItemNoOptionUsesBasePlusPerItem() {
        val order = order(items = listOf(item(quantity = 1, optionCount = 0)))

        val expected = weights.prepBaseSeconds + weights.prepPerItemSeconds
        assertEquals(expected, estimator.estimateSeconds(order))
    }

    @Test
    fun moreOptionsIncreasePrepTime() {
        val noOption = estimator.estimateSeconds(order(items = listOf(item(quantity = 1, optionCount = 0))))
        val twoOptions = estimator.estimateSeconds(order(items = listOf(item(quantity = 1, optionCount = 2))))

        assertTrue("옵션이 많을수록 제조 시간이 길어야 한다", twoOptions > noOption)
    }

    @Test
    fun higherQuantityIncreasesPrepTime() {
        val qtyOne = estimator.estimateSeconds(order(items = listOf(item(quantity = 1, optionCount = 1))))
        val qtyThree = estimator.estimateSeconds(order(items = listOf(item(quantity = 3, optionCount = 1))))

        assertTrue("수량이 많을수록 제조 시간이 길어야 한다", qtyThree > qtyOne)
    }

    @Test
    fun moreItemsIncreasePrepTime() {
        val oneItem = estimator.estimateSeconds(order(items = listOf(item(quantity = 1, optionCount = 0))))
        val twoItems = estimator.estimateSeconds(
            order(items = listOf(item(quantity = 1, optionCount = 0), item(quantity = 1, optionCount = 0))),
        )

        assertTrue("항목이 많을수록 제조 시간이 길어야 한다", twoItems > oneItem)
    }

    @Test
    fun matchesExplicitFormula() {
        // base + Σ(perItem + perOption*옵션수) * quantity
        val order = order(
            items = listOf(
                item(quantity = 2, optionCount = 1),
                item(quantity = 1, optionCount = 3),
            ),
        )
        val expected = weights.prepBaseSeconds +
            (weights.prepPerItemSeconds + weights.prepPerOptionSeconds * 1) * 2 +
            (weights.prepPerItemSeconds + weights.prepPerOptionSeconds * 3) * 1

        assertEquals(expected, estimator.estimateSeconds(order))
    }

    private fun order(items: List<CartItem>): Order =
        Order(
            id = "o1",
            orderNumber = "1042",
            items = items,
            totalAmount = 4_500,
            status = OrderStatus.Accepted,
            createdAtMillis = 0L,
        )

    private fun item(quantity: Int, optionCount: Int): CartItem =
        CartItem(
            id = "item-$quantity-$optionCount",
            menuItemId = "americano",
            name = "아메리카노",
            unitPrice = 4_500,
            selectedOptions = (0 until optionCount).map { index ->
                SelectedOption(
                    groupId = "group-$index",
                    optionId = "option-$index",
                    name = "옵션 $index",
                    extraPrice = 0,
                )
            },
            quantity = quantity,
        )
}
