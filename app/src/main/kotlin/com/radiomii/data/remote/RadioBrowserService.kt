package com.radiomii.data.remote

import com.radiomii.data.remote.dto.CountryDto
import com.radiomii.data.remote.dto.StationDto
import com.radiomii.data.remote.dto.VoteResultDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RadioBrowserService {

    @FormUrlEncoded
    @POST("json/stations/search")
    suspend fun searchStations(
        @Field("name") name: String? = null,
        @Field("tag") tag: String? = null,
        @Field("countrycode") countrycode: String? = null,
        @Field("order") order: String = "clickcount",
        @Field("reverse") reverse: Boolean = true,
        @Field("hidebroken") hidebroken: Boolean = true,
        @Field("is_https") isHttps: String? = null,
        @Field("bitrateMin") bitrateMin: Int = 0,
        @Field("limit") limit: Int = 50,
        @Field("offset") offset: Int = 0,
    ): List<StationDto>

    @GET("json/countries")
    suspend fun getCountries(): List<CountryDto>

    @Suppress("unused")
    @GET("json/url/{uuid}")
    suspend fun logStationClick(@Path("uuid") uuid: String)

    @GET("json/vote/{uuid}")
    suspend fun voteForStation(@Path("uuid") uuid: String): VoteResultDto
}
