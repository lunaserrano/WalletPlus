package com.walletplus.app.data.local.database

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Fechas guardadas de forma consistente: LocalDate como epochDay (sin zona horaria),
 * Instant como epoch millis (UTC) y YearMonth como "yyyy-MM".
 */
class Converters {
    @TypeConverter fun localDateToLong(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun yearMonthToString(value: YearMonth?): String? = value?.toString()
    @TypeConverter fun stringToYearMonth(value: String?): YearMonth? = value?.let(YearMonth::parse)
}
