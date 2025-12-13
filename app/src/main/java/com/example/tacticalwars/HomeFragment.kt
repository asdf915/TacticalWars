package com.example.tacticalwars

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private var animatorSet: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val units = listOf(
            R.id.redUnit1, R.id.redUnit2, R.id.redUnit3, R.id.redUnit4,
            R.id.blueUnit1, R.id.blueUnit2, R.id.blueUnit3, R.id.blueUnit4
        ).map { view.findViewById<View>(it) }

        
        view.post {
            if (isAdded) {
                startParade(units, view.width.toFloat())
            }
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        animatorSet?.cancel()
    }
}
