package com.henry.towerrush.media

import android.content.Context
import android.media.MediaPlayer
import com.henry.towerrush.R

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private var bgPlayer: MediaPlayer? = null
    private var newRecordPlayer: MediaPlayer? = null

    fun startBackgroundMusic() {
        try {
            if (bgPlayer == null) {
                bgPlayer = MediaPlayer.create(appContext, R.raw.background_music).apply {
                    isLooping = true
                    setVolume(0.4f, 0.4f)
                    start()
                }
            } else if (!bgPlayer!!.isPlaying) {
                bgPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopBackgroundMusic() {
        try {
            bgPlayer?.pause()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playBlockPlace() {
        playSfx(R.raw.block_place)
    }

    fun playMagicEvent() {
        playSfx(R.raw.magic_event)
    }

    fun playNewRecord() {
        stopBackgroundMusic()
        try {
            newRecordPlayer?.release()
            newRecordPlayer = MediaPlayer.create(appContext, R.raw.new_record).apply {
                isLooping = true
                setVolume(0.7f, 0.7f)
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopNewRecord() {
        try {
            newRecordPlayer?.stop()
            newRecordPlayer?.release()
            newRecordPlayer = null
        } catch (e: Exception) {}
    }

    private fun playSfx(resId: Int) {
        try {
            val mp = MediaPlayer.create(appContext, resId)
            mp.setOnCompletionListener { it.release() }
            mp.setVolume(0.6f, 0.6f)
            mp.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            bgPlayer?.release()
            bgPlayer = null
            newRecordPlayer?.release()
            newRecordPlayer = null
        } catch (e: Exception) {}
    }
}
