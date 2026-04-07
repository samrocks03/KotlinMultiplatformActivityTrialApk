package com.example.kmp_basic_app.data.remote

import com.apollographql.apollo.ApolloClient

fun createRickAndMortyApolloClient(): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("https://rickandmortyapi.com/graphql")
        .build()
}
