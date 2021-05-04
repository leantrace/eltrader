package com.leantrace.mongo
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("app.data.mongodb")
data class MongoProperties(val uri: String,
                           val database: String,
                           val user: String?,
                           val password: String?)
