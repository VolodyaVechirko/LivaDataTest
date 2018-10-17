package com.vvechirko.livadatatest

import com.google.gson.GsonBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object Interactor {

    val api = ApiService.create(Api::class.java)

    fun getUsers(): Observable<List<UserEntity>> {
        return api.getUsers()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun getPosts(userId: Int): Observable<List<PostEntity>> {
        return api.getPosts(userId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}

object ApiService {

    const val API_ENDPOINT = "https://jsonplaceholder.typicode.com/"
    const val DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ"

    fun <S> create(clazz: Class<S>) = Retrofit.Builder()
        .baseUrl(API_ENDPOINT)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build()
        )
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(
            GsonConverterFactory.create(
                GsonBuilder()
                    .setDateFormat(DATE_FORMAT)
                    .create()
            )
        ).build()
        .create(clazz)
}

interface Api {

    @GET("users")
    fun getUsers(): Observable<List<UserEntity>>

    @GET("posts")
    fun getPosts(@Query("userId") userId: Int): Observable<List<PostEntity>>
}

data class PostEntity(
    var id: String,
    var title: String,
    var body: String,
    var userId: String
)

data class UserEntity(
    var id: String,
    var name: String,
    var username: String,
    var phone: String? = null,
    var website: String? = null
)