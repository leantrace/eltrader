package com.leantrace.mongo
/**
 * Created on 01.05.21
 * @author: Alexander Schamne <alexander.schamne@leantrace.ch>
 *
 */

import brave.Tracing
import brave.mongodb.MongoDBTracing
import com.github.cloudyrock.spring.v5.EnableMongock
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsCommandListener
import io.micrometer.core.instrument.binder.mongodb.MongoMetricsConnectionPoolListener
import org.bson.types.Decimal128
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.MongoDatabaseFactory
import org.springframework.data.mongodb.MongoTransactionManager
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.convert.MongoCustomConversions
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal

@Configuration
@ImportResource("classpath:mongo/transactional.xml")
@EnableConfigurationProperties(MongoProperties::class)
@EnableMongock
class MongoClientConfiguration(private val MongoProperties: MongoProperties,
                               private val meterRegistry: MeterRegistry?,
                               private val tracing: Tracing?) : AbstractMongoClientConfiguration() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun getDatabaseName(): String = MongoProperties.database

    override fun mongoClientSettings(): MongoClientSettings {
        val connectionString = UriComponentsBuilder.fromUriString(MongoProperties.uri)
                .query("retryWrites=true&w=majority&wtimeoutMS=10000&journal=true&readConcernLevel=majority").build().toUriString()

        return MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionString))
                .applicationName("Apiable")
                .configureCredential()
                .configureMonitoring()
                .build()
    }

    private fun MongoClientSettings.Builder.configureCredential(): MongoClientSettings.Builder {
        val user = MongoProperties.user
        val password = MongoProperties.password

        if (password != null && user != null) {
            this.credential(MongoCredential.createCredential(user, "admin", password.toCharArray()))
        } else {
            logger.info("Not using authentication for MongoDB. Username or Password was unspecified")
        }

        return this
    }

    private fun MongoClientSettings.Builder.configureMonitoring(): MongoClientSettings.Builder {
        if (meterRegistry != null) {
            this.addCommandListener(MongoMetricsCommandListener(meterRegistry))
                    .applyToConnectionPoolSettings { it.addConnectionPoolListener(MongoMetricsConnectionPoolListener(meterRegistry)) }
        }
        if (tracing != null) {
            this.addCommandListener(MongoDBTracing.create(tracing).commandListener())
        }

        return this
    }

    override fun customConversions(): MongoCustomConversions = MongoCustomConversions.create {
        it.useNativeDriverJavaTimeCodecs()

        // Override Spring Data's default conversion of BigDecimal to String
        it.registerConverter(BigDecimalToDecimal128Converter())
        it.registerConverter(Decimal128ToBigDecimalConverter())
    }

    private class BigDecimalToDecimal128Converter : Converter<BigDecimal, Decimal128?> {
        override fun convert(source: BigDecimal): Decimal128? = Decimal128(source)
    }

    private class Decimal128ToBigDecimalConverter : Converter<Decimal128, BigDecimal?> {
        override fun convert(source: Decimal128): BigDecimal? = source.bigDecimalValue()
    }

    /**
     * Enable Transaction support.
     */
    @Bean
    fun mongoTransactionManager(@Suppress("SpringJavaInjectionPointsAutowiringInspection") dbFactory: MongoDatabaseFactory): MongoTransactionManager = MongoTransactionManager(dbFactory)

    @Bean
    fun gridFsTemplate(mongoDatabaseFactory: MongoDatabaseFactory, mappingMongoConverter: MappingMongoConverter): GridFsTemplate = GridFsTemplate(mongoDatabaseFactory, mappingMongoConverter)
}
