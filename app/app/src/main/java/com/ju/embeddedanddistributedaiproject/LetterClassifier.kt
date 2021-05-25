package com.ju.embeddedanddistributedaiproject

import android.content.Context
import android.graphics.*
import android.util.Log
import com.ju.embeddedanddistributedaiproject.ml.LetterClassificationModel
import com.vansuita.gaussianblur.GaussianBlur
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Handles the classification of drawn letters
class LetterClassifier(passedContext: Context) {
    private val context: Context = passedContext
    private var model: LetterClassificationModel? = null

    init {
        model = LetterClassificationModel.newInstance(context)
    }

    // Takes the drawing of the user, processes it and returns the index of the identified class
    fun classify(bitmap: Bitmap) : Int {

        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 28, 28, 1), DataType.FLOAT32)
        inputFeature0.loadBuffer(preprocessInput(bitmap))

        val outputs = model!!.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val result = outputFeature0.floatArray

        var bestValue = Float.NEGATIVE_INFINITY
        var bestIndex = 0

        for (i in result.indices) {
            if (result[i] > bestValue) {
                bestValue = result[i]
                bestIndex = i
            }
        }

        return bestIndex
    }

    // Gets the area on the bitmap where the user, has drawn, returns it as a 1:1 aspect ratio blurred bitmap
    private fun getAreaOfInterest(src: Bitmap, paddingRatio: Float, blurRadius: Int) : Bitmap {
        val pixels = IntArray(src.width * src.height)

        src.getPixels(pixels, 0, src.width, 0, 0, src.width, src.height)
        var top = src.height
        var left = src.width
        var bottom = 0
        var right = 0

        var foundPixel = false

        for (x in 0 until src.width) {
            for (y in 0 until src.height) {
                val currentPixel = pixels[x + y * src.width]

                if ((currentPixel and 0xff) < 0xff) {
                    foundPixel = true
                    if (x < left) {
                        left = x
                    }
                    if (x > right) {
                        right = x
                    }
                    if (y < top) {
                        top = y
                    }
                    if (y > bottom) {
                        bottom = y
                    }
                }
            }
        }

        if (!foundPixel)
            return src

        val croppedBitmap = Bitmap.createBitmap(src, left, top, right - left, bottom - top)

        val padding : Int

        var verticalDiff = 0
        var horizontalDiff = 0

        if (croppedBitmap.width < croppedBitmap.height) {
            horizontalDiff = croppedBitmap.height - croppedBitmap.width
            padding = (croppedBitmap.height * paddingRatio).toInt()
        } else {
            verticalDiff = croppedBitmap.width - croppedBitmap.height
            padding = (croppedBitmap.width * paddingRatio).toInt()
        }

        val paddedBitmap = Bitmap.createBitmap(croppedBitmap.width + padding * 2 + horizontalDiff, croppedBitmap.height + padding * 2 + verticalDiff, croppedBitmap.config)
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(croppedBitmap, padding.toFloat() + horizontalDiff.toFloat() / 2f, padding.toFloat() + verticalDiff.toFloat() / 2f, null)

        val blurredBitmap: Bitmap = GaussianBlur.with(context).radius(blurRadius).size(100f).render(paddedBitmap)
        
        return blurredBitmap
    }

    // Prepares the bitmap before it is processed by the neural network
    private fun preprocessInput(src: Bitmap) : ByteBuffer {
        // Get area of interest on the bitmap
        val areaOfInterest = getAreaOfInterest(src, 0.05f, 3)

        // Resize to 28x28
        val resizedBitmap = Bitmap.createScaledBitmap(areaOfInterest, 28, 28, true)

        // Rotate and flip the bitmap to match the orientation of the images in the training dataset
        val rotate: Matrix = Matrix()
        rotate.postRotate(90f)
        val rotatedBitmap = Bitmap.createBitmap(resizedBitmap,0, 0, resizedBitmap.width, resizedBitmap.height, rotate, true)

        val flip: Matrix = Matrix()
        flip.postScale(-1f, 1f, rotatedBitmap.width / 2f, rotatedBitmap.height / 2f)
        val flippedBitmap = Bitmap.createBitmap(rotatedBitmap,0, 0, rotatedBitmap.width, rotatedBitmap.height, flip, true)

        val bitmap = Bitmap.createBitmap(28, 28, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Invert the colors of the bitmap to match the colors of the images in the training dataset

        val matrixGrayscale = ColorMatrix()
        matrixGrayscale.setSaturation(0f)

        val matrixInvert = ColorMatrix()
        matrixInvert.set(
            floatArrayOf(
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
        matrixInvert.preConcat(matrixGrayscale)

        val filter = ColorMatrixColorFilter(matrixInvert)
        paint.colorFilter = filter

        canvas.drawBitmap(flippedBitmap, 0f, 0f, paint)

        val inputBuffer = ByteBuffer.allocateDirect(28 * 28 * 4)

        // Match endianness
        inputBuffer.order(ByteOrder.nativeOrder())

        // Push pixels to a bytebuffer
        val pixels = IntArray(28 * 28)
        bitmap.getPixels(pixels, 0, 28, 0, 0, 28, 28)
        for (i in pixels.indices) {
            // Since our bitmap is in grayscale, we can simply get the blue channel to get a value between 0 and 255
            inputBuffer.putFloat((pixels[i] and 0xff).toFloat())
        }

        return inputBuffer
    }

    private fun finalize() {
        model?.close()
    }
}