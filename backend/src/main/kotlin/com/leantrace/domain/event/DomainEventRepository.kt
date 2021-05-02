package com.leantrace.domain.event
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.springframework.data.repository.PagingAndSortingRepository

interface DomainEventRepository : PagingAndSortingRepository<DomainEvent<Any>, DomainEventId>
