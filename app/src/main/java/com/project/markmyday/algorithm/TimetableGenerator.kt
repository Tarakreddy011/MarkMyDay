package com.project.markmyday.algorithm

import android.util.Log
import com.project.markmyday.data.model.DaySchedule
import com.project.markmyday.data.model.Period
import com.project.markmyday.data.model.SubjectQuota
import com.project.markmyday.data.model.Teacher
import com.project.markmyday.data.model.Timetable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

class TimetableGenerator {

    companion object {
        private const val TAG = "TimetableAlgo"

        private const val PRIMARY_PERIODS = 7
        private const val OTHER_PERIODS = 10

        private const val MAX_SUBJECTS_PER_DAY = 3
        private const val MAX_CONSECUTIVE_SAME_SUBJECT = 2

        private const val LEISURE_SUBJECT = "Leisure"
        private const val UNASSIGNED_TEACHER = "UNASSIGNED"
    }

    val daysOfWeek = listOf(
        "Monday",
        "Tuesday",
        "Wednesday",
        "Thursday",
        "Friday",
        "Saturday"
    )

    // -------------------------------------------------------------------------
    // 1. DEFAULT QUOTA GENERATION
    // -------------------------------------------------------------------------

    fun generateDefaultQuota(
        category: String,
        homeTeacher: Teacher?,
        availableTeachers: List<Teacher>
    ): MutableMap<String, SubjectQuota> {

        val quotaMap = mutableMapOf<String, SubjectQuota>()

        val subjectDistribution = when (category) {

            "Primary" -> mapOf(
                "Telugu" to 6,
                "Hindi" to 6,
                "English" to 6,
                "Math" to 8,
                "Science" to 8,
                "Social" to 8
            )

            "Secondary" -> mapOf(
                "Telugu" to 6,
                "Hindi" to 6,
                "English" to 6,
                "Math" to 14,
                "Science" to 14,
                "Social" to 14
            )

            "High School" -> mapOf(
                "Telugu" to 6,
                "Hindi" to 6,
                "English" to 6,
                "Math" to 11,
                "Phy" to 11,
                "Bio" to 10,
                "Social" to 10
            )

            else -> emptyMap()
        }

        for ((subject, count) in subjectDistribution) {

            val assignedTeacher = findTeacherForSubject(
                subject = subject,
                category = category,
                homeTeacher = homeTeacher,
                availableTeachers = availableTeachers
            )

            quotaMap[subject] = SubjectQuota(
                subject = subject,
                classCount = count,
                teacherId = assignedTeacher?.teacherId ?: UNASSIGNED_TEACHER,
                teacherName = assignedTeacher?.name ?: "No Teacher"
            )
        }

        return quotaMap
    }

    private fun findTeacherForSubject(
        subject: String,
        category: String,
        homeTeacher: Teacher?,
        availableTeachers: List<Teacher>
    ): Teacher? {

        // Prefer home teacher if they teach this subject
        if (homeTeacher?.subject == subject &&
            homeTeacher.classesTaughtCategories.contains(category)
        ) {
            return homeTeacher
        }

        // Otherwise find another suitable teacher
        return availableTeachers.firstOrNull {
            it.subject == subject &&
                    it.classesTaughtCategories.contains(category)
        }
    }

    // -------------------------------------------------------------------------
    // 2. MAIN GENERATOR
    // -------------------------------------------------------------------------

    suspend fun generateScheduleForClass(
        category: String,
        quotaMap: Map<String, SubjectQuota>,
        homeTeacherId: String,
        allExistingTimetables: List<Timetable>,
        currentClassName: String
    ): Map<String, DaySchedule> = withContext(Dispatchers.Default) {

        val periodsPerDay = getPeriodsPerDay(category)
        val totalSlots = periodsPerDay * daysOfWeek.size

        val workingQuota = quotaMap
            .mapValues { (_, quota) -> quota.copy() }
            .toMutableMap()

        val totalRequiredClasses = workingQuota.values.sumOf {
            it.classCount
        }

        // ---------------------------------------------------------------------
        // Check if quota is larger than available timetable slots
        // ---------------------------------------------------------------------

        if (totalRequiredClasses > totalSlots) {

            Log.e(
                TAG,
                "Impossible timetable: required=$totalRequiredClasses, slots=$totalSlots"
            )

            return@withContext emptySchedule(periodsPerDay)
        }

        // ---------------------------------------------------------------------
        // Add Leisure
        // ---------------------------------------------------------------------

        val leisureSlots = totalSlots - totalRequiredClasses

        if (leisureSlots > 0) {

            workingQuota[LEISURE_SUBJECT] = SubjectQuota(
                subject = LEISURE_SUBJECT,
                teacherId = "NONE",
                teacherName = "Free Period",
                classCount = leisureSlots
            )
        }

        // ---------------------------------------------------------------------
        // Create empty schedule
        // ---------------------------------------------------------------------

        val schedule = createEmptySchedule(periodsPerDay)

        // ---------------------------------------------------------------------
        // Reserve Period 1 for Home Teacher
        // ---------------------------------------------------------------------

        reserveHomeTeacherPeriods(
            schedule = schedule,
            quota = workingQuota,
            homeTeacherId = homeTeacherId,
            periodsPerDay = periodsPerDay
        )

        // ---------------------------------------------------------------------
        // Start Backtracking
        // ---------------------------------------------------------------------

        val success = solve(
            schedule = schedule,
            quota = workingQuota,
            dayIndex = 0,
            periodIndex = 0,
            periodsPerDay = periodsPerDay,
            allExistingTimetables = allExistingTimetables,
            currentClassName = currentClassName
        )

        if (!success) {

            Log.e(
                TAG,
                "Unable to generate conflict-free timetable for $currentClassName"
            )

            return@withContext emptySchedule(periodsPerDay)
        }

        // ---------------------------------------------------------------------
        // Convert to DaySchedule
        // ---------------------------------------------------------------------

        schedule.mapValues { (_, periods) ->
            DaySchedule(
                periods = periods.filterNotNull()
            )
        }
    }

    // -------------------------------------------------------------------------
    // 3. EMPTY SCHEDULE
    // -------------------------------------------------------------------------

    private fun createEmptySchedule(
        periodsPerDay: Int
    ): MutableMap<String, MutableList<Period?>> {

        return daysOfWeek.associateWith {
            MutableList<Period?>(periodsPerDay) { null }
        }.toMutableMap()
    }

    private fun emptySchedule(
        periodsPerDay: Int
    ): Map<String, DaySchedule> {

        return daysOfWeek.associateWith {
            DaySchedule(
                periods = emptyList()
            )
        }
    }

    // -------------------------------------------------------------------------
    // 4. HOME TEACHER RESERVATION
    // -------------------------------------------------------------------------

    private fun reserveHomeTeacherPeriods(
        schedule: MutableMap<String, MutableList<Period?>>,
        quota: MutableMap<String, SubjectQuota>,
        homeTeacherId: String,
        periodsPerDay: Int
    ) {

        val homeTeacherQuota = quota.values.firstOrNull {
            it.teacherId == homeTeacherId &&
                    it.subject != LEISURE_SUBJECT &&
                    it.classCount > 0
        } ?: return

        val subject = homeTeacherQuota.subject

        for (day in daysOfWeek) {

            val remaining = quota[subject]?.classCount ?: 0

            if (remaining <= 0) {
                break
            }

            val (start, end) = getPeriodTimings(1)

            schedule[day]!![0] = Period(
                periodNumber = 1,
                startTime = start,
                endTime = end,
                subject = subject,
                teacherId = homeTeacherId,
                teacherName = homeTeacherQuota.teacherName
            )

            quota[subject] = homeTeacherQuota.copy(
                classCount = remaining - 1
            )
        }
    }

    // -------------------------------------------------------------------------
    // 5. BACKTRACKING SOLVER
    // -------------------------------------------------------------------------

    private suspend fun solve(
        schedule: MutableMap<String, MutableList<Period?>>,
        quota: MutableMap<String, SubjectQuota>,
        dayIndex: Int,
        periodIndex: Int,
        periodsPerDay: Int,
        allExistingTimetables: List<Timetable>,
        currentClassName: String
    ): Boolean {

        yield()

        // ---------------------------------------------------------------------
        // Finished
        // ---------------------------------------------------------------------

        if (dayIndex >= daysOfWeek.size) {
            return quota.values.all {
                it.classCount == 0
            }
        }

        // ---------------------------------------------------------------------
        // Move to next day
        // ---------------------------------------------------------------------

        if (periodIndex >= periodsPerDay) {

            return solve(
                schedule,
                quota,
                dayIndex + 1,
                0,
                periodsPerDay,
                allExistingTimetables,
                currentClassName
            )
        }

        val currentDay = daysOfWeek[dayIndex]

        // ---------------------------------------------------------------------
        // Already occupied
        // ---------------------------------------------------------------------

        if (schedule[currentDay]!![periodIndex] != null) {

            return solve(
                schedule,
                quota,
                dayIndex,
                periodIndex + 1,
                periodsPerDay,
                allExistingTimetables,
                currentClassName
            )
        }

        // ---------------------------------------------------------------------
        // Get candidate subjects
        // ---------------------------------------------------------------------

        val candidates = getCandidates(
            schedule = schedule,
            day = currentDay,
            periodIndex = periodIndex,
            quota = quota,
            allExistingTimetables = allExistingTimetables,
            currentClassName = currentClassName
        )

        // ---------------------------------------------------------------------
        // Try each candidate
        // ---------------------------------------------------------------------

        for (item in candidates) {

            if (!isValidPlacement(
                    schedule = schedule,
                    day = currentDay,
                    periodIndex = periodIndex,
                    item = item,
                    allExistingTimetables = allExistingTimetables,
                    currentClassName = currentClassName
                )
            ) {
                continue
            }

            // PLACE

            val (start, end) = getPeriodTimings(
                periodIndex + 1
            )

            schedule[currentDay]!![periodIndex] = Period(
                periodNumber = periodIndex + 1,
                startTime = start,
                endTime = end,
                subject = item.subject,
                teacherId = item.teacherId,
                teacherName = item.teacherName
            )

            quota[item.subject] = item.copy(
                classCount = item.classCount - 1
            )

            // RECURSE

            val solved = solve(
                schedule = schedule,
                quota = quota,
                dayIndex = dayIndex,
                periodIndex = periodIndex + 1,
                periodsPerDay = periodsPerDay,
                allExistingTimetables = allExistingTimetables,
                currentClassName = currentClassName
            )

            if (solved) {
                return true
            }

            // BACKTRACK

            schedule[currentDay]!![periodIndex] = null

            quota[item.subject] = item
        }

        return false
    }

    // -------------------------------------------------------------------------
    // 6. CANDIDATE SORTING
    // -------------------------------------------------------------------------

    private fun getCandidates(
        schedule: MutableMap<String, MutableList<Period?>>,
        day: String,
        periodIndex: Int,
        quota: MutableMap<String, SubjectQuota>,
        allExistingTimetables: List<Timetable>,
        currentClassName: String
    ): List<SubjectQuota> {

        return quota.values
            .filter { it.classCount > 0 }
            .filter {
                isValidPlacement(
                    schedule = schedule,
                    day = day,
                    periodIndex = periodIndex,
                    item = it,
                    allExistingTimetables = allExistingTimetables,
                    currentClassName = currentClassName
                )
            }
            .sortedWith(
                compareByDescending<SubjectQuota> {

                    // Schedule subjects with more remaining classes first
                    it.classCount

                }.thenBy {

                    // Leisure should be considered last
                    if (it.subject == LEISURE_SUBJECT) 1 else 0
                }
            )
    }

    // -------------------------------------------------------------------------
    // 7. VALIDATION
    // -------------------------------------------------------------------------

    private fun isValidPlacement(
        schedule: MutableMap<String, MutableList<Period?>>,
        day: String,
        periodIndex: Int,
        item: SubjectQuota,
        allExistingTimetables: List<Timetable>,
        currentClassName: String
    ): Boolean {

        // -------------------------------------------------------------
        // Leisure
        // -------------------------------------------------------------

        if (item.subject == LEISURE_SUBJECT) {

            val leisureCount = schedule[day]!!
                .filterNotNull()
                .count {
                    it.subject == LEISURE_SUBJECT
                }

            // Maximum one leisure period per day
            return leisureCount == 0
        }

        // -------------------------------------------------------------
        // Teacher must be assigned
        // -------------------------------------------------------------

        if (item.teacherId == UNASSIGNED_TEACHER) {
            return false
        }

        // -------------------------------------------------------------
        // Teacher conflict with other classes
        // -------------------------------------------------------------

        val teacherBusyElsewhere = allExistingTimetables.any { timetable ->

            timetable.className != currentClassName &&
                    timetable.weeklySchedule[day]
                        ?.periods
                        ?.any { period ->

                            period.periodNumber == periodIndex + 1 &&
                                    period.teacherId == item.teacherId

                        } == true
        }

        if (teacherBusyElsewhere) {
            return false
        }

        // -------------------------------------------------------------
        // Maximum subject count per day
        // -------------------------------------------------------------

        val todaysSubjectCount = schedule[day]!!
            .filterNotNull()
            .count {
                it.subject == item.subject
            }

        if (todaysSubjectCount >= MAX_SUBJECTS_PER_DAY) {
            return false
        }

        // -------------------------------------------------------------
        // No 3 consecutive classes of same subject
        // -------------------------------------------------------------

        if (hasTooManyConsecutiveSubjects(
                schedule = schedule,
                day = day,
                periodIndex = periodIndex,
                subject = item.subject
            )
        ) {
            return false
        }

        return true
    }

    // -------------------------------------------------------------------------
    // 8. CONSECUTIVE SUBJECT CHECK
    // -------------------------------------------------------------------------

    private fun hasTooManyConsecutiveSubjects(
        schedule: MutableMap<String, MutableList<Period?>>,
        day: String,
        periodIndex: Int,
        subject: String
    ): Boolean {

        if (periodIndex < MAX_CONSECUTIVE_SAME_SUBJECT) {
            return false
        }

        var consecutive = 0

        for (index in periodIndex - 1 downTo 0) {

            val previousSubject =
                schedule[day]!![index]?.subject

            if (previousSubject == subject) {
                consecutive++
            } else {
                break
            }
        }

        return consecutive >= MAX_CONSECUTIVE_SAME_SUBJECT
    }

    // -------------------------------------------------------------------------
    // 9. PERIOD COUNT
    // -------------------------------------------------------------------------

    private fun getPeriodsPerDay(
        category: String
    ): Int {

        return if (category == "Primary") {
            PRIMARY_PERIODS
        } else {
            OTHER_PERIODS
        }
    }

    // -------------------------------------------------------------------------
    // 10. PERIOD TIMINGS
    // -------------------------------------------------------------------------

    private fun getPeriodTimings(
        periodNumber: Int
    ): Pair<String, String> {

        return when (periodNumber) {

            1 -> "09:00 AM" to "09:45 AM"

            2 -> "09:45 AM" to "10:30 AM"

            3 -> "10:30 AM" to "11:15 AM"

            4 -> "11:30 AM" to "12:15 PM"

            5 -> "12:15 PM" to "01:00 PM"

            6 -> "01:40 PM" to "02:25 PM"

            7 -> "02:25 PM" to "03:10 PM"

            8 -> "03:20 PM" to "04:05 PM"

            9 -> "04:05 PM" to "04:50 PM"

            10 -> "04:50 PM" to "05:30 PM"

            else -> "" to ""
        }
    }
}