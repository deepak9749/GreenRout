package com.example.data.repository

import com.example.data.model.MarketPrice
import com.example.data.model.RecyclingCompany
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class MarketRepository {
    private val marketPricesFlow = MutableStateFlow<List<MarketPrice>>(emptyList())
    private val companiesFlow = MutableStateFlow<List<RecyclingCompany>>(emptyList())

    init {
        seedMarketData()
    }

    private fun seedMarketData() {
        val prices = listOf(
            MarketPrice(id = "p1", wasteType = "Iron", approximatePrice = "26 - 32", unit = "kg"),
            MarketPrice(id = "p2", wasteType = "Plastic (PET / Mixed)", approximatePrice = "12 - 18", unit = "kg"),
            MarketPrice(id = "p3", wasteType = "Paper / Cardboard", approximatePrice = "10 - 15", unit = "kg"),
            MarketPrice(id = "p4", wasteType = "Glass Bottles", approximatePrice = "2 - 5", unit = "kg"),
            MarketPrice(id = "p5", wasteType = "Metal (Copper / Brass / Aluminum)", approximatePrice = "120 - 450", unit = "kg"),
            MarketPrice(id = "p6", wasteType = "Electronics / E-waste", approximatePrice = "25 - 80", unit = "unit / kg"),
            MarketPrice(id = "p7", wasteType = "Fridge / Refrigerator", approximatePrice = "700 - 1200", unit = "unit"),
            MarketPrice(id = "p8", wasteType = "TV / Monitor", approximatePrice = "200 - 500", unit = "unit")
        )
        marketPricesFlow.value = prices

        val partners = listOf(
            RecyclingCompany(
                id = "c1",
                companyName = "Green Sole Recycling Technologies",
                materials = listOf("Plastic", "Rubber", "Footwear"),
                description = "Authorized recycling facility processing polymers and industrial post-consumer plastics.",
                officialWebsite = "https://www.greensole.com"
            ),
            RecyclingCompany(
                id = "c2",
                companyName = "Karo Sambhav E-Waste Recyclers",
                materials = listOf("Electronics", "TV", "Fridge", "Copper"),
                description = "CPCB-authorized Producer Responsibility Organization (PRO) for certified electronic waste management.",
                officialWebsite = "https://www.karosambhav.com"
            ),
            RecyclingCompany(
                id = "c3",
                companyName = "Chintan Environmental Research and Action Group",
                materials = listOf("Plastic", "Paper", "Glass", "Metal"),
                description = "Social enterprise collaborating with informal waste pickers and municipal recycling channels.",
                officialWebsite = "https://www.chintan-india.org"
            )
        )
        companiesFlow.value = partners
    }

    fun getMarketPrices(): Flow<List<MarketPrice>> = marketPricesFlow

    fun getRecyclingCompanies(): Flow<List<RecyclingCompany>> = companiesFlow
}
