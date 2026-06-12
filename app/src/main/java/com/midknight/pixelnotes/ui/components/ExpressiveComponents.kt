package com.midknight.pixelnotes.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

// 1. MorphPolygonShape limpio: Solo para botones 1:1 (Iconos y FABs circulares)
class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(percentage).asComposePath()
        val matrix = android.graphics.Matrix()

        // Escala simétrica basada en el lado más corto para no deformar
        val scale = minOf(size.width, size.height) / 2f
        matrix.postScale(scale, scale)
        matrix.postTranslate(size.width / 2f, size.height / 2f)

        val androidPath = path.asAndroidPath()
        androidPath.transform(matrix)

        return Outline.Generic(androidPath.asComposePath())
    }
}

// 2. Botones cuadrados (Usan la figura polígonal perfecta)
@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp
) {
    val shapeA = remember { RoundedPolygon.circle() }
    val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.75f, rounding = CornerRounding(0.2f)) }
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
        label = "iconMorph"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(MorphPolygonShape(morph, progress))
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun ExpressiveFAB(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val shapeA = remember { RoundedPolygon.circle() }
    val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 12, innerRadius = 0.85f, rounding = CornerRounding(0.2f)) }
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "fabMorph"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(MorphPolygonShape(morph, progress))
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

// 3. Botones Rectangulares (Arreglados con RoundedCornerShape nativo)
@Composable
fun ExpressiveExtendedFAB(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animamos los DP de las esquinas en lugar de un polígono
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "extFabCorner"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ExpressiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    isSquareEdge: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Ajuste dinámico de esquinas para mantener la estética limpia
    val defaultRadius = if (isSquareEdge) 8.dp else 24.dp
    val pressedRadius = if (isSquareEdge) 24.dp else 8.dp

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) pressedRadius else defaultRadius,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "btnCorner"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}