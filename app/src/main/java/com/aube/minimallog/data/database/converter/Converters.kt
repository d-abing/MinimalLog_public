package com.aube.minimallog.data.database.converter
import androidx.room.TypeConverter
import java.time.LocalDate

object Converters {
    @TypeConverter fun epochDayToLocalDate(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()
}