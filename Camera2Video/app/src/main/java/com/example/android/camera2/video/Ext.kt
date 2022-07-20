package com.example.android.camera2.video

import android.util.Size

/**
 *
 * @author liz
 * @since 2022/7/20
 */

fun Size.is720P(): Boolean {
    return width == 1280 && height == 720
}

fun Size.is1080P(): Boolean {
    return width == 1920 && height == 1080
}

fun Size.is4K(): Boolean {
    return width == 3840 && height == 2160
}



