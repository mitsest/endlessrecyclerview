# Endless Recycler View

An endless recycler view that also support NestedScrollView parent

To use it

```xml

<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    tools:context=".MainActivity">

    <com.mitsest.endlessrecyclerview.EndlessRecyclerView
        android:id="@+id/activity_main_persons_list"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
        app:progressBarViewId="@+id/activity_main_progress_bar"
        app:scrollEndGravity="bottom"
        app:spanCount="2" />

    <ProgressBar
        android:id="@+id/activity_main_progress_bar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />
</FrameLayout>

```

A progress bar or a swipe refresh layout is required, in  order for the custom view to check if it's already fetching data. It's your obligation to show/hide the progressbar.

app:scrollEndGravity can be bottom, right, left and top
