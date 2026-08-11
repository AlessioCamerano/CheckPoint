package com.example.checkpoint.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface MainRepository {
    fun getStreamData(): Flow<String>
}

class MainRepositoryImpl : MainRepository {
    override fun getStreamData(): Flow<String> = flow {
        // Mock data representing a database or network stream
        var count = 0
        while (true) {
            emit("Checkpoint Data #$count")
            delay(2000)
            count++
        }
    }
}
