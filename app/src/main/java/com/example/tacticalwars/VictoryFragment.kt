package com.example.tacticalwars

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
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
        val llWinnerUnits = view.findViewById<LinearLayout>(R.id.llWinnerUnits)
        val btnBackToHome = view.findViewById<Button>(R.id.btnBackToHome)

        val teamName = if (winnerTeam == 0) "ROJO" else "AZUL"
        tvVictoryMessage.text = "VICTORIA DEL EQUIPO $teamName"
        tvVictoryMessage.setTextColor(if (winnerTeam == 0) Color.RED else Color.CYAN)


        for (i in 0 until 4) {
            val unitView = ImageView(requireContext())
            val size = (resources.displayMetrics.density * 64).toInt()
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(16, 0, 16, 0)
            unitView.layoutParams = params
            unitView.setBackgroundColor(if (winnerTeam == 0) Color.RED else Color.BLUE)
            unitView.setImageResource(getUnitIcon(i))
            unitView.setPadding(16, 16, 16, 16)
            llWinnerUnits.addView(unitView)

            startDancingAnimation(unitView, i * 200L)
        }

        btnBackToHome.setOnClickListener {
            parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }
    }

    private fun startDancingAnimation(view: View, delay: Long) {
        val jump = ObjectAnimator.ofFloat(view, "translationY", 0f, -50f).apply {
            duration = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            startDelay = delay
        }
        jump.start()
    }

    private fun getUnitIcon(index: Int): Int {
        return when (index % 4) {
            0 -> android.R.drawable.ic_menu_info_details
            1 -> android.R.drawable.ic_menu_gallery
            2 -> android.R.drawable.ic_menu_camera
            else -> android.R.drawable.ic_menu_compass
        }
    }
}
