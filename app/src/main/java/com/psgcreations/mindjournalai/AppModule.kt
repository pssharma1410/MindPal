package com.psgcreations.mindjournalai

import android.content.Context
import androidx.room.Room
import com.psgcreations.mindjournalai.data.JournalDao
import com.psgcreations.mindjournalai.room.JournalDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore

    @Provides
    @Singleton
    fun provideJournalDatabase(
        @ApplicationContext context: Context
    ): JournalDatabase = Room.databaseBuilder(
        context,
        JournalDatabase::class.java,
        "journal_db"
    ).fallbackToDestructiveMigration(false)
        .build()

    // DAO Provider
    @Provides
    @Singleton
    fun provideJournalDao(db: JournalDatabase): JournalDao = db.journalDao()
}
