package com.cafeminsu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import com.cafeminsu.ui.theme.CafeTheme

@Composable
fun CafeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = CafeTheme.colors
    val spacing = CafeTheme.spacing
    val minHeight = spacing.space10 + spacing.space3

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(
                color = colors.surfaceCard,
                shape = CafeTheme.shapes.radiusMd,
            ),
        enabled = enabled,
        singleLine = singleLine,
        textStyle = CafeTheme.typography.bodyL.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.space4, vertical = spacing.space3),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = CafeTheme.typography.bodyL,
                        color = colors.muted,
                    )
                }
                innerTextField()
            }
        },
    )
}
