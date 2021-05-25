package com.ju.embeddedanddistributedaiproject

import android.graphics.Bitmap
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.ju.embeddedanddistributedaiproject.databinding.FragmentFirstBinding
import android.util.Log

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private var classifier: LetterClassifier? = null

    private var text: String = ""

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        classifier = LetterClassifier(requireContext())
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        binding.textviewFirst.text = text
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Classify the letter drawn by the user, wipe the drawing view/canvas, and append the letter
        binding.buttonFirst.setOnClickListener {
            val result = classifier!!.classify(binding.drawingView.getBitmap())
            binding.drawingView.resetDrawing()
            text += (result + 'A'.code).toChar()
            binding.textviewFirst.text = text
        }

        // Clear the letters classified
        binding.buttonClear.setOnClickListener {
            text = ""
            binding.textviewFirst.text = text
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}