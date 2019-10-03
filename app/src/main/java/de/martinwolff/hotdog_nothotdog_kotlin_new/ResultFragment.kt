package de.martinwolff.hotdog_nothotdog_kotlin_new

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import de.martinwolff.hotdog_nothotdog_kotlin_new.databinding.FragmentResultBinding
import java.io.IOException


/**
 * The Fragment where to display the image to classify, classify it and show
 * the results
 */
class ResultFragment : Fragment() {

    private val TAG = "ResultFragment"

    private lateinit var capturedImageView: ImageView
    private lateinit var resultView: TextView
    private lateinit var resultTimeView: TextView
    private lateinit var resultProbaView: TextView

    private lateinit var classifier: Classifier

    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentResultBinding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_result, container, false)

        capturedImageView = binding.imagePreview
        resultView = binding.resultView
        resultTimeView = binding.resultTimeView
        resultProbaView = binding.resultProbaView

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        try {
            Log.d(TAG, "In classifier initialization")
            classifier = Classifier(activity!!)
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to initialize the image classifier")
        }

        viewModel = ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
        viewModel.getBitmap().observe(viewLifecycleOwner, Observer {
            capturedImageView.setImageBitmap(it)

            Log.d(TAG, "Width: "+ it.width.toString())
            Log.d(TAG, "Height: "+ it.height.toString())

            val result = classifier.classifyImage(it)
            val placeholder = result.timeCost.toString() + " ms"

            resultView.text = result.resultText
            resultTimeView.text = placeholder
            resultProbaView.text = result.resultProba.toString()
        })
    }
}
