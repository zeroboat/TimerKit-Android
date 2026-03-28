package com.zeroboat.timerkit.common

import android.content.Context
import com.google.android.gms.ads.MobileAds

object AdHelper {
    fun initialize(context: Context) {
        MobileAds.initialize(context)
    }
}
