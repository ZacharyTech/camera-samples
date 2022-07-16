package com.example.android.camera2.video

import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *
 * @author liz
 * @since 2022/7/7
 */
object C {

    const val TAG = "CVDO"

}



suspend fun View.setSelectedEx(selected: Boolean) {
    withContext(Dispatchers.Main) {
        isSelected = selected
    }
}

