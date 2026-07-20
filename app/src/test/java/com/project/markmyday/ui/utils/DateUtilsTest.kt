package com.project.markmyday.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

class DateUtilsTest {

    @Test
    fun calculateAgeFromDateOfBirth_nullInput_returnsNull() {
        assertNull(calculateAgeFromDateOfBirth(null))
    }

    @Test
    fun calculateAgeFromDateOfBirth_birthdayIsToday_returnsCorrectAge() {
        val dobCal = Calendar.getInstance()
        dobCal.add(Calendar.YEAR, -20)

        val age = calculateAgeFromDateOfBirth(dobCal.time)
        assertEquals(20, age)
    }

    @Test
    fun calculateAgeFromDateOfBirth_birthdayWasYesterday_returnsCorrectAge() {
        val dobCal = Calendar.getInstance()
        dobCal.add(Calendar.YEAR, -20)
        dobCal.add(Calendar.DAY_OF_YEAR, -1)

        val age = calculateAgeFromDateOfBirth(dobCal.time)
        assertEquals(20, age)
    }

    @Test
    fun calculateAgeFromDateOfBirth_birthdayIsTomorrow_returnsCorrectAgeMinusOne() {
        val dobCal = Calendar.getInstance()
        dobCal.add(Calendar.YEAR, -20)
        dobCal.add(Calendar.DAY_OF_YEAR, 1)

        val age = calculateAgeFromDateOfBirth(dobCal.time)
        assertEquals(19, age)
    }

    @Test
    fun formatDateForDisplay_nullInput_returnsEmptyString() {
        assertEquals("", formatDateForDisplay(null))
    }

    @Test
    fun formatDateForDisplay_validDate_returnsFormattedString() {
        val localDate = LocalDate.of(2023, 12, 5)
        val date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

        val formatted = formatDateForDisplay(date)
        assertEquals("05122023", formatted)
    }
}
