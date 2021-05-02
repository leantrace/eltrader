package com.leantrace.domain
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
import java.time.LocalDateTime

abstract class Trackable<A : Trackable<A>>(created: LocalDateTime,
                                           updated: LocalDateTime,
                                           version: Int?): AggregateRoot<A>(version) {
    var created: LocalDateTime = created
        protected set
    var updated: LocalDateTime = updated
        protected set

    override fun toString(): String {
        return "${this.javaClass.name}(id=$id created=$created, updated=$updated)"
    }

}
