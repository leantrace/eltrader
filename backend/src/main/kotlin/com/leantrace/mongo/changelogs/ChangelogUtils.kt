package com.leantrace.mongo.changelogs
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate
import com.mongodb.client.model.Collation

internal val defaultCollation = Collation.builder().locale("en").build()

internal fun MongockTemplate.getCollectionNameForEntity(type: Class<*>): String = converter.mappingContext.getRequiredPersistentEntity(type).collection
