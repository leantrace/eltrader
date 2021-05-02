package com.leantrace.mongo.changelogs
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.leantrace.domain.BinanceOrder
import com.leantrace.mongo.changelogs.defaultCollation
import com.leantrace.mongo.changelogs.getCollectionNameForEntity
import com.mongodb.client.model.CreateCollectionOptions
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.index.Index

@ChangeLog
class CL001InitialChangeLog {

    @ChangeSet(id = "initial-create-collections", author = "Alexander Schamne", order = "001")
    fun createCollections(mongockTemplate: MongockTemplate) {
        mongockTemplate.db.drop()
        listOf(
            BinanceOrder::class.java,
        ).map {
            mongockTemplate.getCollectionNameForEntity(it)
        }.forEach {
            mongockTemplate.db.createCollection(it, CreateCollectionOptions().collation(defaultCollation))
        }
    }

    @ChangeSet(id = "initial-data", author = "Alexander Schamne", order = "002")
    fun insertBaseData(mongockTemplate: MongockTemplate) {
    }

    @ChangeSet(id = "initial-indexes", author = "Alexander Schamne", order = "003")
    fun createIndexes(mongockTemplate: MongockTemplate) {
        mongockTemplate.indexOps(BinanceOrder::class.java).ensureIndex(Index(BinanceOrder::created.name, Sort.Direction.DESC))
    }
}
