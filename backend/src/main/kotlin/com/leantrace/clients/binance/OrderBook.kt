package com.leantrace.clients.binance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.IOException
import java.math.BigDecimal


data class OrderBook(
    val asks: List<OrderBookEntry>,
    val bids: List<OrderBookEntry>,
    val lastUpdateId: Long
)

@JsonDeserialize(using = OrderBookEntryDeserializer::class)
@JsonSerialize(using = OrderBookEntrySerializer::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderBookEntry(val price: BigDecimal, val qty: BigDecimal)


class OrderBookEntrySerializer : JsonSerializer<OrderBookEntry>() {
    @Throws(IOException::class)
    override fun serialize(orderBookEntry: OrderBookEntry, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartArray()
        gen.writeString(orderBookEntry.price.toString())
        gen.writeString(orderBookEntry.qty.toString())
        gen.writeEndArray()
    }
}

class OrderBookEntryDeserializer : JsonDeserializer<OrderBookEntry?>() {
    @Throws(IOException::class)
    override fun deserialize(jp: JsonParser, ctx: DeserializationContext?): OrderBookEntry {
        val oc: ObjectCodec = jp.getCodec()
        val node: JsonNode = oc.readTree(jp)
        val price = node[0].asText()
        val qty = node[1].asText()
        val orderBookEntry = OrderBookEntry(BigDecimal(price), BigDecimal(qty))
        return orderBookEntry
    }
}

