package com.leantrace.mongo
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.bson.*
import org.bson.BsonArray.parse
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationOperationContext
import org.springframework.data.mongodb.core.aggregation.TypedAggregation

/**
 * Allows to use native BSON objects for the Spring Data Aggregation framework.
 */
class NativeAggregationOperation(bsonStage: BsonDocument): AggregationOperation {

    private val document = Document(bsonStage as Map<String, Any>)

    override fun toDocument(context: AggregationOperationContext): Document = document
}

/**
 * Creates an Aggregation from a BSON File located in resources/mongo/.
 *
 * That file can be copied from/to MongoDB tools like MongoDB Compass.
 */
inline fun <reified T> createAggregationFromFile(filename: String): TypedAggregation<T> = parse(ClassPathResource("mongo/$filename").url.readText()).values
        .map { it.asDocument() }
        .map { NativeAggregationOperation(it) }
        .let { Aggregation.newAggregation(T::class.java, it) }

/**
 * Creates an Aggregation from a BSON File located in resources/mongo/.
 *
 * That file can be copied from/to MongoDB tools like MongoDB Compass.
 */
inline fun <reified T> createAggregationFromFileWithAdditionalMatch(filename: String, appendMatch: Map<String, BsonValue>, pageable: Pageable? = null): TypedAggregation<T> {
    val bson = parse(ClassPathResource("mongo/$filename").url.readText())
    if (pageable != null) {
        pageable.sort.forEach {
            bson.add(
                BsonDocument().append(
                    "\$sort", BsonDocument()
                        .append(it.property, BsonInt32(if (it.isAscending) 1 else -1))
                        .append(it.property, BsonInt32(if (it.isAscending) 1 else -1))
                )
            )
        }
        bson.add(
            BsonDocument().append(
                "\$facet", BsonDocument()
                    .append("metadata", BsonArray(listOf(BsonDocument().append("\$count", BsonString("count")))))
                    .append(
                        "data", BsonArray(
                            listOf(
                                BsonDocument().append("\$skip", BsonInt32(pageable.offset.toInt())),
                                BsonDocument().append("\$limit", BsonInt32(pageable.pageSize))
                            )
                        )
                    )
            )
        )
    }
    return bson.values
        .map {
            val doc = it.asDocument()
            if (doc.containsKey("\$match")) {
                appendMatch.forEach { a ->
                    val d = doc["\$match"]
                    if (d is BsonDocument) {
                        d.append(a.key, a.value)
                    }
                }
            }
            doc
        }
        .map { NativeAggregationOperation(it) }
        .let { Aggregation.newAggregation(T::class.java, it) }
}
