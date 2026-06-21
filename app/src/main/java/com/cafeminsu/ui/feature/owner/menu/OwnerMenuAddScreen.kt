package com.cafeminsu.ui.feature.owner.menu

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafeminsu.R
import com.cafeminsu.ui.components.CafeButton
import com.cafeminsu.ui.components.CafeChip
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun OwnerMenuAddRoute(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OwnerMenuAddViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OwnerMenuAddEvent.Saved -> {
                    Toast.makeText(context, "메뉴가 추가되었어요", Toast.LENGTH_SHORT).show()
                    onSaved()
                }

                is OwnerMenuAddEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    OwnerMenuAddScreen(
        uiState = uiState,
        onImagePicked = viewModel::onImagePicked,
        onCategorySelected = viewModel::onCategorySelected,
        onNameChange = viewModel::onNameChange,
        onPriceChange = viewModel::onPriceChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSaleToggle = viewModel::onSaleToggle,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
fun OwnerMenuAddScreen(
    uiState: OwnerMenuAddUiState,
    onImagePicked: (String?) -> Unit,
    onCategorySelected: (OwnerMenuAddCategory) -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSaleToggle: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { onImagePicked(it.toString()) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.canvas,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = colors.surfaceDark,
                    contentColor = colors.onDark,
                    shape = CafeTheme.shapes.radiusMd,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OwnerMenuAddTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.space5),
                verticalArrangement = Arrangement.spacedBy(spacing.space5),
            ) {
                Spacer(modifier = Modifier.height(spacing.space1))

                ImageUploadBox(
                    imageUri = uiState.imageUri,
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )

                FieldSection(label = "카테고리", required = true) {
                    CategoryChips(
                        selected = uiState.category,
                        onCategorySelected = onCategorySelected,
                    )
                }

                FieldSection(label = "메뉴명", required = true) {
                    OwnerMenuAddField(
                        value = uiState.name,
                        onValueChange = onNameChange,
                        placeholder = "메뉴 이름을 입력하세요",
                    )
                }

                FieldSection(label = "가격", required = true) {
                    OwnerMenuAddField(
                        value = uiState.priceInput,
                        onValueChange = onPriceChange,
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leading = {
                            Text(
                                text = "₩",
                                style = CafeTheme.typography.bodyL,
                                color = colors.muted,
                            )
                        },
                    )
                }

                FieldSection(label = "설명", required = false) {
                    OwnerMenuAddField(
                        value = uiState.description,
                        onValueChange = onDescriptionChange,
                        placeholder = "메뉴 설명을 입력하세요 (선택)",
                        singleLine = false,
                        minHeight = spacing.space18 + spacing.space8,
                    )
                }

                SaleStatusCard(
                    onSale = uiState.onSale,
                    onSaleToggle = onSaleToggle,
                )

                Spacer(modifier = Modifier.height(spacing.space2))
            }

            OwnerMenuAddBottomBar(
                enabled = uiState.canSubmit,
                onSubmit = onSubmit,
            )
        }
    }
}

@Composable
private fun OwnerMenuAddTopBar(onBack: () -> Unit) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space14)
            .background(colors.canvas)
            .padding(horizontal = spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(spacing.space10 + spacing.space2)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "뒤로",
                tint = colors.ink,
                modifier = Modifier.size(spacing.space6),
            )
        }

        Text(
            text = "메뉴 추가",
            modifier = Modifier
                .weight(1f)
                .padding(end = spacing.space10 + spacing.space2),
            style = CafeTheme.typography.h2,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ImageUploadBox(
    imageUri: String?,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val cornerRadius = spacing.space4

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(spacing.space18 + spacing.space10)
            .clip(CafeTheme.shapes.radiusLg)
            .background(colors.surfaceCard)
            .drawBehind {
                if (imageUri == null) {
                    drawRoundRect(
                        color = colors.hairline,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(cornerRadius.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(DashOn, DashOff),
                                0f,
                            ),
                        ),
                    )
                }
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "대표 사진",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "＋",
                    style = CafeTheme.typography.h1,
                    color = colors.muted,
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = "대표 사진 추가",
                    style = CafeTheme.typography.caption,
                    color = colors.muted,
                )
            }
        }
    }
}

@Composable
private fun FieldSection(
    label: String,
    required: Boolean,
    content: @Composable () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = CafeTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
            if (required) {
                Spacer(modifier = Modifier.size(spacing.space1))
                Text(
                    text = "*필수",
                    style = CafeTheme.typography.caption,
                    color = colors.primary,
                )
            }
        }
        content()
    }
}

@Composable
private fun CategoryChips(
    selected: OwnerMenuAddCategory,
    onCategorySelected: (OwnerMenuAddCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(CafeTheme.spacing.space2),
    ) {
        OwnerMenuAddCategory.entries.forEach { category ->
            CafeChip(
                text = category.label,
                selected = category == selected,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun OwnerMenuAddField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    minHeight: Dp? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val resolvedMinHeight = minHeight ?: (spacing.space10 + spacing.space3)
    val verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = resolvedMinHeight)
            .background(color = colors.surfaceCard, shape = CafeTheme.shapes.radiusMd),
        singleLine = singleLine,
        textStyle = CafeTheme.typography.bodyL.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                verticalAlignment = verticalAlignment,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(modifier = Modifier.size(spacing.space2))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = CafeTheme.typography.bodyL,
                            color = colors.muted,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun SaleStatusCard(
    onSale: Boolean,
    onSaleToggle: (Boolean) -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CafeTheme.shapes.radiusLg)
            .background(colors.surfaceCard)
            .padding(horizontal = spacing.space4, vertical = spacing.space4),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.space1)) {
            Text(
                text = "판매 상태",
                style = CafeTheme.typography.h3,
                color = colors.ink,
            )
            Text(
                text = "등록 즉시 판매중으로 표시됩니다",
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }

        Switch(
            checked = onSale,
            onCheckedChange = onSaleToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                checkedBorderColor = colors.primary,
                uncheckedThumbColor = colors.canvas,
                uncheckedTrackColor = colors.hairline,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}

@Composable
private fun OwnerMenuAddBottomBar(
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = colors.hairline,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .background(colors.canvas)
            .padding(horizontal = spacing.space5, vertical = spacing.space4),
    ) {
        CafeButton(
            text = "저장하기",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
}

private const val DashOn = 12f
private const val DashOff = 8f
