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

            if (impl.scrollEndGravity == ScrollEndGravity.BOTTOM && scrollY == measuredHeight - childMeasuredHeight) {
                impl.processScrollStateChanged()
            }
        }
    }

}

class ProgressBarAdapterDataObserver(progressBar: View?) : RecyclerView.AdapterDataObserver() {

    private var progressBarWeakReference = WeakReference(progressBar)
    var firstPass: Boolean = false // avoid race condition. In case of nested scroll view parent this observer is attached onAttachedToWindow and not on init

    override fun onChanged() {
        super.onChanged()
        firstPass = true
        hideProgress()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        super.onItemRangeChanged(positionStart, itemCount)
        firstPass = true
        hideProgress()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        super.onItemRangeInserted(positionStart, itemCount)
        firstPass = true
        hideProgress()
    }

    private fun hideProgress() {
        progressBarWeakReference.get()?.visibility = View.GONE
    }

    fun setProgressBar(progressBar: View?) {
        progressBarWeakReference = WeakReference(progressBar)
    }

}

class EndlessRecyclerView : FrameLayout,
    EndlessRecyclerViewOnScrollListenerImpl,
    EndlessRecyclerViewAdapter.RequestErrorListener {

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
    private val recyclerView: EndlessRecyclerViewRV
    private var nestedScrollViewParent: NestedScrollView? = null
    private var progressBar: View? = null

    // Observer for notifyDataSetChanged() etc. events
    private val recyclerViewDataObserver by lazy { ProgressBarAdapterDataObserver(progressBar) }
    private val recyclerViewOnScrollListener by lazy { EndlessRecyclerViewOnScrollListener(this) }
    private val nestedScrollViewOnScrollListener by lazy { NestedScrollViewOnScrollListener(this) }

    // Delegate adapter attribute to recycler view
    var adapter: EndlessRecyclerViewAdapter<out RecyclerView.ViewHolder>?
        set(value) {
            recyclerView.adapter = value
            /**
             * Assign self as request error listener
             * The consumer is tasked to notify this view , in case of errors through adapter's onRequestError
             * @see EndlessRecyclerViewAdapter.onRequestError
             **/
            recyclerView.adapter?.requestErrorListener = this
            recyclerView.adapter?.registerAdapterDataObserver(recyclerViewDataObserver)
        }
        get() = recyclerView.adapter

    var layoutManager: RecyclerView.LayoutManager?
        set(value) {
            recyclerView.layoutManager = value
        }
        get() = recyclerView.layoutManager

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        initArgs(context, attrs, defStyleAttr)

        // Create an EndlessRecyclerViewRV, passing down arguments like orientation and layoutManager
        recyclerView = EndlessRecyclerViewRV(context, attrs, defStyleAttr)
            .apply {
                // Change nothing but the id of the view to avoid IllegalArgumentExceptions on restore instance state
                id = View.generateViewId()
            }
            .apply {
                addOnScrollListener(recyclerViewOnScrollListener)
            }

        addRecyclerView()
    }

    // Add recycler and change its height to match_parent
    private fun addRecyclerView() {
        addView(recyclerView)
        recyclerView.run {
            val newLayoutParams = LayoutParams(layoutParams)
            newLayoutParams.height = LayoutParams.MATCH_PARENT
            layoutParams = newLayoutParams
        }
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


    private fun removeRecyclerViewOnScrollListener() {
        recyclerView.removeOnScrollListener(recyclerViewOnScrollListener)
    }

    private fun addProgressBar() {
        if (progressBarViewId != DEFAULT_PROGRESS_BAR_VIEW_ID) {
            rootView?.findViewById<View>(progressBarViewId)?.run {
                progressBar = this
                recyclerViewDataObserver.setProgressBar(progressBar)
                if (recyclerViewDataObserver.firstPass) {
                    hideProgress()
                }
            }
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return recyclerView.canScrollHorizontally(direction)
                || nestedScrollViewParent?.canScrollHorizontally(direction) == true

    }

    override fun canScrollVertically(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return recyclerView.canScrollVertically(direction)
                || nestedScrollViewParent?.canScrollVertically(direction) == true
    }

    override fun onScrollListenerImplScrollEnd() {
        if (isProgressBarVisible()) return

        showProgress()

        // Notify context through recycler view's adapter
        recyclerView.adapter?.scrollEndListener?.onScrollEnd(++page)
    }

    override fun onRequestError(message: String?) {
        hideProgress()
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

    private fun hideProgress() {
        progressBar?.visibility = View.GONE
    }
}
