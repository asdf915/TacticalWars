package com.example.tacticalwars

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class HomeFragment : Fragment() {

    private var animatorSet: AnimatorSet? = null
    private var mediaPlayer: MediaPlayer? = null
    private var musicVolume: Float = 0.5f
    private var sfxVolume: Float = 0.5f
    private var difficulty: String = "Normal"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMusic()

        val units = listOf(
            R.id.redUnit1, R.id.redUnit2, R.id.redUnit3, R.id.redUnit4,
            R.id.blueUnit1, R.id.blueUnit2, R.id.blueUnit3, R.id.blueUnit4
        ).map { view.findViewById<View>(it) }

        view.post {
            if (isAdded) {
                startParade(units, view.width.toFloat())
            }
        }

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun setupMusic() {
        mediaPlayer = MediaPlayer.create(context, R.raw.musica_guerra)
        mediaPlayer?.isLooping = true
        mediaPlayer?.setVolume(musicVolume, musicVolume)
        mediaPlayer?.start()
    }

    private fun showSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_settings, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        val sliderMusic = dialogView.findViewById<Slider>(R.id.sliderMusic)
        val sliderSFX = dialogView.findViewById<Slider>(R.id.sliderSFX)
        val spinnerDifficulty = dialogView.findViewById<Spinner>(R.id.spinnerDifficulty)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        sliderMusic.value = musicVolume
        sliderSFX.value = sfxVolume

        val difficulties = arrayOf("Fácil", "Normal", "Difícil", "Experto")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = adapter
        spinnerDifficulty.setSelection(difficulties.indexOf(difficulty))

        btnApply.setOnClickListener {
            musicVolume = sliderMusic.value
            sfxVolume = sliderSFX.value
            difficulty = spinnerDifficulty.selectedItem.toString()
            
            mediaPlayer?.setVolume(musicVolume, musicVolume)
            
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startParade(units: List<View>, screenWidth: Float) {
        if (!isAdded || view == null) return

        val lane = view?.findViewById<View>(R.id.llUnitLane) ?: return
        val laneLeft = lane.left.toFloat()

        val animators = units.mapIndexed { index, unit ->
            val unitAbsoluteLeft = laneLeft + unit.left
            val startX = screenWidth - unitAbsoluteLeft
            val endX = -unitAbsoluteLeft - unit.width.toFloat()
            
            unit.translationX = startX
            unit.alpha = 1f

            ObjectAnimator.ofFloat(unit, "translationX", startX, endX).apply {
                duration = 8000 
                startDelay = index * 1500L 
                interpolator = LinearInterpolator()
            }
        }

        animatorSet = AnimatorSet().apply {
            playTogether(animators)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view?.postDelayed({
                        if (isAdded) startParade(units, screenWidth)
                    }, 3000L)
                }
            })
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        animatorSet?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
