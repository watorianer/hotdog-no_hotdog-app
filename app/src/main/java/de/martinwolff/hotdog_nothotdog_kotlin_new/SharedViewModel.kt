package de.martinwolff.hotdog_nothotdog_kotlin_new

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val bitmap: MutableLiveData<Bitmap> = MutableLiveData()

    fun setBitmap(input: Bitmap) {
        bitmap.value = input
    }

    fun getBitmap() : LiveData<Bitmap> {
        return bitmap
    }
}