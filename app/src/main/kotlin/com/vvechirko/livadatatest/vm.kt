package com.vvechirko.livadatatest

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

typealias Data<T> = (data: T?) -> Unit
typealias DataPage<T> = (data: T?, page: Int) -> Unit
typealias Error = (error: String) -> Unit
typealias Loading = (b: Boolean) -> Unit
typealias Empty = () -> Unit

const val PAGE_START = 1
const val PAGE_NONE = -1

enum class Status {
    SUCCESS,
    LOADING,
    ERROR
}

fun ErrorParser(t: Throwable?) = t?.message ?: t.toString()


/**     Response<T> for rx.Observable<T> + Pagination
 *
 *      ViewModel:
 *      val data: PagingResponseData<String> = PagingResponseData()
 *      data.from(Observable.just("ololo"), page)
 *
 *      Activity/Fragment:
 *      data.observeResponse(this,
 *          { data, page -> ... },
 *          { error -> ... },
 *          { b -> ... }
 *      )
 */

class PagingResponseData<T> : LiveData<Response<List<T>>>() {

    val disposable = CompositeDisposable()

    var currentPage: Int = PAGE_NONE
    val allItems: MutableList<T> = mutableListOf()

    val isEmpty: Boolean
        get() = currentPage == PAGE_NONE

    fun observeResponse(owner: LifecycleOwner, success: DataPage<List<T>>? = null, error: Error? = null, loading: Loading? = null) {
        val viewLifecycleOwner = (owner as? Fragment)?.viewLifecycleOwner ?: owner

        if (currentPage != PAGE_NONE) {
            value = Response.success(allItems, currentPage)
        }

        observe(viewLifecycleOwner, Observer<Response<List<T>>> {
            when (it?.status) {
                Status.SUCCESS -> {
                    if (it.page != currentPage) {
                        currentPage = it.page
                        if (currentPage == PAGE_START) allItems.clear()
                        allItems.addAll(it.data ?: emptyList())
                    }

                    loading?.invoke(false)
                    success?.invoke(it.data, it.page)
                }
                Status.ERROR -> {
                    loading?.invoke(false)
                    error?.invoke(ErrorParser(it.error))
                }
                Status.LOADING -> loading?.invoke(true)
            }
        })
    }

    override fun onInactive() {
        disposable.clear()
    }

    fun from(observable: Observable<List<T>>, page: Int) {
        observable
            .doOnSubscribe { value = Response.loading() }
            .subscribe(
                { data -> value = Response.success(data, page) },
                { t -> value = Response.error(t) }
//                        { t -> postValue(Response.error(ErrorParser.parse(t))) }
            ).also { disposable.add(it) }
    }
}


/**     Response<T> for rx.Observable<T>
 *
 *      ViewModel:
 *      val data: ResponseData<String> = ResponseData()
 *      data.from(Observable.just("ololo"))
 *
 *      Activity/Fragment:
 *      data.observeResponse(this,
 *          { data -> ... },
 *          { error -> ... },
 *          { b -> ... }
 *      )
 */

class ResponseData<T> : LiveData<Response<T>>() {

    val disposable = CompositeDisposable()

    fun observeResponse(owner: LifecycleOwner, success: Data<T>? = null, error: Error? = null, loading: Loading? = null) {
        val viewLifecycleOwner = (owner as? Fragment)?.viewLifecycleOwner ?: owner

        observe(viewLifecycleOwner, Observer {
            when (it?.status) {
                Status.SUCCESS -> {
                    loading?.invoke(false)
                    success?.invoke(it.data)
                }
                Status.ERROR -> {
                    loading?.invoke(false)
                    error?.invoke(ErrorParser(it.error))
                }
                Status.LOADING -> loading?.invoke(true)
            }
        })
    }

    fun from(observable: Observable<T>) {
        observable
            .doOnSubscribe { value = Response.loading() }
            .subscribe(
                { data -> value = Response.success(data) },
                { t -> value = Response.error(t) }
//                        { t -> postValue(Response.error(ErrorParser.parse(t))) }
            ).also { disposable.add(it) }
    }

    override fun onInactive() {
        disposable.clear()
    }
}


/**     ActionData for rx.Completable
 *
 *      ViewModel:
 *      val data: ActionData = ActionData()
 *      data.from(Completable.complete())
 *
 *      Activity/Fragment:
 *      data.observeAction(this,
 *          { ... },
 *          { error -> ... },
 *          { b -> ... }
 *      )
 */

class ActionData : LiveData<Action>() {

    val disposable = CompositeDisposable()

    fun observeAction(owner: LifecycleOwner, success: Empty? = null, error: Error? = null, loading: Loading? = null) {
        val viewLifecycleOwner = (owner as? Fragment)?.viewLifecycleOwner ?: owner

        observe(viewLifecycleOwner, Observer {
            when (it?.status) {
                Status.SUCCESS -> {
                    loading?.invoke(false)
                    success?.invoke()
                }
                Status.ERROR -> {
                    loading?.invoke(false)
                    error?.invoke(ErrorParser(it.error))
                }
                Status.LOADING -> loading?.invoke(true)
            }
        })
    }

    override fun onInactive() {
        disposable.clear()
    }

    fun from(completable: Completable, resettable: Boolean = true) {
        completable
            .doOnSubscribe { value = Action.loading() }
            .doAfterTerminate { if (resettable) value = null }
            .subscribe(
                { value = Action.success() },
                { t -> value = Action.error(t) }
//                        { t -> postValue(Action.error(ErrorParser.parse(t))) }
            ).also { disposable.add(it) }
    }
}


/**
 *      Response<T> for Observable<T> + Pagination
 */

class Response<T>(val status: Status, val data: T? = null, val page: Int = PAGE_NONE, val error: Throwable? = null) {
    companion object {
        fun <T> success(data: T?, page: Int = PAGE_NONE) = Response<T>(Status.SUCCESS, data, page)

        fun <T> error(error: Throwable?) = Response<T>(Status.ERROR, error = error)

        fun <T> loading() = Response<T>(Status.LOADING)
    }
}


/**
 *      Action for Completable
 */

class Action(val status: Status, val error: Throwable? = null) {
    companion object {
        fun success() = Action(Status.SUCCESS)

        fun error(error: Throwable?) = Action(Status.ERROR, error)

        fun loading() = Action(Status.LOADING)
    }
}