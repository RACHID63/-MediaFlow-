package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Contrôleur robuste pour le déplacement et réordonnancement par glisser-déposer (Drag & Drop)
 * d'éléments d'une liste (chansons d'albums ou de playlists).
 *
 * Résout le problème de réinitialisation ("snapping back") en :
 * 1. Utilisant l'ID unique immuable de l'élément (itemKey) pour le pointerInput au lieu de l'index
 * 2. Enregistrant immédiatement et de manière persistante la nouvelle liste à la fin du geste
 * 3. Ne réinitialisant jamais la position en cas d'annulation de geste
 */
class ReorderableListController<T>(
    initialItems: List<T>,
    val keyOf: (T) -> Any,
    private val onReordered: (List<T>) -> Unit
) {
    var list by mutableStateOf(initialItems)
        private set

    var draggingKey by mutableStateOf<Any?>(null)
        private set

    var dragOffsetY by mutableFloatStateOf(0f)
        private set

    val isDragging: Boolean
        get() = draggingKey != null

    fun syncItems(newItems: List<T>) {
        if (!isDragging) {
            list = newItems
        }
    }

    fun onDragStart(key: Any) {
        draggingKey = key
        dragOffsetY = 0f
    }

    fun onDrag(dragAmountY: Float, itemHeightPx: Float) {
        val currentKey = draggingKey ?: return
        val currentIndex = list.indexOfFirst { keyOf(it) == currentKey }
        if (currentIndex < 0) return

        dragOffsetY += dragAmountY
        val threshold = itemHeightPx * 0.45f

        // Déplacement vers le bas
        if (dragOffsetY > threshold && currentIndex < list.size - 1) {
            val mutable = list.toMutableList()
            val item = mutable.removeAt(currentIndex)
            mutable.add(currentIndex + 1, item)
            list = mutable
            dragOffsetY -= itemHeightPx
        }
        // Déplacement vers le haut
        else if (dragOffsetY < -threshold && currentIndex > 0) {
            val mutable = list.toMutableList()
            val item = mutable.removeAt(currentIndex)
            mutable.add(currentIndex - 1, item)
            list = mutable
            dragOffsetY += itemHeightPx
        }
    }

    fun onDragEnd() {
        val finalKey = draggingKey
        draggingKey = null
        dragOffsetY = 0f
        if (finalKey != null) {
            onReordered(list)
        }
    }

    fun onDragCancel() {
        // En cas de relâchement ou d'interruption, on valide la position atteinte
        onDragEnd()
    }
}

@Composable
fun <T> rememberReorderableList(
    items: List<T>,
    keyOf: (T) -> Any,
    onReordered: (List<T>) -> Unit
): ReorderableListController<T> {
    val currentOnReordered by rememberUpdatedState(onReordered)
    val controller = remember {
        ReorderableListController(
            initialItems = items,
            keyOf = keyOf,
            onReordered = { currentOnReordered(it) }
        )
    }

    // Synchronisation avec la base de données Room quand la liste change en dehors du drag
    LaunchedEffect(items) {
        if (!controller.isDragging) {
            controller.syncItems(items)
        }
    }

    return controller
}

@Composable
fun <T> DraggableReorderableItem(
    itemKey: Any,
    controller: ReorderableListController<T>,
    modifier: Modifier = Modifier,
    content: @Composable (isDragging: Boolean) -> Unit
) {
    val isDragging = controller.draggingKey == itemKey
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 3f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = controller.dragOffsetY
                    shadowElevation = 18.dp.toPx()
                    scaleX = 1.035f
                    scaleY = 1.035f
                }
            }
            .pointerInput(itemKey) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        controller.onDragStart(itemKey)
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
