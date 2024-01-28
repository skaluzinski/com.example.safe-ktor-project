package com.example.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ApiResponse<T>(val errorMessage: String?, val data: T)

class ApiResponseSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<ApiResponse<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor
    override fun serialize(encoder: Encoder, value: ApiResponse<T>) = dataSerializer.serialize(encoder, value.data)
    override fun deserialize(decoder: Decoder) = ApiResponse(errorMessage = "", data = dataSerializer.deserialize(decoder))
}