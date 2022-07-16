package com.example.android.camera2.video.fragments

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.android.camera2.video.AudioCapture
import com.example.android.camera2.video.setSelectedEx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vdo.android.databinding.FragmentAudioViewBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 * @author liz
 * @since 2022/7/13
 */
class AudioFragment : Fragment() {

    private var _fragmentBinding: FragmentAudioViewBinding? = null
    private val fragmentBinding get() = _fragmentBinding!!


    private val mAudio by lazy { AudioCapture() }
    private val mMuxer by lazy {
        val path = createFile(requireContext(),"mp4").path
        MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _fragmentBinding = FragmentAudioViewBinding.inflate(inflater, container, false)
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() = lifecycleScope.launch(Dispatchers.Main) {
        fragmentBinding.captureButton.setOnClickListener {
            when(!it.isSelected) {
                true -> lifecycleScope.launch(Dispatchers.IO) {
                    it.setSelectedEx(true)
                    start()
                }
                false -> lifecycleScope.launch(Dispatchers.IO) {
                    it.setSelectedEx(false)
                    stop()
                }
            }
        }
    }


    private fun start() {
        mAudio.start(object : AudioCapture.Callback {
            var _audioTrack = -1
            val audioTrack get() = _audioTrack

            override fun onFormatChanged(format: MediaFormat) {
                Log.d(TAG, "onFormatChanged: ")
                if (_audioTrack == -1) {
                    // _audioTrack = mMuxer.addTrack(format)
//                    mMuxer.start()
                    _audioTrack = 1
                }


            }

            override fun onOutputBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
                if (audioTrack != -1) {
                    mMuxer.writeSampleData(audioTrack, buffer, info)
                }
            }

            override fun onError(e: MediaCodec.CodecException) {
                Log.d(TAG, "onError: ")
            }

            val outputStream by lazy {
                val file = createFile(requireContext(), "mp4")
                Log.d(TAG, "AAC: $file")
                FileOutputStream(file)
            }

            override fun onRawData(byteArray: ByteArray, size: Int) {
                outputStream.write(byteArray, 0, size)
            }
        })
        //mMuxer.start()
    }

    private fun stop() {
        mAudio.stop()
        mMuxer.stop()
    }


    companion object {
        private val TAG = SurfaceViewFragment::class.java.simpleName

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L

        /** Creates a [File] named with the current date and time */
        private fun createFile(context: Context, extension: String): File {
             val root = context.filesDir
//            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val atest = File(root, "1vdo")
            if (!atest.isDirectory) {
                atest.mkdirs()
            }
            val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
            return File(atest, "AID_${sdf.format(Date())}.$extension")
        }
    }
}
