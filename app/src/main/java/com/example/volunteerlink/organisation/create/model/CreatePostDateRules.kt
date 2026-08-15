package com.example.volunteerlink.organisation.create.model

import java.util.Calendar

/** Date rules shared by the Step 1 UI and CreatePostViewModel. */
object CreatePostDateRules {
    const val MINIMUM_LEAD_DAYS = 7

    fun startOfDayMillis(timeMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun minimumStartDateMillis(todayMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(todayMillis)
            add(Calendar.DAY_OF_YEAR, MINIMUM_LEAD_DAYS)
        }.timeInMillis
    }

    fun isValidStartDate(dateMillis: Long?): Boolean {
        if (dateMillis == null) return false
        return startOfDayMillis(dateMillis) >= minimumStartDateMillis()
    }

    fun isValidPhysicalEndDate(
        startDateMillis: Long?,
        endDateMillis: Long?,
        isMultiDay: Boolean
    ): Boolean {
        if (startDateMillis == null || endDateMillis == null) return false

        return if (isMultiDay) {
            startOfDayMillis(endDateMillis) > startOfDayMillis(startDateMillis)
        } else {
            startOfDayMillis(endDateMillis) == startOfDayMillis(startDateMillis)
        }
    }

    fun isValidRemoteDueDate(
        startDateMillis: Long?,
        dueDateMillis: Long?
    ): Boolean {
        if (startDateMillis == null || dueDateMillis == null) return false
        return startOfDayMillis(dueDateMillis) > startOfDayMillis(startDateMillis)
    }

    fun nextDayMillis(dateMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(dateMillis)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
}
