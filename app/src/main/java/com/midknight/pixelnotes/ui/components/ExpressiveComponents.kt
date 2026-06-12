package com.midknight.pixelnotes.ui.components

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
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
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

class MorphPolygonShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotation: Float = 0f
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(percentage).asComposePath()
        matrix.reset()
        matrix.translate(size.width / 2f, size.height / 2f)
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.rotateZ(rotation)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@Composable
fun ExpressiveIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    isSemiSquared: Boolean = false
) {
    val shapeA = remember { 
        if (isSemiSquared) RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) 
        else RoundedPolygon.circle(numVertices = 8) 
    }
    val shapeB = remember { 
        if (isSemiSquared) RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.1f)) 
        else RoundedPolygon.star(numVerticesPerRadius = 8, innerRadius = 0.85f, rounding = CornerRounding(0.2f)) 
    }
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "morph"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isSemiSquared && isPressed) 0f else if (isSemiSquared) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "rot"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(MorphPolygonShape(morph, progress, rotation))
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
    val shapeA = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) } // Square (when rotated)
    val shapeB = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.05f)) } // Sharp Rhomboid
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "fabMorph"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isPressed) 0f else 45f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "fabRot"
    )

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(MorphPolygonShape(morph, progress, rotation))
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

@Composable
fun ExpressiveExtendedFAB(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    val shapeA = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) }
    val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.92f, rounding = CornerRounding(0.2f)) }
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "extFabMorph"
    )

    Box(
        modifier = modifier
            .clip(MorphPolygonShape(morph, progress))
            .background(containerColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val shapeA = remember { RoundedPolygon(numVertices = 4, rounding = CornerRounding(0.3f)) }
    val shapeB = remember { RoundedPolygon.star(numVerticesPerRadius = 4, innerRadius = 0.95f, rounding = CornerRounding(0.2f)) }
    val morph = remember { Morph(shapeA, shapeB) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "btnMorph"
    )

    Box(
        modifier = modifier
            .clip(MorphPolygonShape(morph, progress))
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
