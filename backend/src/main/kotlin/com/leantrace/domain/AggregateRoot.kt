package com.leantrace.domain
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import com.leantrace.domain.event.DomainEvent
import org.springframework.data.annotation.Version
import org.springframework.data.domain.AfterDomainEventPublication
import org.springframework.data.domain.DomainEvents
import java.util.Collections.unmodifiableList


abstract class AggregateRoot<A : AggregateRoot<A>>(@Version var version: Int?) : Identifiable<String> {

    @org.springframework.data.annotation.Transient
    @Transient
    private val domainEvents: MutableList<DomainEvent<*>> = ArrayList()

    /**
     * Registers the given event object for publication on a call to a Spring Data repository's save methods.
     */
    protected fun registerEvent(event: DomainEvent<*>) {
        domainEvents.add(event)
    }

    /**
     * Clears all domain events currently held. Usually invoked by the infrastructure in place in Spring Data
     * repositories.
     */
    @AfterDomainEventPublication
    protected fun clearDomainEvents() {
        domainEvents.clear()
    }

    /**
     * All domain events currently captured by the aggregate.
     */
    @DomainEvents
    internal fun domainEvents(): List<DomainEvent<*>> {
        return unmodifiableList(domainEvents)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AggregateRoot<*>

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
