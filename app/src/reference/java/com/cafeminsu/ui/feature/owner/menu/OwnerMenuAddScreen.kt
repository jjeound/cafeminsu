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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
        optionActions = OwnerMenuOptionActions(
            onAddGroup = viewModel::onAddOptionGroup,
            onRemoveGroup = viewModel::onRemoveOptionGroup,
            onGroupNameChange = viewModel::onOptionGroupNameChange,
            onAddOption = viewModel::onAddOption,
            onRemoveOption = viewModel::onRemoveOption,
            onOptionNameChange = viewModel::onOptionNameChange,
            onOptionPriceChange = viewModel::onOptionPriceChange,
        ),
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
    optionActions: OwnerMenuOptionActions,
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
        // 부모 AppNavHost Scaffold 가 이미 시스템바 인셋을 적용하므로 여기서 중복 적용하지 않는다.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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

                OwnerMenuOptionSection(
                    optionGroups = uiState.optionGroups,
                    actions = optionActions,
                )

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
    containerColor: Color? = null,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val resolvedMinHeight = minHeight ?: (spacing.space10 + spacing.space3)
    val resolvedContainer = containerColor ?: colors.surfaceCard
    val verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = resolvedMinHeight)
            .background(color = resolvedContainer, shape = CafeTheme.shapes.radiusMd),
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

/** 옵션 섹션 콜백 묶음. 화면 시그니처를 간결하게 유지하기 위해 한 데이터로 전달한다. */
data class OwnerMenuOptionActions(
    val onAddGroup: () -> Unit,
    val onRemoveGroup: (String) -> Unit,
    val onGroupNameChange: (String, String) -> Unit,
    val onAddOption: (String) -> Unit,
    val onRemoveOption: (String, String) -> Unit,
    val onOptionNameChange: (String, String, String) -> Unit,
    val onOptionPriceChange: (String, String, String) -> Unit,
)

@Composable
private fun OwnerMenuOptionSection(
    optionGroups: List<OwnerMenuOptionGroupInput>,
    actions: OwnerMenuOptionActions,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "옵션",
                style = CafeTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                color = colors.ink,
            )
            Spacer(modifier = Modifier.size(spacing.space2))
            Text(
                text = "선택사항",
                style = CafeTheme.typography.caption,
                color = colors.muted,
            )
        }

        optionGroups.forEach { group ->
            key(group.id) {
                OwnerMenuOptionGroupCard(group = group, actions = actions)
            }
        }

        AddRowButton(
            text = "+ 옵션 그룹 추가",
            dashed = true,
            onClick = actions.onAddGroup,
        )
    }
}

@Composable
private fun OwnerMenuOptionGroupCard(
    group: OwnerMenuOptionGroupInput,
    actions: OwnerMenuOptionActions,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CafeTheme.shapes.radiusLg)
            .background(colors.surfaceCard)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OwnerMenuAddField(
                    value = group.name,
                    onValueChange = { actions.onGroupNameChange(group.id, it) },
                    placeholder = "옵션 그룹 이름 (예: 사이즈)",
                    containerColor = colors.canvas,
                )
            }
            DeleteIconButton(
                onClick = { actions.onRemoveGroup(group.id) },
                contentDescription = "옵션 그룹 삭제",
            )
        }

        group.options.forEach { option ->
            key(option.id) {
                OwnerMenuOptionRow(
                    groupId = group.id,
                    option = option,
                    actions = actions,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline),
        )

        AddRowButton(
            text = "+ 옵션 추가",
            dashed = false,
            onClick = { actions.onAddOption(group.id) },
        )
    }
}

@Composable
private fun OwnerMenuOptionRow(
    groupId: String,
    option: OwnerMenuOptionInput,
    actions: OwnerMenuOptionActions,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OwnerMenuAddField(
                value = option.name,
                onValueChange = { actions.onOptionNameChange(groupId, option.id, it) },
                placeholder = "옵션 이름",
                containerColor = colors.canvas,
            )
        }
        Box(modifier = Modifier.width(spacing.space18 + spacing.space10 + spacing.space4)) {
            OwnerMenuAddField(
                value = option.priceInput,
                onValueChange = { actions.onOptionPriceChange(groupId, option.id, it) },
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                containerColor = colors.canvas,
                leading = {
                    Text(
                        text = "₩",
                        style = CafeTheme.typography.bodyL,
                        color = colors.muted,
                    )
                },
            )
        }
        DeleteIconButton(
            onClick = { actions.onRemoveOption(groupId, option.id) },
            contentDescription = "옵션 삭제",
        )
    }
}

@Composable
private fun DeleteIconButton(
    onClick: () -> Unit,
    contentDescription: String,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing

    Box(
        modifier = Modifier
            .size(spacing.space8)
            .clip(CafeTheme.shapes.radiusSm)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = contentDescription,
            tint = colors.muted,
            modifier = Modifier.size(spacing.space5),
        )
    }
}

@Composable
private fun AddRowButton(
    text: String,
    dashed: Boolean,
    onClick: () -> Unit,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val cornerRadius = spacing.space3

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.space10 + spacing.space2)
            .clip(CafeTheme.shapes.radiusMd)
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = colors.hairline,
                            cornerRadius = CornerRadius(cornerRadius.toPx()),
                            style = Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn, DashOff), 0f),
                            ),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CafeTheme.typography.body.copy(fontWeight = FontWeight.Medium),
            color = colors.primary,
        )
    }
}

private const val DashOn = 12f
private const val DashOff = 8f
