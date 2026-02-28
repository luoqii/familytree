package com.familytree.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familytree.app.data.model.FamilyMember
import com.familytree.app.ui.viewmodel.FamilyViewModel

private const val NODE_WIDTH = 160f
private const val NODE_HEIGHT = 80f
private const val HORIZONTAL_SPACING = 40f
private const val VERTICAL_SPACING = 100f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    viewModel: FamilyViewModel,
    onMemberClick: (String) -> Unit
) {
    val allMembers by viewModel.allMembers.collectAsState()

    LaunchedEffect(allMembers) {
        viewModel.loadTreeData()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "族谱",
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )

        if (allMembers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "族谱树形图",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = "添加家族成员后，这里将展示\n家族关系的树形结构图",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            FamilyTreeCanvas(
                members = allMembers,
                onMemberClick = onMemberClick
            )
        }
    }
}

private data class LayoutNode(
    val member: FamilyMember,
    val x: Float,
    val y: Float,
    val children: List<LayoutNode>
)

@Composable
private fun FamilyTreeCanvas(
    members: List<FamilyMember>,
    onMemberClick: (String) -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val maleColor = Color(0xFF42A5F5)
    val femaleColor = Color(0xFFEF5350)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val layoutNodes = remember(members) { buildLayout(members) }
    val hitRects = remember(layoutNodes) { buildHitRects(layoutNodes) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.3f, 3f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .transformable(state = state)
                .pointerInput(hitRects) {
                    detectTapGestures { tapOffset ->
                        val adjustedX = (tapOffset.x - offsetX) / scale
                        val adjustedY = (tapOffset.y - offsetY) / scale
                        for ((rect, memberId) in hitRects) {
                            if (rect.contains(Offset(adjustedX, adjustedY))) {
                                onMemberClick(memberId)
                                break
                            }
                        }
                    }
                }
        ) {
            val startX = 40f
            val startY = 40f

            for (node in layoutNodes) {
                drawTreeConnections(node, startX, startY, lineColor)
            }
            for (node in layoutNodes) {
                drawTreeNodes(node, startX, startY, surfaceColor, maleColor, femaleColor, onSurfaceColor)
            }
        }
    }
}

private fun buildLayout(members: List<FamilyMember>): List<LayoutNode> {
    val roots = members.filter { it.fatherId == null && it.motherId == null }
    if (roots.isEmpty()) return emptyList()

    var currentX = 0f
    return roots.map { root ->
        val node = layoutSubtree(root, members, 0, currentX)
        currentX = getSubtreeMaxX(node) + HORIZONTAL_SPACING + NODE_WIDTH
        node
    }
}

private fun layoutSubtree(
    member: FamilyMember,
    all: List<FamilyMember>,
    depth: Int,
    startX: Float
): LayoutNode {
    val children = all.filter { it.fatherId == member.id || it.motherId == member.id }
    val y = depth * (NODE_HEIGHT + VERTICAL_SPACING)

    if (children.isEmpty()) {
        return LayoutNode(member, startX, y, emptyList())
    }

    var childX = startX
    val childNodes = children.map { child ->
        val childNode = layoutSubtree(child, all, depth + 1, childX)
        childX = getSubtreeMaxX(childNode) + HORIZONTAL_SPACING + NODE_WIDTH
        childNode
    }

    val firstChildCenter = childNodes.first().x + NODE_WIDTH / 2
    val lastChildCenter = childNodes.last().x + NODE_WIDTH / 2
    val parentX = ((firstChildCenter + lastChildCenter) / 2) - NODE_WIDTH / 2

    return LayoutNode(member, parentX, y, childNodes)
}

private fun getSubtreeMaxX(node: LayoutNode): Float {
    return if (node.children.isEmpty()) {
        node.x
    } else {
        maxOf(node.x, node.children.maxOf { getSubtreeMaxX(it) })
    }
}

private fun buildHitRects(nodes: List<LayoutNode>): List<Pair<Rect, String>> {
    val result = mutableListOf<Pair<Rect, String>>()
    fun collect(node: LayoutNode) {
        val rect = Rect(
            left = node.x + 40f,
            top = node.y + 40f,
            right = node.x + 40f + NODE_WIDTH,
            bottom = node.y + 40f + NODE_HEIGHT
        )
        result.add(rect to node.member.id)
        node.children.forEach { collect(it) }
    }
    nodes.forEach { collect(it) }
    return result
}

private fun DrawScope.drawTreeConnections(
    node: LayoutNode,
    startX: Float,
    startY: Float,
    lineColor: Color
) {
    val parentCenterX = startX + node.x + NODE_WIDTH / 2
    val parentBottomY = startY + node.y + NODE_HEIGHT

    for (child in node.children) {
        val childCenterX = startX + child.x + NODE_WIDTH / 2
        val childTopY = startY + child.y

        val midY = (parentBottomY + childTopY) / 2

        val path = Path().apply {
            moveTo(parentCenterX, parentBottomY)
            lineTo(parentCenterX, midY)
            lineTo(childCenterX, midY)
            lineTo(childCenterX, childTopY)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        drawTreeConnections(child, startX, startY, lineColor)
    }
}

private fun DrawScope.drawTreeNodes(
    node: LayoutNode,
    startX: Float,
    startY: Float,
    surfaceColor: Color,
    maleColor: Color,
    femaleColor: Color,
    textColor: Color
) {
    val x = startX + node.x
    val y = startY + node.y
    val borderColor = if (node.member.gender == "male") maleColor else femaleColor

    drawRoundRect(
        color = borderColor.copy(alpha = 0.15f),
        topLeft = Offset(x, y),
        size = Size(NODE_WIDTH, NODE_HEIGHT),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
    )

    drawRoundRect(
        color = borderColor,
        topLeft = Offset(x, y),
        size = Size(NODE_WIDTH, NODE_HEIGHT),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor(
            if (textColor == Color.Black) "#1C1B1F" else "#E6E1E5"
        )
        textSize = 32f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    drawContext.canvas.nativeCanvas.drawText(
        node.member.fullName,
        x + NODE_WIDTH / 2,
        y + 36f,
        textPaint
    )

    val subPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 22f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    val subtitle = buildString {
        append(if (node.member.gender == "male") "男" else "女")
        node.member.birthDate?.let { append(" · $it") }
    }

    drawContext.canvas.nativeCanvas.drawText(
        subtitle,
        x + NODE_WIDTH / 2,
        y + 62f,
        subPaint
    )

    for (child in node.children) {
        drawTreeNodes(child, startX, startY, surfaceColor, maleColor, femaleColor, textColor)
    }
}
