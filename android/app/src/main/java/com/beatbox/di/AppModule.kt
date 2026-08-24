package com.beatbox.di

import android.content.Context
import com.beatbox.data.api.BeatBoxApi
import com.beatbox.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthRepository(api: BeatBoxApi): AuthRepository = AuthRepository(api)

    @Provides
    @Singleton
    fun provideMusicRepository(api: BeatBoxApi): MusicRepository = MusicRepository(api)

    @Provides
    @Singleton
    fun provideSocialRepository(api: BeatBoxApi): SocialRepository = SocialRepository(api)

    @Provides
    @Singleton
    fun providePremiumRepository(api: BeatBoxApi): PremiumRepository = PremiumRepository(api)

    @Provides
    @Singleton
    fun provideStripeRepository(api: BeatBoxApi): StripeRepository = StripeRepository(api)

    @Provides
    @Singleton
    fun provideSearchRepository(api: BeatBoxApi): SearchRepository = SearchRepository(api)

    @Provides
    @Singleton
    fun provideReportRepository(api: BeatBoxApi): ReportRepository = ReportRepository(api)

    @Provides
    @Singleton
    fun provideAdminRepository(api: BeatBoxApi): AdminRepository = AdminRepository(api)
}
