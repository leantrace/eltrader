package com.leantrace.controller

import com.leantrace.strategies.CryptoMomentumStrategy
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 *
 *
 * Created on 05.05.21
 * @author: Alexander Schamne <alexander.schamne@gmail.com>
 *
 */
@RestController
@RequestMapping("api/v1/apis", produces = [MediaType.APPLICATION_JSON_VALUE])
class ApiController(val strategy: CryptoMomentumStrategy) {
    @GetMapping("run")
    suspend fun run(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) sort: List<String>?
    ) {
        strategy.init()
        //strategy.execute()
    }


}
