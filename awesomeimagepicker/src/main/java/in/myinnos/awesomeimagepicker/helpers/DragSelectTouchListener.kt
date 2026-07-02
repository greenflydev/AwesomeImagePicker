package `in`.myinnos.awesomeimagepicker.helpers

import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Callback interface for drag-select events.
 */
interface DragSelectListener {
    /**
     * Called when an item's selection state should change during a drag gesture.
     * @param position The adapter position of the item.
     * @param shouldBeSelected Whether the item should be selected (true) or deselected (false).
     */
    fun onDragStateChanged(position: Int, shouldBeSelected: Boolean)

    /**
     * Called when the drag gesture ends (finger lifted or cancelled).
     */
    fun onDragSelectionFinished()
}

/**
 * A RecyclerView.OnItemTouchListener that implements swipe-to-select.
 *
 * Activation: When a finger touches down and then moves into a different grid cell
 * (different adapter position), drag-select mode activates with haptic feedback.
 * No long-press is required.
 *
 * Mode: The anchor item's current selection state determines the drag mode:
 * - If anchor is unselected → drag mode selects items
 * - If anchor is selected → drag mode deselects items
 *
 * Reversing: Moving the finger backward reverses changes made during the drag.
 * Items outside the current range are restored to their original state.
 *
 * Auto-scroll: When the finger is near the top/bottom edge, the RecyclerView
 * auto-scrolls to allow selecting items off-screen.
 */
class DragSelectTouchListener(
    private val recyclerView: RecyclerView,
    private val listener: DragSelectListener
) : RecyclerView.OnItemTouchListener {

    companion object {
        /** Edge zone in dp for auto-scroll activation */
        private const val AUTO_SCROLL_EDGE_DP = 50f

        /** Auto-scroll speed in pixels per tick */
        private const val AUTO_SCROLL_SPEED_PX = 20

        /** Auto-scroll interval in ms */
        private const val AUTO_SCROLL_INTERVAL_MS = 16L

        /** Minimum movement in pixels to consider activation (prevents accidental triggers) */
        private const val MIN_ACTIVATION_DISTANCE_DP = 8f

        /**
         * Ratio threshold: horizontal movement must be at least this much greater than
         * vertical movement to activate drag-select. This prevents vertical scrolling
         * from triggering drag-select.
         */
        private const val HORIZONTAL_BIAS_RATIO = 1.5f
    }

    private val autoScrollEdgePx: Float
    private val minActivationDistancePx: Float
    private val handler = Handler(Looper.getMainLooper())

    // Touch tracking
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownPosition = RecyclerView.NO_POSITION

    // Drag state
    private var isDragActive = false
    private var anchorPosition = RecyclerView.NO_POSITION
    private var lastDragPosition = RecyclerView.NO_POSITION
    private var dragModeIsSelect = true // true = select, false = deselect

    // Track original states of items affected during this drag so we can reverse
    private val originalStates = mutableMapOf<Int, Boolean>()

    // Track which positions are currently in the "affected" range
    private val currentlyAffectedPositions = mutableSetOf<Int>()

    // Auto-scroll
    private var isAutoScrolling = false
    private var autoScrollDirection = 0 // -1 = up, 1 = down, 0 = none

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Callback to check if an item at a position is currently selected
    var isSelectedProvider: ((Int) -> Boolean)? = null

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (!isDragActive || autoScrollDirection == 0) {
                isAutoScrolling = false
                return
            }
            recyclerView.scrollBy(0, autoScrollDirection * AUTO_SCROLL_SPEED_PX)
            // Re-evaluate drag position after scroll
            updateDragPosition(lastTouchX, lastTouchY)
            handler.postDelayed(this, AUTO_SCROLL_INTERVAL_MS)
        }
    }

    init {
        val density = recyclerView.context.resources.displayMetrics.density
        autoScrollEdgePx = AUTO_SCROLL_EDGE_DP * density
        minActivationDistancePx = MIN_ACTIVATION_DISTANCE_DP * density
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = e.x
                touchDownY = e.y
                touchDownPosition = getPositionUnder(e.x, e.y)
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragActive && touchDownPosition != RecyclerView.NO_POSITION) {
                    val dx = abs(e.x - touchDownX)
                    val dy = abs(e.y - touchDownY)
                    val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    // Only consider activation if we've moved enough
                    if (distance >= minActivationDistancePx) {
                        // Require horizontal-dominant movement to distinguish from vertical scroll.
                        // If movement is primarily vertical, let RecyclerView handle it as a scroll.
                        if (dx > dy * HORIZONTAL_BIAS_RATIO) {
                            val currentPosition = getPositionUnder(e.x, e.y)
                            if (currentPosition != RecyclerView.NO_POSITION &&
                                currentPosition != touchDownPosition) {
                                // The finger crossed into a different cell horizontally — activate drag-select
                                activateDrag(touchDownPosition, e.x, e.y)
                                return true
                            }
                        } else if (dy > dx * HORIZONTAL_BIAS_RATIO) {
                            // Movement is clearly vertical — abort drag detection for this gesture
                            touchDownPosition = RecyclerView.NO_POSITION
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragActive) {
                    endDrag()
                    return true
                }
                resetTouchState()
            }
        }

        return isDragActive
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        lastTouchX = e.x
        lastTouchY = e.y

        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (isDragActive) {
                    updateDragPosition(e.x, e.y)
                    updateAutoScroll(e.y)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragActive) {
                    endDrag()
                }
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // No-op
    }

    private fun activateDrag(anchorPos: Int, currentX: Float, currentY: Float) {
        isDragActive = true
        anchorPosition = anchorPos
        lastDragPosition = anchorPos
        originalStates.clear()
        currentlyAffectedPositions.clear()

        // Determine drag mode based on anchor item's current state
        val anchorIsSelected = isSelectedProvider?.invoke(anchorPos) ?: false
        dragModeIsSelect = !anchorIsSelected
        // If anchor is unselected, drag will SELECT items
        // If anchor is selected, drag will DESELECT items

        // Haptic feedback on activation
        recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Prevent RecyclerView from scrolling
        recyclerView.parent?.requestDisallowInterceptTouchEvent(true)

        // Apply to the anchor and current position
        val currentPosition = getPositionUnder(currentX, currentY)
        val endPos = if (currentPosition != RecyclerView.NO_POSITION) currentPosition else anchorPos
        applyDragToRange(anchorPos, endPos)
    }

    private fun updateDragPosition(x: Float, y: Float) {
        val currentPosition = getPositionUnder(x, y)
        if (currentPosition == RecyclerView.NO_POSITION) {
            return
        }

        if (currentPosition == lastDragPosition) {
            return
        }

        lastDragPosition = currentPosition
        applyDragToRange(anchorPosition, currentPosition)
    }

    /**
     * Applies the drag mode (select or deselect) to all items in the range [anchor, current].
     * Items that were previously affected but are now outside the range are restored
     * to their original state (reverse logic).
     */
    private fun applyDragToRange(anchor: Int, current: Int) {
        val rangeStart = min(anchor, current)
        val rangeEnd = max(anchor, current)
        val newRange = (rangeStart..rangeEnd).toSet()

        // Find positions that left the range (finger moved backward) — restore them
        val leftRange = currentlyAffectedPositions - newRange
        for (pos in leftRange) {
            val originalState = originalStates[pos]
            if (originalState != null) {
                // Restore to original state
                val currentState = isSelectedProvider?.invoke(pos) ?: false
                if (currentState != originalState) {
                    listener.onDragStateChanged(pos, originalState)
                }
            }
        }

        // Find positions that entered the range — apply drag mode
        val enteredRange = newRange - currentlyAffectedPositions
        for (pos in enteredRange) {
            // Save original state before modifying
            if (!originalStates.containsKey(pos)) {
                originalStates[pos] = isSelectedProvider?.invoke(pos) ?: false
            }
            // Apply drag mode
            listener.onDragStateChanged(pos, dragModeIsSelect)
        }

        currentlyAffectedPositions.clear()
        currentlyAffectedPositions.addAll(newRange)
    }

    private fun endDrag() {
        stopAutoScroll()
        listener.onDragSelectionFinished()
        resetState()
    }

    private fun resetState() {
        isDragActive = false
        anchorPosition = RecyclerView.NO_POSITION
        lastDragPosition = RecyclerView.NO_POSITION
        originalStates.clear()
        currentlyAffectedPositions.clear()
        recyclerView.parent?.requestDisallowInterceptTouchEvent(false)
        resetTouchState()
    }

    private fun resetTouchState() {
        touchDownPosition = RecyclerView.NO_POSITION
    }

    private fun updateAutoScroll(y: Float) {
        val height = recyclerView.height.toFloat()

        autoScrollDirection = when {
            y < autoScrollEdgePx -> -1 // Near top — scroll up
            y > height - autoScrollEdgePx -> 1 // Near bottom — scroll down
            else -> 0
        }

        if (autoScrollDirection != 0 && !isAutoScrolling) {
            isAutoScrolling = true
            handler.post(autoScrollRunnable)
        } else if (autoScrollDirection == 0) {
            stopAutoScroll()
        }
    }

    private fun stopAutoScroll() {
        isAutoScrolling = false
        autoScrollDirection = 0
        handler.removeCallbacks(autoScrollRunnable)
    }

    private fun getPositionUnder(x: Float, y: Float): Int {
        val child: View = recyclerView.findChildViewUnder(x, y) ?: return RecyclerView.NO_POSITION
        return recyclerView.getChildAdapterPosition(child)
    }
}
