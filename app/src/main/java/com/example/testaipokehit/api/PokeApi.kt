package com.example.testaipokehit.api

import com.example.testaipokehit.api.dto.PokemonDto
import com.example.testaipokehit.api.dto.PokemonListResponse
import com.example.testaipokehit.api.dto.SpeciesDto
import com.example.testaipokehit.api.dto.TypeDetailDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PokeApi {

    @GET("pokemon")
    suspend fun getPokemonPage(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): PokemonListResponse

    @GET("pokemon/{id}")
    suspend fun getPokemon(@Path("id") id: Int): PokemonDto

    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(@Path("id") id: Int): SpeciesDto

    @GET("type/{name}")
    suspend fun getType(@Path("name") name: String): TypeDetailDto
}