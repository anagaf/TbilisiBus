package com.anagaf.tbilisibus.data

interface SituationProvider {
    suspend fun getSituation(routeNumber: Int): Situation
}