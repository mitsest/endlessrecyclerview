package com.mitsest.endlessrecyclerview

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.IntDef
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.lang.ref.WeakReference
import kotlin.math.abs


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

class RecyclerViewOnScrollListener(
    impl: EndlessRecyclerViewOnScrollListenerImpl?
) :
    RecyclerView.OnScrollListener() {

    private val impl = WeakReference(impl)

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)

        val mImpl = impl.get()
        mImpl ?: return

        if (newState != RecyclerView.SCROLL_STATE_IDLE) return

        mImpl.processScrollStateChanged()
    }
}

class NestedScrollViewOnScrollListener(
    impl: EndlessRecyclerViewOnScrollListenerImpl
) :
    NestedScrollView.OnScrollChangeListener {

    private val impl = WeakReference(impl)

    override fun onScrollChange(v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
        val mImpl = impl.get()
        mImpl ?: return

        if (mImpl.scrollEndGravity == ScrollEndGravity.TOP && scrollY == 0) {
            mImpl.processScrollStateChanged()
            return
        }

        v?.run {
            val childMeasuredHeight = getChildAt(0)?.measuredHeight ?: return@run

            if (mImpl.scrollEndGravity == ScrollEndGravity.BOTTOM && scrollY == abs(measuredHeight - childMeasuredHeight)) {
                mImpl.processScrollStateChanged()
            }
        }
    }

}

class EndlessRecyclerView : RecyclerView,
    EndlessRecyclerViewOnScrollListenerImpl {

    companion object {
        const val DEFAULT_PAGE = 1
        const val DEFAULT_SCROLL_END_GRAVITY = ScrollEndGravity.BOTTOM
        const val DEFAULT_PROGRESS_BAR_VIEW_ID = View.NO_ID
        const val DEFAULT_SWIPE_TO_REFRESH_VIEW_ID = View.NO_ID
        const val DEFAULT_TOTAL_PAGES = -1
    }

    @IdRes
    private var progressBarViewId = DEFAULT_PROGRESS_BAR_VIEW_ID

    @IdRes
    private var swipeToRefreshViewId = DEFAULT_SWIPE_TO_REFRESH_VIEW_ID

    var page: Int = DEFAULT_PAGE

    // Update this variable to support canceling requests when totalPages is reached
    var totalPages: Int = DEFAULT_TOTAL_PAGES

    override var scrollEndGravity: Int = DEFAULT_SCROLL_END_GRAVITY

    private val restorer = StateRestorer(this)

    private var nestedScrollViewParent: WeakReference<NestedScrollView>? = null
    private var progressBar: WeakReference<View>? = null
    private var swipeRefreshLayout: WeakReference<SwipeRefreshLayout>? = null
    private var recyclerViewOnScrollListener = RecyclerViewOnScrollListener(this)
    private val nestedScrollViewOnScrollListener = NestedScrollViewOnScrollListener(this)

    private var scrollEndListener: WeakReference<ScrollEndListener>? = null

    fun setScrollEndListener(scrollEndListener: ScrollEndListener) {
        this.scrollEndListener = WeakReference(scrollEndListener)
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, -1)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        initArgs(context, attrs, defStyleAttr)
        addOnScrollListener(recyclerViewOnScrollListener)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val state = super.onSaveInstanceState()
        state?.run {
            return restorer.save(state)
        }

        return state
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val superState = restorer.restore(state)
        super.onRestoreInstanceState(superState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setNestedScrollViewParent()
        addNestedScrollViewOnScrollListener()
        addProgressBar()
        addSwipeRefreshLayout()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        adapter = null
    }

    private fun setNestedScrollViewParent() {
        var mParent = parent
        while (mParent != null) {

            if (mParent is NestedScrollView) {
                nestedScrollViewParent = WeakReference(mParent)
                break
            }

            mParent = mParent.parent
        }
    }

    private fun addNestedScrollViewOnScrollListener() {
        nestedScrollViewParent?.get()?.run {
            setOnScrollChangeListener(nestedScrollViewOnScrollListener)
        }
    }


    private fun addProgressBar() {
        if (progressBarViewId != DEFAULT_PROGRESS_BAR_VIEW_ID) {
            rootView?.findViewById<View>(progressBarViewId)?.run {
                progressBar = WeakReference(this)
            }
        }
    }

    private fun addSwipeRefreshLayout() {
        if (swipeToRefreshViewId != DEFAULT_SWIPE_TO_REFRESH_VIEW_ID) {
            rootView?.findViewById<SwipeRefreshLayout>(swipeToRefreshViewId)?.run {
                swipeRefreshLayout = WeakReference(this)
            }
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return super.canScrollHorizontally(direction)
                || nestedScrollViewParent?.get()?.canScrollHorizontally(direction) == true

    }

    override fun canScrollVertically(direction: Int): Boolean {
        val itemCount = adapter?.itemCount ?: 0
        if (itemCount == 0) return true

        return super.canScrollVertically(direction)
                || nestedScrollViewParent?.get()?.canScrollVertically(direction) == true
    }

    override fun onScrollListenerImplScrollEnd() {
        if ((totalPages != DEFAULT_TOTAL_PAGES && page >= totalPages) || isProgressBarVisible() || swipeLayoutRefreshing()) {
            return
        }

        scrollEndListener?.get()?.onScrollEnd(++page)
    }


    private fun initArgs(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        attrs ?: return

        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.EndlessRecyclerView, defStyleAttr, 0)

        try {
            scrollEndGravity = a.getInt(R.styleable.EndlessRecyclerView_scrollEndGravity, DEFAULT_SCROLL_END_GRAVITY)

            progressBarViewId =
                a.getResourceId(R.styleable.EndlessRecyclerView_progressBarViewId, DEFAULT_PROGRESS_BAR_VIEW_ID)

            swipeToRefreshViewId =
                a.getResourceId(
                    R.styleable.EndlessRecyclerView_swipeRefreshLayoutId,
                    DEFAULT_SWIPE_TO_REFRESH_VIEW_ID
                )

            if (progressBarViewId == DEFAULT_PROGRESS_BAR_VIEW_ID && swipeToRefreshViewId == DEFAULT_SWIPE_TO_REFRESH_VIEW_ID) {
                throw NotImplementedError(
                    "This view does not work without a progress bar or SwipeRefreshLayout." +
                            "Please add an app:progressBarViewId or app:swipeRefreshLayoutId property to your EndlessRecyclerView XML element"
                )
            }

        } finally {
            a.recycle()
        }
    }

    private fun isProgressBarVisible() = progressBar?.get()?.visibility == View.VISIBLE

    private fun swipeLayoutRefreshing() = swipeRefreshLayout?.get()?.isRefreshing == true

    interface ScrollEndListener {
        fun onScrollEnd(page: Int)
    }

}
