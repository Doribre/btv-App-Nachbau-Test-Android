package de.bibeltv.mediathek.di

import javax.inject.Qualifier

/** Markiert den OkHttpClient der Bibelthek (ohne Disk-Cache – es wird nichts gespeichert). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BibleHttpClient
