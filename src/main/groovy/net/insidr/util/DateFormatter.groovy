package net.insidr.util

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DateFormatter {

    static def fromDateStringToTime(dateTime) {
        def convertedDateTime = Date.from(LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC))
        return convertedDateTime.time as Double
    }

    static def currentTime() {
        return new Date().time as Double
    }

}
