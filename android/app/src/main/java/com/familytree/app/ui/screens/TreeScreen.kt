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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.familytree.app.data.model.FamilyMember
import com.familytree.app.ui.viewmodel.FamilyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val NODE_WIDTH = 160f
private const val NODE_HEIGHT = 80f
private const val HORIZONTAL_SPACING = 40f
private const val VERTICAL_SPACING = 100f
private const val MAX_TREE_RENDER_NODES = 1200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    viewModel: FamilyViewModel,
    onMemberClick: (String) -> Unit
) {
    val allMembers by viewModel.allMembers.collectAsState()

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

internal data class LayoutNode(
    val member: FamilyMember,
    val x: Float,
    val y: Float,
    val children: List<LayoutNode>
)

internal data class LayoutBuildResult(
    val roots: List<LayoutNode>,
    val renderedNodeCount: Int,
    val isTruncated: Boolean
)

private data class SubtreeLayout(
    val node: LayoutNode,
    val maxX: Float
)

private data class TreeLayoutUiState(
    val nodes: List<LayoutNode> = emptyList(),
    val hitRects: List<Pair<Rect, String>> = emptyList(),
    val renderedNodeCount: Int = 0,
    val isTruncated: Boolean = false
)

private class LayoutTraverseState(
    val maxNodes: Int
) {
    var renderedNodeCount: Int = 0
    var isTruncated: Boolean = false
}

private val treeMemberComparator = compareBy<FamilyMember>({ it.lastName }, { it.firstName }, { it.id })

@Composable
private fun FamilyTreeCanvas(
    members: List<FamilyMember>,
    onMemberClick: (String) -> Unit
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val maleColor = Color(0xFF42A5F5)
    val femaleColor = Color(0xFFEF5350)
    val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    val titlePaint = remember(onSurfaceColor) {
        android.graphics.Paint().apply {
            color = onSurfaceColor.toArgb()
            textSize = 32f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val subtitlePaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    val layoutState by produceState(initialValue = TreeLayoutUiState(), members) {
        value = withContext(Dispatchers.Default) {
            val layoutResult = buildLayout(members, maxNodes = MAX_TREE_RENDER_NODES)
            TreeLayoutUiState(
                nodes = layoutResult.roots,
                hitRects = buildHitRects(layoutResult.roots),
                renderedNodeCount = layoutResult.renderedNodeCount,
                isTruncated = layoutResult.isTruncated
            )
        }
    }

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
                .pointerInput(layoutState.hitRects) {
                    detectTapGestures { tapOffset ->
                        val adjustedX = (tapOffset.x - offsetX) / scale
                        val adjustedY = (tapOffset.y - offsetY) / scale
                        for ((rect, memberId) in layoutState.hitRects) {
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

            for (node in layoutState.nodes) {
                drawTreeConnections(node, startX, startY, lineColor)
            }
            for (node in layoutState.nodes) {
                drawTreeNodes(
                    node = node,
                    startX = startX,
                    startY = startY,
                    maleColor = maleColor,
                    femaleColor = femaleColor,
                    titlePaint = titlePaint,
                    subtitlePaint = subtitlePaint
                )
            }
        }

        if (layoutState.isTruncated) {
            Text(
                text = "成员较多，当前仅渲染前 ${layoutState.renderedNodeCount} 人以避免卡顿",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

internal fun buildLayout(
    members: List<FamilyMember>,
    maxNodes: Int = MAX_TREE_RENDER_NODES
): LayoutBuildResult {
    if (members.isEmpty() || maxNodes <= 0) {
        return LayoutBuildResult(
            roots = emptyList(),
            renderedNodeCount = 0,
            isTruncated = members.isNotEmpty() && maxNodes <= 0
        )
    }

    val childrenByParent = buildChildrenByParent(members)
    val roots = members.filter { it.fatherId == null && it.motherId == null }
        .sortedWith(treeMemberComparator)
    if (roots.isEmpty()) {
        return LayoutBuildResult(roots = emptyList(), renderedNodeCount = 0, isTruncated = false)
    }

    val state = LayoutTraverseState(maxNodes)
    val result = mutableListOf<LayoutNode>()
    var currentX = 0f

    for (root in roots) {
        if (state.renderedNodeCount >= state.maxNodes) {
            state.isTruncated = true
            break
        }
        val layout = layoutSubtree(
            member = root,
            childrenByParent = childrenByParent,
            depth = 0,
            startX = currentX,
            state = state,
            pathVisited = mutableSetOf()
        ) ?: continue
        result += layout.node
        currentX = layout.maxX + HORIZONTAL_SPACING + NODE_WIDTH
    }

    return LayoutBuildResult(
        roots = result,
        renderedNodeCount = state.renderedNodeCount,
        isTruncated = state.isTruncated
    )
}

private fun buildChildrenByParent(members: List<FamilyMember>): Map<String, List<FamilyMember>> {
    val childrenByParent = mutableMapOf<String, MutableList<FamilyMember>>()
    for (member in members) {
        member.fatherId?.let { fatherId ->
            childrenByParent.getOrPut(fatherId) { mutableListOf() }.add(member)
        }
        member.motherId
            ?.takeIf { it != member.fatherId }
            ?.let { motherId ->
                childrenByParent.getOrPut(motherId) { mutableListOf() }.add(member)
            }
    }
    childrenByParent.values.forEach { it.sortWith(treeMemberComparator) }
    return childrenByParent
}

private fun layoutSubtree(
    member: FamilyMember,
    childrenByParent: Map<String, List<FamilyMember>>,
    depth: Int,
    startX: Float,
    state: LayoutTraverseState,
    pathVisited: MutableSet<String>
): SubtreeLayout? {
    if (state.renderedNodeCount >= state.maxNodes) {
        state.isTruncated = true
        return null
    }
    if (!pathVisited.add(member.id)) {
        return null
    }

    return try {
        state.renderedNodeCount += 1
        val y = depth * (NODE_HEIGHT + VERTICAL_SPACING)
        val children = childrenByParent[member.id].orEmpty()

        if (children.isEmpty()) {
            SubtreeLayout(node = LayoutNode(member, startX, y, emptyList()), maxX = startX)
        } else {
            var childX = startX
            val childLayouts = mutableListOf<SubtreeLayout>()
            for (child in children) {
                if (state.renderedNodeCount >= state.maxNodes) {
                    state.isTruncated = true
                    break
                }
                val childLayout = layoutSubtree(
                    member = child,
                    childrenByParent = childrenByParent,
                    depth = depth + 1,
                    startX = childX,
                    state = state,
                    pathVisited = pathVisited
                )
                if (childLayout != null) {
                    childLayouts += childLayout
                    childX = childLayout.maxX + HORIZONTAL_SPACING + NODE_WIDTH
                }
                if (state.isTruncated) {
                    break
                }
            }

            if (childLayouts.isEmpty()) {
                SubtreeLayout(node = LayoutNode(member, startX, y, emptyList()), maxX = startX)
            } else {
                val firstChildCenter = childLayouts.first().node.x + NODE_WIDTH / 2
                val lastChildCenter = childLayouts.last().node.x + NODE_WIDTH / 2
                val parentX = ((firstChildCenter + lastChildCenter) / 2) - NODE_WIDTH / 2
                val maxX = maxOf(parentX, childLayouts.maxOf { it.maxX })
                SubtreeLayout(
                    node = LayoutNode(member, parentX, y, childLayouts.map { it.node }),
                    maxX = maxX
                )
            }
        }
    } finally {
        pathVisited.remove(member.id)
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

        drawLine(
            color = lineColor,
            start = Offset(parentCenterX, parentBottomY),
            end = Offset(parentCenterX, midY),
            strokeWidth = 2f
        )
        drawLine(
            color = lineColor,
            start = Offset(parentCenterX, midY),
            end = Offset(childCenterX, midY),
            strokeWidth = 2f
        )
        drawLine(
            color = lineColor,
            start = Offset(childCenterX, midY),
            end = Offset(childCenterX, childTopY),
            strokeWidth = 2f
        )

        drawTreeConnections(child, startX, startY, lineColor)
    }
}

private fun DrawScope.drawTreeNodes(
    node: LayoutNode,
    startX: Float,
    startY: Float,
    maleColor: Color,
    femaleColor: Color,
    titlePaint: android.graphics.Paint,
    subtitlePaint: android.graphics.Paint
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

    drawContext.canvas.nativeCanvas.drawText(
        node.member.fullName,
        x + NODE_WIDTH / 2,
        y + 36f,
        titlePaint
    )

    val subtitle = buildString {
        append(if (node.member.gender == "male") "男" else "女")
        node.member.birthDate?.let { append(" · $it") }
    }

    drawContext.canvas.nativeCanvas.drawText(
        subtitle,
        x + NODE_WIDTH / 2,
        y + 62f,
        subtitlePaint
    )

    for (child in node.children) {
        drawTreeNodes(child, startX, startY, maleColor, femaleColor, titlePaint, subtitlePaint)
    }
}
