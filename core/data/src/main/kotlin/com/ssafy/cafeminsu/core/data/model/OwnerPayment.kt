package com.ssafy.cafeminsu.core.data.model

import com.ssafy.cafeminsu.core.model.sales.DailySales
import com.ssafy.cafeminsu.core.model.sales.SoldMenu
import com.ssafy.cafeminsu.core.model.sales.StorePayment
import com.ssafy.cafeminsu.core.model.sales.StorePaymentHistory
import com.ssafy.cafeminsu.core.model.sales.StoreSalesSummary
import com.ssafy.cafeminsu.core.network.model.response.payment.StorePaymentLineResponse
import com.ssafy.cafeminsu.core.network.model.response.payment.StorePaymentsResponse
import com.ssafy.cafeminsu.core.network.model.response.payment.StoreSalesSummaryResponse

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
