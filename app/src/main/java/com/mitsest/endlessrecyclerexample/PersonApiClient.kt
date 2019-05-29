package com.mitsest.endlessrecyclerexample

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
open class ApiResponse(
    @field:Json(name = "count")
    open val count: Int,

    @field:Json(name = "next")
    open val next: String?,

    @field:Json(name = "previous")
    open val previous: String?
)


@JsonClass(generateAdapter = true)
data class PersonListResponse(
    override val count: Int, override val next: String?, override val previous: String?,
    @field:Json(name = "results")
    val results: List<Person>
) : ApiResponse(count, next, previous)

@JsonClass(generateAdapter = true)
data class Person(
    @field:Json(name = "first_name")
    val firstName: String,

    @field:Json(name = "last_name")
    val lastName: String,

    @field:Json(name = "favorite_number")
    val favoriteNumber: String
)

interface PersonApiClient {
    @GET("persons")
    fun getPersons(@Query("page") page: Int): Single<PersonListResponse>
}