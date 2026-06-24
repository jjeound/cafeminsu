package com.cafeminsu.data.mock

import com.cafeminsu.domain.model.AppNotification
import com.cafeminsu.domain.model.Coupon
import com.cafeminsu.domain.model.CouponStatus
import com.cafeminsu.domain.model.CouponType
import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.NotificationType
import com.cafeminsu.domain.model.StampCard
import com.cafeminsu.domain.model.Store
import com.cafeminsu.domain.model.StoreAmenity
import com.cafeminsu.domain.model.StoreStatus
import java.time.Instant
import java.time.ZoneId

object MockData {
    val menuCategories: List<MenuCategory> = listOf(
        MenuCategory(id = "coffee", name = "커피", sortOrder = 1),
        MenuCategory(id = "tea", name = "티", sortOrder = 2),
        MenuCategory(id = "dessert", name = "디저트", sortOrder = 3),
    )

    private val sizeOptions = MenuOptionGroup(
        id = "size",
        name = "사이즈",
        required = true,
        minSelect = 1,
        maxSelect = 1,
        options = listOf(
            MenuOption(id = "size-regular", name = "Regular", extraPrice = 0, isAvailable = true),
            MenuOption(id = "size-large", name = "Large", extraPrice = 500, isAvailable = true),
        ),
    )

    private val temperatureOptions = MenuOptionGroup(
        id = "temperature",
        name = "온도",
        required = true,
        minSelect = 1,
        maxSelect = 1,
        options = listOf(
            MenuOption(id = "temperature-hot", name = "HOT", extraPrice = 0, isAvailable = true),
            MenuOption(id = "temperature-ice", name = "ICE", extraPrice = 0, isAvailable = true),
        ),
    )

    private val shotOptions = MenuOptionGroup(
        id = "shot",
        name = "샷 추가",
        required = false,
        minSelect = 0,
        maxSelect = 1,
        options = listOf(
            MenuOption(id = "shot-none", name = "없음", extraPrice = 0, isAvailable = true),
            MenuOption(id = "shot-one", name = "+1샷", extraPrice = 500, isAvailable = true),
            MenuOption(id = "shot-two", name = "+2샷", extraPrice = 1_000, isAvailable = true),
        ),
    )

    private val teaSweetnessOptions = MenuOptionGroup(
        id = "sweetness",
        name = "당도",
        required = false,
        minSelect = 0,
        maxSelect = 1,
        options = listOf(
            MenuOption(id = "sweetness-less", name = "덜 달게", extraPrice = 0, isAvailable = true),
            MenuOption(id = "sweetness-honey", name = "꿀 추가", extraPrice = 600, isAvailable = true),
        ),
    )

    val menuItems: List<MenuItem> = listOf(
        MenuItem(
            id = "americano",
            categoryId = "coffee",
            name = "민수 아메리카노",
            description = "고소한 블렌드의 깔끔한 기본 커피",
            basePrice = 4_500,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(temperatureOptions, sizeOptions, shotOptions),
        ),
        MenuItem(
            id = "latte",
            categoryId = "coffee",
            name = "바닐라라떼",
            description = "달콤한 바닐라 시럽이 어우러진 부드러운 라떼",
            basePrice = 5_500,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(temperatureOptions, sizeOptions, shotOptions),
        ),
        MenuItem(
            id = "einspanner",
            categoryId = "coffee",
            name = "아인슈페너",
            description = "달콤한 크림을 올린 시그니처 커피",
            basePrice = 6_000,
            imageUrl = null,
            isSoldOut = true,
            options = listOf(temperatureOptions, sizeOptions),
        ),
        MenuItem(
            id = "yuja-tea",
            categoryId = "tea",
            name = "유자차",
            description = "향긋한 유자청을 넣은 따뜻한 차",
            basePrice = 4_800,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(temperatureOptions, teaSweetnessOptions),
        ),
        MenuItem(
            id = "earl-grey",
            categoryId = "tea",
            name = "얼그레이",
            description = "베르가못 향이 은은한 홍차",
            basePrice = 4_600,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(temperatureOptions),
        ),
        MenuItem(
            id = "cheesecake",
            categoryId = "dessert",
            name = "바스크 치즈케이크",
            description = "진한 치즈 풍미의 조각 케이크",
            basePrice = 6_800,
            imageUrl = null,
            isSoldOut = false,
            options = emptyList(),
        ),
    )

    val nearbyStores: List<Store> = listOf(
        Store(
            id = "gangnam",
            name = "카페민수 강남점",
            address = "서울 강남구 테헤란로 134",
            phone = "02-3456-7890",
            distanceMeters = 120,
            latitude = 37.498,
            longitude = 127.028,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = listOf(
                StoreAmenity.Outlet,
                StoreAmenity.Wifi,
                StoreAmenity.DriveThru,
                StoreAmenity.Terrace,
                StoreAmenity.Parking,
            ),
        ),
        Store(
            id = "yeoksam",
            name = "카페민수 역삼점",
            address = "서울 강남구 역삼로 92",
            phone = "02-3456-7810",
            distanceMeters = 340,
            latitude = 37.500,
            longitude = 127.036,
            status = StoreStatus.Open,
            closingTimeLabel = "22:00 마감",
            amenities = listOf(
                StoreAmenity.Outlet,
                StoreAmenity.Wifi,
                StoreAmenity.Terrace,
                StoreAmenity.Parking,
            ),
        ),
        Store(
            id = "seolleung",
            name = "카페민수 선릉점",
            address = "서울 강남구 선릉로 414",
            phone = "02-3456-7820",
            distanceMeters = 780,
            latitude = 37.504,
            longitude = 127.049,
            status = StoreStatus.ClosingSoon,
            closingTimeLabel = "20:00 마감",
            amenities = listOf(
                StoreAmenity.Outlet,
                StoreAmenity.Wifi,
            ),
        ),
    )

    val initialStampCard: StampCard = StampCard(
        userId = "guest",
        currentCount = 4,
        goalCount = 10,
        history = emptyList(),
    )

    val initialGifticons: List<Gifticon> = listOf(
        Gifticon(
            id = "gifticon-1",
            title = "아메리카노 교환권",
            barcodeValue = "CAFE-MINSU-GIFT-0001",
            qrValue = "CAFE-MINSU-QR-0001",
            expiresAtMillis = 1_830_297_600_000L,
            status = GifticonStatus.Available,
        ),
        Gifticon(
            id = "gifticon-2",
            title = "디저트 2천원 할인권",
            barcodeValue = "CAFE-MINSU-GIFT-0002",
            qrValue = "CAFE-MINSU-QR-0002",
            expiresAtMillis = 1_827_619_200_000L,
            status = GifticonStatus.Expired,
            amount = 2_000,
        ),
    )

    val initialCoupons: List<Coupon> = listOf(
        Coupon(
            id = "coupon-free-drink",
            type = CouponType.FreeDrink,
            title = "무료 음료 1잔 쿠폰",
            amount = null,
            expiresAtMillis = 1_788_105_600_000L,
            status = CouponStatus.Available,
        ),
        Coupon(
            id = "coupon-10000",
            type = CouponType.Amount,
            title = "₩10,000",
            amount = 10_000,
            expiresAtMillis = 1_774_704_000_000L,
            status = CouponStatus.Available,
        ),
        Coupon(
            id = "coupon-8500",
            type = CouponType.Amount,
            title = "₩8,500",
            amount = 8_500,
            expiresAtMillis = 1_774_704_000_000L,
            status = CouponStatus.Available,
        ),
    )

    fun initialNotifications(nowMillis: Long): List<AppNotification> {
        val zoneId = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val yesterday = today.minusDays(1)

        return listOf(
            AppNotification(
                id = "noti-order-ready",
                type = NotificationType.OrderReady,
                title = "주문이 준비됐어요",
                body = "주문번호 A-2419 — 픽업대에서 수령해주세요",
                createdAtMillis = nowMillis,
                read = false,
            ),
            AppNotification(
                id = "noti-order-accepted",
                type = NotificationType.OrderAccepted,
                title = "주문이 수락됐어요",
                body = "주문번호 A-2419",
                createdAtMillis = nowMillis - FiveMinutesMillis,
                read = false,
            ),
            AppNotification(
                id = "noti-stamp-earned",
                type = NotificationType.StampEarned,
                title = "스탬프 적립",
                body = "강남점에서 스탬프 2개가 적립되었어요",
                createdAtMillis = nowMillis - FiveMinutesMillis,
                read = true,
            ),
            AppNotification(
                id = "noti-gifticon-received",
                type = NotificationType.GifticonReceived,
                title = "기프티콘이 도착했어요",
                body = "친구가 ₩10,000 기프티콘을 보냈어요",
                createdAtMillis = yesterday.atTime(19, 42).atZone(zoneId).toInstant().toEpochMilli(),
                read = true,
            ),
            AppNotification(
                id = "noti-order-completed",
                type = NotificationType.OrderCompleted,
                title = "주문 완료",
                body = "주문번호 A-2331 — 카페민수에서 즐거운 시간 되세요",
                createdAtMillis = yesterday.atTime(14, 51).atZone(zoneId).toInstant().toEpochMilli(),
                read = true,
            ),
        )
    }

    private const val FiveMinutesMillis = 5L * 60L * 1000L
}
