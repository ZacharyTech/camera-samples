package com.example.android.camera2.video

import android.annotation.SuppressLint
import android.media.*
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import androidx.annotation.IntRange
import com.example.android.camera2.video.C.TAG
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.receiveOrNull
import kotlinx.coroutines.channels.sendBlocking
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 *
 * @author liz
 * @since 2022/7/12
 */
class AudioCapture(
    private val sampleRate: Int = 44_100,
    private val encoding: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO
) {

    private val channelCount = getChannelCount(channelConfig)
    private val audioRecord: AudioRecord by lazy { createAudioRecord() }
    private val audioEncoder: MediaCodec? by lazy { createAudioEncoder() }
    private val minBufferSize by lazy {
        AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            encoding
        )
    }
    private val bufs = ConcurrentLinkedQueue<Pair<Int, ByteArray>>()

    private val bitRate = 128_1000
    private var recording = false


    fun start(callback: Callback) {
//        if (audioEncoder == null) {
//            throw NullPointerException("Unsupport codec")
//        }
        val mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
        val audioFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, minBufferSize*2)
        }
        val audioEncoderThread = HandlerThread("")
        audioEncoderThread.start()
        val backgroundHandler = Handler(audioEncoderThread.looper)

        val buf = ByteArray(minBufferSize)
        audioEncoder!!.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val inputBuffer = codec.getInputBuffer(index)
                if (inputBuffer != null) {
                    val size = audioRecord.read(buf, 0, buf.size)
                    Log.d(TAG, "inputBuffer limit: ${inputBuffer.capacity()} onInputBufferAvailable: $size")
                    inputBuffer.put(buf, 0, size)
                    codec.queueInputBuffer(index, 0, size, System.nanoTime() / 1000, 0)
                }
                if (!recording) {
                    codec.stop()
                    codec.release()
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                val outputBuffer = codec.getOutputBuffer(index)
                if (outputBuffer != null) {
                    callback.onOutputBufferAvailable(outputBuffer, info)
                }
                codec.releaseOutputBuffer(index, false)
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                callback.onError(e)
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                callback.onFormatChanged(format)
            }

        }, backgroundHandler)
        recording = true
//        thread {
//            while (recording) {
//                val beginTime = System.currentTimeMillis()
//                val buf = ByteArray(minBufferSize)
//                val size = audioRecord.read(buf, 0, buf.size)
//                val endTime = System.currentTimeMillis()
//                Log.d(TAG, "audio: 耗时 ${endTime - beginTime}, 数据大小: $size")
//                if (size > 0) {
//                   //bufs.offer(Pair(size, buf))
//                    callback.onRawData(buf, size)
//                }
//            }
//        }
        audioRecord.startRecording()
        audioEncoder!!.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioEncoder!!.start()
    }

    fun stop() {
        recording = false
        audioRecord.stop()
        audioRecord.release()
    }

    private fun createAudioEncoder(): MediaCodec? {
        val mimeType = MediaFormat.MIMETYPE_AUDIO_AAC
        return MediaCodecs.selectEncoder(mimeType)
    }


    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): AudioRecord {
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(encoding)
            .setChannelMask(channelConfig)
            .build()

        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBufferSize)
            .build()
    }

    /**
     * return channel count by channelConfig
     */
    private fun getChannelCount(channelConfig: Int): Int =
        when (channelConfig) {
            AudioFormat.CHANNEL_IN_DEFAULT,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.CHANNEL_CONFIGURATION_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.CHANNEL_CONFIGURATION_STEREO,
            AudioFormat.CHANNEL_IN_FRONT or AudioFormat.CHANNEL_IN_BACK -> 2
            // AudioFormat.CHANNEL_INVALID
            else -> {
                Log.d(TAG, "getChannelCount():  Invalid channel configuration.")
                AudioRecord.ERROR_BAD_VALUE
            }
        }



    interface Callback {

        fun onFormatChanged(format: MediaFormat)

        fun onOutputBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo)

        fun onError(e: MediaCodec.CodecException)

        fun onRawData(byteArray: ByteArray, size: Int) {

        }
    }

}