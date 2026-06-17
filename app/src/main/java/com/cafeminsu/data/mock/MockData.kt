package com.cafeminsu.data.mock

import com.cafeminsu.domain.model.Gifticon
import com.cafeminsu.domain.model.GifticonStatus
import com.cafeminsu.domain.model.MenuCategory
import com.cafeminsu.domain.model.MenuItem
import com.cafeminsu.domain.model.MenuOption
import com.cafeminsu.domain.model.MenuOptionGroup
import com.cafeminsu.domain.model.StampCard

object MockData {
    const val minimumOrderAmount: Int = 10_000

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
            MenuOption(id = "size-regular", name = "레귤러", extraPrice = 0, isAvailable = true),
            MenuOption(id = "size-large", name = "라지", extraPrice = 700, isAvailable = true),
        ),
    )

    private val temperatureOptions = MenuOptionGroup(
        id = "temperature",
        name = "온도",
        required = true,
        minSelect = 1,
        maxSelect = 1,
        options = listOf(
            MenuOption(id = "temperature-hot", name = "따뜻하게", extraPrice = 0, isAvailable = true),
            MenuOption(id = "temperature-ice", name = "차갑게", extraPrice = 0, isAvailable = true),
        ),
    )

    private val shotOptions = MenuOptionGroup(
        id = "shot",
        name = "샷",
        required = false,
        minSelect = 0,
        maxSelect = 2,
        options = listOf(
            MenuOption(id = "shot-extra", name = "샷 추가", extraPrice = 500, isAvailable = true),
            MenuOption(id = "shot-decaf", name = "디카페인", extraPrice = 500, isAvailable = true),
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
            options = listOf(sizeOptions, temperatureOptions, shotOptions),
        ),
        MenuItem(
            id = "latte",
            categoryId = "coffee",
            name = "크림 라떼",
            description = "부드러운 우유와 진한 에스프레소",
            basePrice = 5_300,
            imageUrl = null,
            isSoldOut = false,
            options = listOf(sizeOptions, temperatureOptions, shotOptions),
        ),
        MenuItem(
            id = "einspanner",
            categoryId = "coffee",
            name = "아인슈페너",
            description = "달콤한 크림을 올린 시그니처 커피",
            basePrice = 6_000,
            imageUrl = null,
            isSoldOut = true,
            options = listOf(sizeOptions, temperatureOptions),
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
        ),
    )
}
