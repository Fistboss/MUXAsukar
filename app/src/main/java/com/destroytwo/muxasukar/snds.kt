package com.destroytwo.muxasukar

import android.content.Context
import android.media.SoundPool

object SoundManager {
    private var soundPool: SoundPool? = null
    private var soundId1: Int = 0
    private var currentStreamId: Int = 0 // Сохраняем ID играющего звука
    private var isLoaded = false

    fun init(context: Context) {
        if (soundPool != null) return // Защита от повторной инициализации

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .build()

        // Загружаем звук ОДИН раз
        soundId1 = pool.load(context, R.raw.similarin, 1)

        // Ждем загрузки и ставим флаг
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0 && sampleId == soundId1) {
                isLoaded = true
            }
        }
        soundPool = pool
    }

    fun playSound(why: Int) {
        if (!isLoaded) return // Не играем, если звук еще грузится

        if (why == 0) {
            // Останавливаем предыдущий звук, если он еще играет
            stopSound()
            // Запускаем и СОХРАНЯЕМ streamId
            currentStreamId = soundPool?.play(soundId1, 1f, 1f, 0, 0, 1f) ?: 0
        }
    }

    fun stopSound() {
        if (currentStreamId != 0) {
            soundPool?.stop(currentStreamId)
            currentStreamId = 0
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        isLoaded = false
    }
}