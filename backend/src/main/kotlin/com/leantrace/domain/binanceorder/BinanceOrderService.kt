package com.leantrace.domain.binanceorder

import com.leantrace.domain.AbstractService
import com.leantrace.domain.BinanceOrder
import org.springframework.stereotype.Service


@Service
class BinanceOrderService(repo: BinanceOrderRepository) : AbstractService<BinanceOrderRepository, BinanceOrder, String>(repo) {
    override fun resolve(objects: List<BinanceOrder>): List<BinanceOrder> = objects
    override fun omit(objects: List<BinanceOrder>): List<BinanceOrder> = objects
}
