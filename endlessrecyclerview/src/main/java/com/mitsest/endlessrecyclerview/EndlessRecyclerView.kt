package com.mitsest.endlessrecyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference


@Retention(AnnotationRetention.SOURCE)
@IntDef(ScrollEndGravity.TOP, ScrollEndGravity.BOTTOM, ScrollEndGravity.LEFT, ScrollEndGravity.RIGHT)
annotation class ScrollEndGravity {
    companion object {
        const val LEFT = 0
        const val TOP = 1
        const val RIGHT = 2
        const val BOTTOM = 3
    }
}

interface EndlessRecyclerViewOnScrollListenerImpl {
    val scrollEndGravity: Int

    fun canScrollVertically(direction: Int): Boolean
    fun canScrollHorizontally(direction: Int): Boolean

    fun onScrollListenerImplScrollEnd()

    fun processScrollStateChanged() {
        when (scrollEndGravity) {
            ScrollEndGravity.BOTTOM -> if (!canScrollVertically(1)) onScrollListenerImplScrollEnd()

            ScrollEndGravity.TOP -> if (!canScrollVertically(-1)) onScrollListenerImplScrollEnd()

            ScrollEndGravity.RIGHT -> if (!canScrollHorizontally(1)) onScrollListenerImplScrollEnd()

            ScrollEndGravity.LEFT -> if (!canScrollHorizontally(-1)) onScrollListenerImplScrollEnd()
        }
    }
}

class EndlessRecyclerViewOnScrollListener(
    private val impl: EndlessRecyclerViewOnScrollListenerImpl
) :
    RecyclerView.OnScrollListener() {


    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        if (newState != RecyclerView.SCROLL_STATE_IDLE) return

        impl.processScrollStateChanged()
    }
}

class NestedScrollViewOnScrollListener(
    private val impl: EndlessRecyclerViewOnScrollListenerImpl
) :
    NestedScrollView.OnScrollChangeListener {
    override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        if (impl.scrollEndGravity == ScrollEndGravity.TOP && scrollY == 0) {
            impl.processScrollStateChanged()
            return
        }

        v?.run {
            val childMeasuredHeight = getChildAt(0)?.measuredHeight ?: return@run

            if (impl.scrollEndGravity == ScrollEndGravity.BOTTOM && scrollY == Math.abs(measuredHeight - childMeasuredHeight)) {
                impl.processScrollStateChanged()
            }
        }
    }

}

class EndlessRecyclerView : RecyclerView,
    EndlessRecyclerViewOnScrollListenerImpl {

    companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_SCROLL_END_GRAVITY = ScrollEndGravity.BOTTOM
        const val DEFAULT_PROGRESS_BAR_VIEW_ID = -1
    }

    @IdRes
    private var progressBarViewId = DEFAULT_PROGRESS_BAR_VIEW_ID

    var page: Int = DEFAULT_PAGE
    override var scrollEndGravity: Int = DEFAULT_SCROLL_END_GRAVITY

    // Views
    private var nestedScrollViewParent: NestedScrollView? = null
    private var progressBar: View? = null
    private val recyclerViewOnScrollListener by lazy { EndlessRecyclerViewOnScrollListener(this) }
    private val nestedScrollViewOnScrollListener by lazy { NestedScrollViewOnScrollListener(this) }

    var scrollEndListener: ScrollEndListener? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        initArgs(context, attrs, defStyleAttr)
        addOnScrollListener(recyclerViewOnScrollListener)

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setNestedScrollViewParent()
        addNestedScrollViewOnScrollListener()
        addProgressBar()
    }

    private fun setNestedScrollViewParent() {
        var mParent = parent
        while (mParent != null) {

            if (mParent is NestedScrollView) {
                nestedScrollViewParent = mParent
                break
            }

            mParent = mParent.parent
        }
    }

    private fun addNestedScrollViewOnScrollListener() {
        nestedScrollViewParent?.run {
            setOnScrollChangeListener(nestedScrollViewOnScrollListener)
        }
    }


    private fun addProgressBar() {
        if (progressBarViewId != DEFAULT_PROGRESS_BAR_VIEW_ID) {
            rootView?.findViewById<View>(progressBarViewId)?.run {
                progressBar = this
            }
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return super.canScrollHorizontally(direction)
                || nestedScrollViewParent?.canScrollHorizontally(direction) == true

    }

    override fun canScrollVertically(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return super.canScrollVertically(direction)
                || nestedScrollViewParent?.canScrollVertically(direction) == true
    }

    override fun onScrollListenerImplScrollEnd() {
        if (isProgressBarVisible()) return

        // Notify context through recycler view's adapter
        scrollEndListener?.onScrollEnd(++page)
    }


    // XML arguments initialization logic
    private fun initArgs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        attrs ?: return

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.EndlessRecyclerView, defStyleAttr, 0)

        try {
            scrollEndGravity = a.getInt(R.styleable.EndlessRecyclerView_scrollEndGravity, DEFAULT_SCROLL_END_GRAVITY)
            progressBarViewId =
                a.getResourceId(R.styleable.EndlessRecyclerView_progressBarViewId, DEFAULT_PROGRESS_BAR_VIEW_ID)

            if (progressBarViewId == DEFAULT_PROGRESS_BAR_VIEW_ID) {
                throw NotImplementedError("This view does not work without a progress bar. Please add an app:progressBarViewId property to your EndlessRecyclerView XML element")
            }

        } finally {
            a.recycle()
        }
    }

    private fun isProgressBarVisible() = progressBar?.visibility == View.VISIBLE

    private fun showProgress() {
        progressBar?.visibility = View.VISIBLE
    }


    interface ScrollEndListener {
        fun onScrollEnd(page: Int)
    }

}
