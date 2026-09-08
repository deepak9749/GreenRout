package com.example.presentation.market

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.MarketPrice
import com.example.data.model.RecyclingCompany
import com.example.data.repository.MarketRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MarketViewModel(
    marketRepository: MarketRepository
) : ViewModel() {

    val marketPrices: StateFlow<List<MarketPrice>> = marketRepository.getMarketPrices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recyclingCompanies: StateFlow<List<RecyclingCompany>> = marketRepository.getRecyclingCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
