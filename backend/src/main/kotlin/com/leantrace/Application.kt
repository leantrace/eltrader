package com.leantrace
/**
 *
 *
 * Created on 30.04.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
