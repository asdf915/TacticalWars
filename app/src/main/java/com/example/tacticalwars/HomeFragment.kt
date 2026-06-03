package com.example.tacticalwars

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class HomeFragment : Fragment() {

    private var animatorSets: MutableList<AnimatorSet> = mutableListOf()
    private var mediaPlayer: MediaPlayer? = null
    private var musicVolume: Float = 0.5f
    private var sfxVolume: Float = 0.5f
    private var difficulty: String = "Normal"

    private var currentFrame = 1
    private val handler = Handler(Looper.getMainLooper())
    private val animationRunnable = object : Runnable {
        override fun run() {
            currentFrame = if (currentFrame >= 3) 1 else currentFrame + 1
            updateUnitImages()
            handler.postDelayed(this, 500)
        }
    }

    private var redUnits: List<ImageView> = emptyList()
    private var blueUnits: List<ImageView> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMusic()

        redUnits = listOf(R.id.redUnit1, R.id.redUnit2, R.id.redUnit3, R.id.redUnit4).map { view.findViewById<ImageView>(it) }
        blueUnits = listOf(R.id.blueUnit1, R.id.blueUnit2, R.id.blueUnit3, R.id.blueUnit4).map { view.findViewById<ImageView>(it) }

        updateUnitImages()
        handler.post(animationRunnable)

        view.post {
            if (isAdded) {
                val screenWidth = view.width.toFloat()
                // Blue starts immediately, Red starts with a delay of 3 seconds
                startParade(blueUnits, R.id.llBlueLane, screenWidth, 0L)
                startParade(redUnits, R.id.llRedLane, screenWidth, 3000L)
            }
        }

        view.findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        view.findViewById<ImageButton>(R.id.btnVictoryBlue).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, VictoryFragment.newInstance(1))
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MapSelectionFragment.newInstance(difficulty))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateUnitImages() {
        if (!isAdded) return
        val prefixes = listOf("infantry", "bazooka", "tank", "jet")

        redUnits.forEachIndexed { index, imageView ->
            val resName = "${prefixes[index]}stillred$currentFrame"
            val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
            if (resId != 0) imageView.setImageResource(resId)
        }

        blueUnits.forEachIndexed { index, imageView ->
            val resName = "${prefixes[index]}stillblue$currentFrame"
            val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
            if (resId != 0) imageView.setImageResource(resId)
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

        val difficulties = arrayOf("Fácil", "Normal", "Difícil")
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

    private fun startParade(units: List<View>, laneId: Int, screenWidth: Float, initialDelay: Long) {
        if (!isAdded || view == null) return

        val lane = view?.findViewById<View>(laneId) ?: return
        val laneLeft = lane.left.toFloat()

        val animators = units.mapIndexed { index, unit ->
            val unitAbsoluteLeft = laneLeft + unit.left
            val startX = screenWidth - unitAbsoluteLeft
            val endX = -unitAbsoluteLeft - unit.width.toFloat()

            unit.translationX = startX
            unit.alpha = 1f

            ObjectAnimator.ofFloat(unit, "translationX", startX, endX).apply {
                duration = 8000
                startDelay = initialDelay + (index * 1500L)
                interpolator = LinearInterpolator()
            }
        }

        val animatorSet = AnimatorSet().apply {
            playTogether(animators)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view?.postDelayed({
                        if (isAdded) startParade(units, laneId, screenWidth, 0L)
                    }, 3000L)
                }
            })
            start()
        }
        animatorSets.add(animatorSet)
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

        animatorSets.forEach { it.cancel() }
        animatorSets.clear()
        handler.removeCallbacks(animationRunnable)

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroyView()
    }
}