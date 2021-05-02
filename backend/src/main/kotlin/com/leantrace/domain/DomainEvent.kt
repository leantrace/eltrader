package com.leantrace.domain
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */


import com.leantrace.domain.event.DomainEvent
import com.leantrace.domain.event.DomainEventId
import java.time.Instant

data class Created<C: Identifiable<String>>(override val subject: C,
                                            override val id: DomainEventId = createId(),
                                            override val timestamp: Instant = Instant.now(),
                                            override val type: String = "${subject.javaClass.name}Created",
                                            override val userId: String? = null) : DomainEvent<C>
