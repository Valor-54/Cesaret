package com.example.service

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView

/**
 * Custom FrameLayout hosting the ComposeView for the system overlay editor.
 */
class OverlayContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val composeView = ComposeView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    var touchManager: OverlayTouchManager? = null

    init {
        addView(composeView)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        touchManager?.attach()
        touchManager?.requestUpdate()
    }

    override fun onDetachedFromWindow() {
        touchManager?.detach()
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        touchManager?.requestUpdate()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return super.dispatchTouchEvent(ev)
    }
}
