package de.bibeltv.mediathek.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.apollographql.apollo.ApolloClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
            // Quell-Kennung für den Server ("woher kommt es") – exakt inkl. Bindestrich.
            .addHttpHeader("apollographql-client-name", "bre-testapp")
            .addHttpHeader("apollographql-client-version", BuildConfig.VERSION_NAME)
            .addHttpInterceptor(AuthHttpInterceptor(tokenProvider))
            .build()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("btv_settings")
        }

    // Bibelthek-HTTP: bewusst OHNE Cache (kein .cache(...)) – der Bibeltext darf nicht persistiert werden.
    @Provides
    @Singleton
    @BibleHttpClient
    fun provideBibleHttpClient(): okhttp3.OkHttpClient =
        okhttp3.OkHttpClient.Builder().build()
}
