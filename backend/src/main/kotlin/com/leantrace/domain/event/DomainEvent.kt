package com.leantrace.domain.event
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.MongoId
import java.time.Instant

@Document(collection = "domainevents")
interface DomainEvent<T> {
    @get:MongoId val id: DomainEventId
    val timestamp: Instant
    val subject: T
    val type: String
    val userId: String?
}

typealias DomainEventId = String
