package com.vvechirko.livadatatest

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

typealias Data<T> = (data: T?) -> Unit
typealias DataPage<T> = (data: T?, page: Int) -> Unit
typealias Error = (error: String) -> Unit
typealias Loading = (b: Boolean) -> Unit
typealias Empty = () -> Unit
typealias Dispose = ((Disposable) -> Unit)

const val PAGE_START = 1
const val PAGE_NONE = -1

enum class Status {
    SUCCESS,
    LOADING,
    ERROR
}

fun ErrorParser(t: Throwable?) = t?.message ?: t.toString()


class PagingResponseData<T> : LiveData<Response<List<T>>>() {

    var currentPage: Int = PAGE_NONE
    val allItems: MutableList<T> = mutableListOf()

    val isEmpty: Boolean
        get() = currentPage == PAGE_NONE

    fun observeResponse(owner: LifecycleOwner, success: DataPage<List<T>>? = null, error: Error? = null, loading: Loading? = null) {
        removeObservers(owner)
        if (currentPage != PAGE_NONE) {
            value = Response.success(allItems, currentPage)
        }

        observe(owner, Observer<Response<List<T>>> {
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

    fun from(observable: Observable<List<T>>, page: Int, dispose: Dispose? = null) {
        observable
                .doOnSubscribe { value = Response.loading() }
                .subscribe(
                        { data -> value = Response.success(data, page) },
                        { t -> value = Response.error(t) }
//                        { t -> postValue(Response.error(ErrorParser.parse(t))) }
                ).also { dispose?.invoke(it) }
    }
}


/**     Response<T> for Observable<T>
 *
 *      ViewModel:
 *      val data: ResponseData<String> = ResponseData()
 *      data.adapt(Observable.just("ololo"))
 *
 *      Activity/Fragment:
 *      data.observeResponse(this,
 *          { data -> ... },
 *          { error -> ... },
 *          { b -> ... }
 *      )
 */

class Response<T>(val status: Status, val data: T? = null, val page: Int = PAGE_NONE, val error: Throwable? = null) {
    companion object {
        fun <T> success(data: T?, page: Int = PAGE_NONE) = Response<T>(Status.SUCCESS, data, page)

        fun <T> error(error: Throwable?) = Response<T>(Status.ERROR, error = error)

        fun <T> loading() = Response<T>(Status.LOADING)
    }
}

class ResponseObserver<T>(val success: Data<T>? = null, val error: Error? = null, val loading: Loading? = null) : Observer<Response<T>> {

    override fun onChanged(t: Response<T>?) {
        when (t?.status) {
            Status.SUCCESS -> {
                loading?.invoke(false)
                success?.invoke(t.data)
            }
            Status.ERROR -> {
                loading?.invoke(false)
                error?.invoke(ErrorParser(t.error))
            }
            Status.LOADING -> loading?.invoke(true)
        }
    }
}

class ResponseData<T> : LiveData<Response<T>>() {

    fun observeResponse(owner: LifecycleOwner, success: Data<T>? = null, error: Error? = null, loading: Loading? = null) {
        observe(owner, ResponseObserver(success, error, loading))
    }

    fun from(observable: Observable<T>, dispose: Dispose? = null) {
        observable
                .doOnSubscribe { value = Response.loading() }
                .subscribe(
                        { data -> value = Response.success(data) },
                        { t -> value = Response.error(t) }
//                        { t -> postValue(Response.error(ErrorParser.parse(t))) }
                ).also { dispose?.invoke(it) }
    }
}


/**     Action for Completable
 *
 *      ViewModel:
 *      val data: ActionData = ActionData()
 *      data.adapt(Completable.complete())
 *
 *      Activity/Fragment:
 *      data.observeAction(this,
 *          { ... },
 *          { error -> ... },
 *          { b -> ... }
 *      )
 */

class Action(val status: Status, val error: Throwable? = null) {
    companion object {
        fun success() = Action(Status.SUCCESS)

        fun error(error: Throwable?) = Action(Status.ERROR, error)

        fun loading() = Action(Status.LOADING)
    }
}

class ActionObserver(val success: Empty? = null, val error: Error? = null, val loading: Loading? = null) : Observer<Action> {

    override fun onChanged(t: Action?) {
        when (t?.status) {
            Status.SUCCESS -> {
                loading?.invoke(false)
                success?.invoke()
            }
            Status.ERROR -> {
                loading?.invoke(false)
                error?.invoke(ErrorParser(t.error))
            }
            Status.LOADING -> loading?.invoke(true)
        }
    }
}

class ActionData : LiveData<Action>() {

    fun observeAction(owner: LifecycleOwner, success: Empty? = null, error: Error? = null, loading: Loading? = null) {
        observe(owner, ActionObserver(success, error, loading))
    }

    fun from(completable: Completable, dispose: Dispose? = null, resettable: Boolean = true) {
        completable
                .doOnSubscribe { value = Action.loading() }
                .doAfterTerminate { if (resettable) value = null }
                .subscribe(
                        { value = Action.success() },
                        { t -> value = Action.error(t) }
//                        { t -> postValue(Action.error(ErrorParser.parse(t))) }
                ).also { dispose?.invoke(it) }
    }
}