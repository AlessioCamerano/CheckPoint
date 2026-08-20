package com.example.checkpoint.data.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository per la gestione dei post e dei dati principali.
 */
interface MainRepository {
    fun getStreamData(): Flow<String>
}

class MainRepositoryImpl : MainRepository {
    override fun getStreamData(): Flow<String> = flow {
        var count = 0
        while (true) {
            emit("Checkpoint Data #$count")
            delay(2000)
            count++
        }
    }
}
