package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

class ReorderableItemState(
    val isDragging: Boolean,
    val offsetY: Float
)

@Composable
fun <T> rememberReorderableList(
    items: List<T>,
    onReordered: (List<T>) -> Unit
): ReorderableListController<T> {
    return remember(items) {
        ReorderableListController(items, onReordered)
    }
}

class ReorderableListController<T>(
    private val initialItems: List<T>,
    private val onReordered: (List<T>) -> Unit
) {
    var currentList by androidx.compose.runtime.mutableStateOf(initialItems)
        private set

    var draggingIndex by mutableIntStateOf(-1)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    fun onDragStart(index: Int) {
        draggingIndex = index
        dragOffsetY = 0f
    }

    fun onDrag(dragAmountY: Float, itemHeightPx: Float) {
        if (draggingIndex < 0 || draggingIndex >= currentList.size) return

        dragOffsetY += dragAmountY
        val threshold = itemHeightPx * 0.7f

        // Déplacement vers le bas
        if (dragOffsetY > threshold && draggingIndex < currentList.size - 1) {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(draggingIndex)
            mutable.add(draggingIndex + 1, item)
            currentList = mutable
            draggingIndex += 1
            dragOffsetY -= itemHeightPx
        }
        // Déplacement vers le haut
        else if (dragOffsetY < -threshold && draggingIndex > 0) {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(draggingIndex)
            mutable.add(draggingIndex - 1, item)
            currentList = mutable
            draggingIndex -= 1
            dragOffsetY += itemHeightPx
        }
    }

    fun onDragEnd() {
        if (draggingIndex >= 0) {
            onReordered(currentList)
        }
        draggingIndex = -1
        dragOffsetY = 0f
    }

    fun onDragCancel() {
        draggingIndex = -1
        dragOffsetY = 0f
        currentList = initialItems
    }
}

@Composable
fun <T> DraggableReorderableItem(
    index: Int,
    controller: ReorderableListController<T>,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = controller.draggingIndex == index
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 68.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 2f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = controller.dragOffsetY
                    shadowElevation = 16.dp.toPx()
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
            }
            .pointerInput(controller, index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        controller.onDragStart(index)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        controller.onDrag(dragAmount.y, itemHeightPx)
                    },
                    onDragEnd = {
                        controller.onDragEnd()
                    },
                    onDragCancel = {
                        controller.onDragCancel()
                    }
                )
            }
    ) {
        content(isDragging)
    }
}
