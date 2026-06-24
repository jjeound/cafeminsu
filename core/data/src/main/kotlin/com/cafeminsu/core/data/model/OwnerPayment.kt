package com.cafeminsu.core.data.model

import com.cafeminsu.core.model.sales.DailySales
import com.cafeminsu.core.model.sales.SoldMenu
import com.cafeminsu.core.model.sales.StorePayment
import com.cafeminsu.core.model.sales.StorePaymentHistory
import com.cafeminsu.core.model.sales.StoreSalesSummary
import com.cafeminsu.core.network.model.response.payment.StorePaymentLineResponse
import com.cafeminsu.core.network.model.response.payment.StorePaymentsResponse
import com.cafeminsu.core.network.model.response.payment.StoreSalesSummaryResponse

fun StoreSalesSummaryResponse.asExternalModel() = StoreSalesSummary(
    totalSales = totalSales,
    dailySales = dailySales.map { DailySales(it.date, it.amount, it.orderCount) },
    topMenus = topMenus.map { SoldMenu(it.menuId, it.name, it.quantity, it.amount) },
)

fun StorePaymentsResponse.asExternalModel() = StorePaymentHistory(
    totalAmount = total,
    payments = payments.map(StorePaymentLineResponse::asExternalModel),
)

fun StorePaymentLineResponse.asExternalModel() = StorePayment(paymentId, orderId, method, amount, paidAt)
