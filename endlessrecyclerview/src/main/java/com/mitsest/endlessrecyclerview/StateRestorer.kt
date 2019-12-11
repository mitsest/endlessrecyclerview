/**
 * As seen on https://github.com/saket/InboxRecyclerView/blob/master/inboxrecyclerview/src/main/java/me/saket/inboxrecyclerview/StateRestorer.kt
 */
package com.mitsest.endlessrecyclerview

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

internal class StateRestorer(private val recyclerView: EndlessRecyclerView) {

    var page: Int = EndlessRecyclerView.DEFAULT_PAGE

    internal fun save(outState: Parcelable): Parcelable {
        return SavedState(outState, recyclerView.page)
    }

    internal fun restore(inState: Parcelable): Parcelable {
        val savedState = inState as SavedState
        page = savedState.page
        restoreIfPossible()
        return savedState.superState
    }

    private fun restoreIfPossible() {
        recyclerView.page = page
    }
}

@Parcelize
data class SavedState(
    val superState: Parcelable,
    val page: Int
) : Parcelable