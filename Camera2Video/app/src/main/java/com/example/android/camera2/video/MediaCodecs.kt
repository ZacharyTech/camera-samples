package com.example.android.camera2.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList

/**
 *
 * @author liz
 * @since 2022/7/12
 */
object MediaCodecs {

    fun selectEncoder(mimeType: String): MediaCodec? {
        return selectCodec(mimeType, true)
    }

    fun selectDecoder(mimeType: String): MediaCodec? {
        return selectCodec(mimeType, false)
    }


    private fun selectCodec(mimeType: String, encoder: Boolean): MediaCodec? {
        val codecInfo = MediaCodecList(MediaCodecList.ALL_CODECS)
            .codecInfos
            .firstOrNull { it.isEncoder == encoder }

        return codecInfo
            ?.supportedTypes
            ?.firstOrNull { it.equals(mimeType, true) }
            ?.let { MediaCodec.createByCodecName(codecInfo.name) }
    }

}