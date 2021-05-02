package com.leantrace.domain
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.MongoId
import java.io.Serializable

interface Identifiable<ID : Serializable> {
    @get:MongoId
    var id: ID
}

fun createId(): String = ObjectId.get().toHexString()

/**
 * Sorts a list of [Identifiable]s in the same order as a list of ids. Attention:Won't work correctly if it contains the same key multiple times.
 *
 * Workaround for MongoDB not returning results of an $in query in the same order as the parameters were given.
 * https://jira.mongodb.org/browse/SERVER-7528
 */
data class IdParameterListComparator<ID : Serializable, T : Identifiable<ID>>(private val ids: List<ID>) : Comparator<T> {
    override fun compare(o1: T, o2: T): Int = ids.indexOf(o1.id).compareTo(ids.indexOf(o2.id))
}
