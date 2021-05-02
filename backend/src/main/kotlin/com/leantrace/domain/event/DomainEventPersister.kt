package com.leantrace.domain.event

/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */


import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class DomainEventPersister(private val domainEventRepository: DomainEventRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun handle(event: Any) {
        if (event is DomainEvent<*>) {
            @Suppress("UNCHECKED_CAST")
            domainEventRepository.save(event as DomainEvent<Any>).let { logger.info("Domain Event registered: {}", it) }
        }
    }

}
