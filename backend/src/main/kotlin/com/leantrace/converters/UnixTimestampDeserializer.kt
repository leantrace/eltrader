package com.leantrace.converters

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
internal class UnixTimestampDeserializer : JsonDeserializer<LocalDateTime?>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): LocalDateTime? {
        val timestamp = jp.text.trim().toLong()
        return try {
            LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), TimeZone.getDefault().toZoneId())
        } catch (e: Exception) {
            null
        }
    }
}
