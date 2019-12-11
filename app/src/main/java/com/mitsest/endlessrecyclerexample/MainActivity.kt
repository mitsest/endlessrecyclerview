package com.mitsest.endlessrecyclerexample

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.mitsest.endlessrecyclerview.EndlessRecyclerView
import dagger.android.DaggerActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

interface MainActivityPersonsListScrollEndListener : EndlessRecyclerView.ScrollEndListener {
    override fun onScrollEnd(page: Int) {
        onPersonsListScrollEnd(page)
    }

    fun onPersonsListScrollEnd(page: Int)
}

class MainActivity : DaggerActivity(), MainActivityPersonsListScrollEndListener {
    @Inject
    lateinit var personApiClient: PersonApiClient

    private val personsListAdapter by lazy { PersonsListAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        activity_main_persons_list?.adapter = personsListAdapter
        activity_main_persons_list?.setScrollEndListener(this)
        request(page = 1)
    }

    override fun onPersonsListScrollEnd(page: Int) {
        request(page)
    }

    @SuppressLint("CheckResult")
    private fun request(page: Int) {

            activity_main_progress_bar.visibility = View.VISIBLE

            personApiClient.getPersons(page)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableSingleObserver<PersonListResponse>() {
                    override fun onSuccess(t: PersonListResponse) {
                        Handler().postDelayed({

                            onRequestSuccess(t)
                            dispose()
                        }, 1500)

                    }

                    override fun onError(e: Throwable) {
                        onRequestError(e)
                        dispose()
                    }

                })

    }

    private fun onRequestSuccess(t: PersonListResponse) {
        personsListAdapter.addPersons(t.results)
        activity_main_progress_bar.visibility = View.GONE
    }

    private fun onRequestError(e: Throwable) {
        activity_main_progress_bar.visibility = View.GONE
    }
}
