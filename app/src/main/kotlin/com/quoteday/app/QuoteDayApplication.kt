package com.quoteday.app

import android.app.Application
import com.quoteday.app.data.AppSettings
import com.quoteday.app.data.ChallengeRecordStore
import com.quoteday.app.data.HeartStore
import com.quoteday.core.ChallengeGenerator
import com.quoteday.core.QuoteLibrary
import com.quoteday.core.QuoteService

/**
 * 앱이 사는 동안 하나씩만 있으면 되는 것들.
 *
 * 의존성 주입 라이브러리를 쓰지 않는다. 주입할 것이 다섯 개뿐이고, 전부 상태가
 * 없거나 SharedPreferences 한 겹 위에 있어서 프레임워크가 해결해 줄 문제가 없다.
 */
class QuoteDayApplication : Application() {

    val settings: AppSettings by lazy { AppSettings(this) }
    val library: QuoteLibrary by lazy { QuoteLibrary.shared }
    val quotes: QuoteService by lazy { QuoteService(library) }
    val generator: ChallengeGenerator by lazy { ChallengeGenerator(library) }
    val hearts: HeartStore by lazy { HeartStore(this) }
    val challengeRecords: ChallengeRecordStore by lazy { ChallengeRecordStore(this) }
}
