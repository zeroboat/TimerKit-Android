package com.zeroboat.timerkit.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "timerkit_settings")

object PreferencesKeys {
    // Tabata
    val TABATA_TOTAL_SETS        = intPreferencesKey("tabata_total_sets")
    val TABATA_PREPARE_SECONDS   = intPreferencesKey("tabata_prepare_seconds")
    val TABATA_WORK_SECONDS      = intPreferencesKey("tabata_work_seconds")
    val TABATA_REST_SECONDS      = intPreferencesKey("tabata_rest_seconds")

    // Running
    val RUNNING_TOTAL_INTERVALS  = intPreferencesKey("running_total_intervals")
    val RUNNING_WARMUP_SECONDS   = intPreferencesKey("running_warmup_seconds")
    val RUNNING_RUN_SECONDS      = intPreferencesKey("running_run_seconds")
    val RUNNING_REST_SECONDS     = intPreferencesKey("running_rest_seconds")
}
