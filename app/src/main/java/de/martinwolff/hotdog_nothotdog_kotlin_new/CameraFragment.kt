package de.martinwolff.hotdog_nothotdog_kotlin_new


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import de.martinwolff.hotdog_nothotdog_kotlin_new.databinding.FragmentCameraBinding
import java.nio.ByteBuffer

// This is an arbitrary number we are using to keep tab of the permission
// request. Where an app has multiple context for requesting permission,
// this can help differentiate the different contexts
private const val REQUEST_CODE_PERMISSIONS = 1

// This is an array of all the permission specified in the manifest
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

/**
 * The main fragment, where we take the photo for inference
 */
class CameraFragment : Fragment() {

    private val TAG = "CameraFragment"

    private lateinit var viewFinder: TextureView
    private lateinit var captureButton: ImageButton
    private lateinit var navController: NavController

    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentCameraBinding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_camera, container, false)

        // Initialize all the elements of the layout
        viewFinder = binding.viewFinder
        captureButton = binding.captureButton

        // Initialize the ViewModel, we do not add an observer because we don't show anything
        // regarding this ViewModel on the UI
        viewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)

        // Request camera permissions
        // If the the permission was already granted just start the camera
        // If the permission was not yet granted, so for the first start, we request the permissions
        // from the user
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
    }


    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    activity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity?.baseContext!!, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
        }.build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // Try to get the image in the desired resolution for inference
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                setLensFacing(CameraX.LensFacing.BACK)
                setTargetResolution(Size(224, 224))
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        captureButton.setOnClickListener {
            imageCapture.takePicture(object : ImageCapture.OnImageCapturedListener() {
                override fun onCaptureSuccess(imageProxy: ImageProxy, rotationDegrees: Int) {

                    // Get the underlying image, which should be a jpg image given the documentation
                    // with only one plane
                    val capturedImage: Image? = imageProxy.image

                    // Convert image to bitmap
                    val bitmapImage: Bitmap? = imageToBitmap(capturedImage)

                    // Rotate and resize the bitmap
                    val finalBitmapImage: Bitmap? = rescaleAndRotateBitmap(bitmapImage)
                    bitmapImage?.recycle()

                    viewModel.setBitmap(finalBitmapImage!!)

                    navController.navigate(R.id.action_cameraFragment_to_resultFragment)

                    // Close the image Proxy
                    imageProxy.close()
                }

                override fun onError(
                    error: ImageCapture.UseCaseError,
                    message: String, exc: Throwable?
                ) {
                    val msg = "Photo capture failed: $message"
                    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, msg)
                    exc?.printStackTrace()
                }
            })
        }
        // Bind image capture to lifecycle
        CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture)
    }

    // TODO Add landscape support
    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when(viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    // Helper function to convert the ImageProxy to a Bitmap
    private fun imageToBitmap(image: Image?): Bitmap? {
        if (image != null) {
            // The incoming android.media.Image should be in JPG format, which only
            // has one plane
            val buffer: ByteBuffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        }
        else {
            Log.e("Image to bitmap", "Image is null")
            return null
        }
    }

    // Helper function to resize and rotate the bitmap image
    // This function only resize and rotates the bitmap to fixed values
    private fun rescaleAndRotateBitmap(bitmap: Bitmap?, degree: Float = 90F): Bitmap? {

        // First rotate the bitmap
        val matrix = Matrix()
        matrix.postRotate(degree)
        return if (bitmap != null) {
            val rotatedBitmap: Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width,
                bitmap.height, matrix, true)
            bitmap.recycle()

            // THis code is new - Create centercrop first
            val croppedBitmap: Bitmap
            if (rotatedBitmap.width >= rotatedBitmap.height) {
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap,
                    rotatedBitmap.width/2 - rotatedBitmap.height/2,0,
                    rotatedBitmap.height, rotatedBitmap.height)
                rotatedBitmap.recycle()
            } else {
                croppedBitmap = Bitmap.createBitmap(rotatedBitmap, 0,
                    rotatedBitmap.height/2 - rotatedBitmap.width/2, rotatedBitmap.width,
                    rotatedBitmap.width)
                rotatedBitmap.recycle()
            }

            // Now resize the rotated bitmap and return it .NEW: resize the cropped and rotated
            val resizedBitmap: Bitmap = Bitmap.createScaledBitmap(croppedBitmap, 224,
                224, true)
            croppedBitmap.recycle()
            resizedBitmap
        } else {
            Log.e(TAG, "Bitmap rotation failed, because bitmap was null")
            null
        }
    }
}
