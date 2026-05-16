package com.test.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ButtonVariant { Primary, Secondary, Outlined, Ghost, Danger, Success }
enum class ButtonSize { Small, Medium, Large }

data class ButtonConfig(
    val variant: ButtonVariant = ButtonVariant.Primary,
    val size: ButtonSize = ButtonSize.Medium,
    val isLoading: Boolean = false,
    val isFullWidth: Boolean = false,
    val leadingIcon: ImageVector? = null,
    val trailingIcon: ImageVector? = null,
    val cornerRadius: Dp = 8.dp,
    val elevation: Dp = 2.dp,
    val tooltipText: String? = null
)

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    config: ButtonConfig = ButtonConfig(),
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) ButtonTokens.PressedScale else 1f,
        animationSpec = tween(ButtonTokens.AnimationDuration),
        label = "scale"
    )

    val (bgColor, contentColor, borderColor) = resolveColors(config.variant, enabled)
    val animatedBg by animateColorAsState(bgColor, label = "bg")

    val (horizontalPadding, verticalPadding, fontSize, iconSize) = resolveSize(config.size)

    val buttonModifier = modifier
        .scale(scale)
        .then(if (config.isFullWidth) Modifier.fillMaxWidth() else Modifier)

    when (config.variant) {
        ButtonVariant.Outlined -> OutlinedButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !config.isLoading,
            shape = RoundedCornerShape(config.cornerRadius),
            border = BorderStroke(1.dp, borderColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
            interactionSource = interactionSource
        ) {
            ButtonContent(text, config, contentColor, fontSize, iconSize)
        }
        ButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !config.isLoading,
            shape = RoundedCornerShape(config.cornerRadius),
            colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
            interactionSource = interactionSource
        ) {
            ButtonContent(text, config, contentColor, fontSize, iconSize)
        }
        else -> Button(
            onClick = onClick,
            modifier = buttonModifier,
            enabled = enabled && !config.isLoading,
            shape = RoundedCornerShape(config.cornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = animatedBg,
                contentColor = contentColor,
                disabledContainerColor = animatedBg.copy(alpha = 0.38f),
                disabledContentColor = contentColor.copy(alpha = 0.38f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = config.elevation),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
            interactionSource = interactionSource
        ) {
            ButtonContent(text, config, contentColor, fontSize, iconSize)
        }
    }
}

@Composable
private fun ButtonIcon(icon: ImageVector, size: Dp, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = tint
    )
}

@Composable
private fun ButtonContent(
    text: String,
    config: ButtonConfig,
    contentColor: Color,
    fontSize: TextUnit,
    iconSize: Dp
) {
    if (config.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(iconSize),
            color = contentColor,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(8.dp))
    } else {
        config.leadingIcon?.let { icon ->
            ButtonIcon(icon = icon, size = iconSize, tint = contentColor)
            Spacer(Modifier.width(6.dp))
        }
    }
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        color = contentColor
    )
    config.trailingIcon?.let { icon ->
        Spacer(Modifier.width(6.dp))
        ButtonIcon(icon = icon, size = iconSize, tint = contentColor)
    }
}

private data class ColorTriple(val bg: Color, val content: Color, val border: Color)
private data class SizeQuad(val h: Dp, val v: Dp, val font: TextUnit, val icon: Dp)

private fun resolveColors(variant: ButtonVariant, enabled: Boolean): ColorTriple {
    return when (variant) {
        ButtonVariant.Primary -> ColorTriple(Color(0xFF6200EE), Color.White, Color.Transparent)
        ButtonVariant.Secondary -> ColorTriple(Color(0xFF03DAC5), Color.Black, Color.Transparent)
        ButtonVariant.Outlined -> ColorTriple(Color.Transparent, Color(0xFF6200EE), Color(0xFF6200EE))
        ButtonVariant.Ghost -> ColorTriple(Color.Transparent, Color(0xFF6200EE), Color.Transparent)
        ButtonVariant.Danger -> ColorTriple(Color(0xFFB00020), Color.White, Color.Transparent)
        ButtonVariant.Success -> ColorTriple(Color(0xFF388E3C), Color.White, Color.Transparent)
    }
}

private fun resolveSize(size: ButtonSize): SizeQuad {
    return when (size) {
        ButtonSize.Small -> SizeQuad(12.dp, 6.dp, 13.sp, ButtonTokens.SmallIconSize)
        ButtonSize.Medium -> SizeQuad(20.dp, 10.dp, 15.sp, ButtonTokens.MediumIconSize)
        ButtonSize.Large -> SizeQuad(28.dp, 14.dp, 17.sp, ButtonTokens.LargeIconSize)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    CustomButton(text, onClick, modifier, ButtonConfig(ButtonVariant.Primary, isLoading = isLoading))
}

@Composable
fun DangerButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CustomButton(text, onClick, modifier, ButtonConfig(ButtonVariant.Danger))
}

@Composable
fun OutlinedCustomButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    CustomButton(text, onClick, modifier, ButtonConfig(ButtonVariant.Outlined))
}

@Composable
fun FullWidthButton(text: String, onClick: () -> Unit, variant: ButtonVariant = ButtonVariant.Primary) {
    CustomButton(text, onClick, config = ButtonConfig(variant = variant, isFullWidth = true))
}

@Composable
fun IconTextButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: Boolean = false
) {
    CustomButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        config = ButtonConfig(
            leadingIcon = if (!trailing) icon else null,
            trailingIcon = if (trailing) icon else null
        )
    )
}

@Composable
fun LoadingButton(text: String, modifier: Modifier = Modifier) {
    CustomButton(
        text = text,
        onClick = {},
        modifier = modifier,
        config = ButtonConfig(isLoading = true),
        enabled = false
    )
}

object ButtonTokens {
    val PressedScale = 0.95f
    val AnimationDuration = 150
    val DefaultCornerRadius = 12.dp
    val SmallIconSize = 16.dp
    val MediumIconSize = 20.dp
    val LargeIconSize = 24.dp
}