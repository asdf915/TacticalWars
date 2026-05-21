package com.example.tacticalwars

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class VictoryFragment : Fragment() {

    private var winnerTeam: Int = 0
    private var currentFrame = 1
    private val handler = Handler(Looper.getMainLooper())
    private val winnerUnitViews = mutableListOf<ImageView>()
    
    private val animationRunnable = object : Runnable {
        override fun run() {
            currentFrame = if (currentFrame >= 3) 1 else currentFrame + 1
            updateWinnerAnimations()
            handler.postDelayed(this, 500)
        }
    }

    companion object {
        private const val ARG_WINNER_TEAM = "winner_team"

        fun newInstance(winnerTeam: Int): VictoryFragment {
            val fragment = VictoryFragment()
            val args = Bundle()
            args.putInt(ARG_WINNER_TEAM, winnerTeam)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            winnerTeam = it.getInt(ARG_WINNER_TEAM)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_victory, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvVictoryMessage = view.findViewById<TextView>(R.id.tvVictoryMessage)
        val llWinnerUnitsRow1 = view.findViewById<LinearLayout>(R.id.llWinnerUnitsRow1)
        val llWinnerUnitsRow2 = view.findViewById<LinearLayout>(R.id.llWinnerUnitsRow2)
        val btnBackToHome = view.findViewById<Button>(R.id.btnBackToHome)

        val teamName = if (winnerTeam == 0) "ROJO" else "AZUL"
        tvVictoryMessage.text = "VICTORIA DEL EQUIPO $teamName"
        tvVictoryMessage.setTextColor(if (winnerTeam == 0) Color.RED else Color.CYAN)

        val unitPrefixes = listOf("infantry", "bazooka", "tank", "jet")

        for (i in 0 until 4) {
            val unitView = ImageView(requireContext())
            // Increased size in Victory Screen to 120dp
            val size = (resources.displayMetrics.density * 120).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(8, 0, 8, 0)
            unitView.layoutParams = params
            unitView.setPadding(8, 8, 8, 8)
            unitView.tag = unitPrefixes[i]
            
            // Mirror blue units so they face right
            if (winnerTeam == 1) {
                unitView.scaleX = -1f
            }
            
            // Infantry and Bazooka in Row 1 (top), Tank and Jet in Row 2 (bottom)
            if (i < 2) {
                llWinnerUnitsRow1.addView(unitView)
            } else {
                llWinnerUnitsRow2.addView(unitView)
            }
            
            winnerUnitViews.add(unitView)

            startDancingAnimation(unitView, i * 200L)
        }

        updateWinnerAnimations()
        handler.post(animationRunnable)

        btnBackToHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(animationRunnable)
    }

    private fun updateWinnerAnimations() {
        val teamStr = if (winnerTeam == 0) "red" else "blue"
        for (view in winnerUnitViews) {
            val prefix = view.tag as String
            
            // Priority: "still" with frame (the standard idle animation in this project)
            var resId = resources.getIdentifier("${prefix}still$teamStr$currentFrame", "drawable", requireContext().packageName)
            
            // Fallback: "still" without frame
            if (resId == 0) {
                resId = resources.getIdentifier("${prefix}still$teamStr", "drawable", requireContext().packageName)
            }

            // Fallback: "idle"
            if (resId == 0) {
                resId = resources.getIdentifier("${prefix}idle$teamStr$currentFrame", "drawable", requireContext().packageName)
            }

            // Final fallback
            if (resId == 0) {
                resId = resources.getIdentifier("$prefix$teamStr", "drawable", requireContext().packageName)
            }

            if (resId != 0) {
                view.setImageResource(resId)
            }
        }
    }

    private fun startDancingAnimation(view: View, delay: Long) {
        val jump = ObjectAnimator.ofFloat(view, "translationY", 0f, -80f).apply {
            duration = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            startDelay = delay
        }
        jump.start()
    }
}
