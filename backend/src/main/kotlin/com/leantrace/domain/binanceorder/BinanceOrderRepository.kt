package com.leantrace.domain.binanceorder
import com.leantrace.domain.BinanceOrder
import com.leantrace.domain.BinanceOrderId
import org.springframework.data.repository.PagingAndSortingRepository

interface BinanceOrderRepository: PagingAndSortingRepository<BinanceOrder, BinanceOrderId>


