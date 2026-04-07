package com.example.kmp_basic_app.data.remote

import com.apollographql.apollo.ApolloClient

fun createGraphQLZeroApolloClient(): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("https://graphqlzero.almansi.me/api")
        .build()
}
