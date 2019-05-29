package com.mitsest.endlessrecyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

internal class EndlessRecyclerViewRV(context: Context, attrs: AttributeSet?, defStyle: Int) :
    RecyclerView(context, attrs, defStyle) {
    var adapter: EndlessRecyclerViewAdapter<out ViewHolder>? = null
        set(value) {
            field = value
            setAdapter(field)
        }
}

// Make your adapters extend this one
abstract class EndlessRecyclerViewAdapter<VH : RecyclerView.ViewHolder>(open val scrollEndListener: ScrollEndListener? = null) :
    RecyclerView.Adapter<VH>() {

    var requestErrorListener: RequestErrorListener? = null

    fun onRequestError(message: String? = null) {
        requestErrorListener?.onRequestError(message)
    }

    interface ScrollEndListener {
        fun onScrollEnd(page: Int)
    }

    interface RequestErrorListener {
        fun onRequestError(message: String? = null)
    }
}
