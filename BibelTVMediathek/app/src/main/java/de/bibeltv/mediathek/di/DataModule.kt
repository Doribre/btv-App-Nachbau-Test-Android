package de.bibeltv.mediathek.di

import com.apollographql.apollo.ApolloClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.bibeltv.mediathek.BuildConfig
import de.bibeltv.mediathek.data.apollo.AuthHttpInterceptor
import de.bibeltv.mediathek.data.apollo.VideoHubTokenProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideApolloClient(tokenProvider: VideoHubTokenProvider): ApolloClient =
        ApolloClient.Builder()
            .serverUrl(BuildConfig.VIDEOHUB_GRAPHQL_URL)
            .addHttpHeader("apollographql-client-name", "btv-mediathek-android")
            .addHttpHeader("apollographql-client-version", BuildConfig.VERSION_NAME)
            .addHttpInterceptor(AuthHttpInterceptor(tokenProvider))
            .build()
}
