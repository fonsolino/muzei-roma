package it.fonsolo.muzeiroma

import android.content.Context
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object RotationSettings {

    const val PREF_NAME = "muzei_roma_prefs"
    const val KEY_MODE = "rotation_mode"
    const val KEY_ROTATIONS_COUNT = "rotations_count"

    const val MODE_DAY_ONLY = 0
    const val MODE_ROTATION = 1

    const val DEFAULT_MODE = MODE_ROTATION
    const val DEFAULT_ROTATIONS = 3

    const val TOTAL_DAYS = 41
    private val START_DATE: LocalDate = LocalDate.of(2026, 3, 24)

    /**
     * Calcola il numero GIORNO corrente (1..41) basandosi sulla data di oggi
     * e sul ciclo di 41 giorni che inizia il 24 marzo 2026.
     */
    fun getTodayGiorno(): Int {
        val today = LocalDate.now()
        val daysDiff = ChronoUnit.DAYS.between(START_DATE, today)
        return ((daysDiff % TOTAL_DAYS + TOTAL_DAYS) % TOTAL_DAYS + 1).toInt()
    }

    fun getMode(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, DEFAULT_MODE)

    fun getRotationsCount(context: Context): Int =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_ROTATIONS_COUNT, DEFAULT_ROTATIONS)

    fun saveSettings(context: Context, mode: Int, rotationsCount: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MODE, mode)
            .putInt(KEY_ROTATIONS_COUNT, rotationsCount)
            .apply()
    }
}
