package de.martinwolff.hotdog_nothotdog_kotlin_new

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/*Just a simple Classifier class, which is used in most of the examples for classifying an
* incoming video stream or a single image
* */
class Classifier(activity: Activity) {

    private val TAG = "Classifier"

    // Path to the model
    private val MODEL_PATH = "model.tflite"

    // Properties of the image to classify
    private val DIM_BATCH_SIZE = 1
    private val DIM_PIXEL_SIZE = 3
    val DIM_IMG_SIZE_X = 224
    val DIM_IMG_SIZE_Y = 224

    private val IMG_MEAN = 0
    private val IMG_STD = 255.0f

    // Preallocated buffers for storing image data in
    private val intValues: IntArray = IntArray(DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y)

    // An instance of the driver class to run model inference with Tensorflow Lite
    private val tflite: Interpreter? = Interpreter(loadModelFile(activity))

    // A ByteBuffer to hold image data, to be feed into Tensorflow Lite as input
    private val imgData: ByteBuffer? = ByteBuffer.allocateDirect(4* DIM_BATCH_SIZE *
            DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE)

    // A float array to hold the output of the model
    private var output = arrayOf(FloatArray(1))

    fun classifyImage(bitmap: Bitmap) : Result {
        output[0][0] = -1.0f

        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized")
            return Result("Uninitialized Classifier", -1.0f, -1)
        }

        convertBitmapToByteBuffer(bitmap)
        val startTime: Long = SystemClock.uptimeMillis()
        tflite.run(imgData, output)
        val endTime: Long = SystemClock.uptimeMillis()
        val timeCost = endTime - startTime

        if (output[0][0] < 0.5) {
            Log.d(TAG, output[0][0].toString())
            return Result("No hotdog", output[0][0], timeCost)
        } else {
            Log.d(TAG, output[0][0].toString())
            return Result("Hotdog", output[0][0], timeCost)
        }
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap) {
        if (imgData == null) {
            Log.e(TAG, "Image data array was null")
            return
        }
        imgData.order(ByteOrder.nativeOrder())
        imgData.rewind()
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point values and extract the r,g,b values
        var pixel = 0
        for (i in 0 until DIM_IMG_SIZE_X) {
            for (j in 0 until DIM_IMG_SIZE_Y) {
                val value = intValues[pixel++]
                imgData.putFloat((((value shr 16) and 0xFF) - IMG_MEAN)/IMG_STD) // R
                imgData.putFloat((((value shr 8) and 0xFF) - IMG_MEAN)/IMG_STD) // B
                imgData.putFloat(((value and 0xFF) - IMG_MEAN)/IMG_STD) // G
            }
        }
    }

    private fun loadModelFile(activity: Activity): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = activity.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset: Long = fileDescriptor.startOffset
        val declaredLength: Long = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}

data class Result(val resultText: String, val resultProba: Float, val timeCost: Long)