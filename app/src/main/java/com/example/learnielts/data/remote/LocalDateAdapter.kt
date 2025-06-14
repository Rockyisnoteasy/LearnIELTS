package com.example.learnielts.data.remote

import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE // 这是 "YYYY-MM-DD" 格式

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: com.google.gson.JsonDeserializationContext?
    ): LocalDate {
        return try {
            LocalDate.parse(json?.asString, formatter)
        } catch (e: Exception) {
            throw JsonParseException(e)
        }
    }

    override fun serialize(
        src: LocalDate?,
        typeOfSrc: Type?,
        context: com.google.gson.JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(formatter))
    }
}