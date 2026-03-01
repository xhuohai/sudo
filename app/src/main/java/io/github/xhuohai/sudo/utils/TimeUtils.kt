package io.github.xhuohai.sudo.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object TimeUtils {
    fun getTimeAgo(dateString: String): String {
        try {
            // Discourse usually returns ISO 8601 strings like "2024-01-01T12:00:00.000Z"
            // We might need to adjust parsing based on actual format. 
            // Assuming simple ZonedDateTime or similar.
            // For simplicity/robustness, let's try to parse standard ISO.
            
            // Note: dateString might vary. 
            // If it's "2024-01-01T12:00:00.000Z", Instant.parse works.
            // If it's a relative string already, return it.
            
            // Since we are in Android/Java environment, let's use java.time (API 26+)
            // Our minSdk might be lower, so we should check. But typically for modern compose apps it's 21+ with desugaring.
            
            val instant = java.time.Instant.parse(dateString)
            val now = java.time.Instant.now()
            
            val seconds = ChronoUnit.SECONDS.between(instant, now)
            val minutes = ChronoUnit.MINUTES.between(instant, now)
            val hours = ChronoUnit.HOURS.between(instant, now)
            val days = ChronoUnit.DAYS.between(instant, now)
            
            return when {
                seconds < 60 -> "刚刚"
                minutes < 60 -> "${minutes}分钟前"
                hours < 24 -> "${hours}小时前"
                days < 30 -> "${days}天前"
                days < 365 -> "${days / 30}个月前"
                else -> "${days / 365}年前"
            }
        } catch (e: Exception) {
            return dateString // Fallback
        }
    }
}
