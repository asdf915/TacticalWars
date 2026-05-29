package com.example.tacticalwars

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.fragment.app.Fragment

class BattleFragment : Fragment() {

    private var attackerType: String = ""
    private var attackerTeam: Int = 0
    private var attackerMaxHP: Int = 10
    private var attackerCurrentHP: Int = 10
    
    private var targetType: String = ""
    private var targetTeam: Int = 0
    private var targetMaxHP: Int = 10
    private var targetCurrentHP: Int = 10
    
    private var damage: Int = 0

    private lateinit var ivUnitBlue: ImageView
    private lateinit var ivUnitRed: ImageView
    private lateinit var pbHealthBlue: ProgressBar
    private lateinit var pbHealthRed: ProgressBar
    private lateinit var ivFireEffect: ImageView

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        fun newInstance(
            attackerType: String, attackerTeam: Int, attackerMaxHP: Int, attackerCurrentHP: Int,
            targetType: String, targetTeam: Int, targetMaxHP: Int, targetCurrentHP: Int,
            damage: Int
        ): BattleFragment {
            val fragment = BattleFragment()
            val args = Bundle().apply {
                putString("attackerType", attackerType)
                putInt("attackerTeam", attackerTeam)
                putInt("attackerMaxHP", attackerMaxHP)
                putInt("attackerCurrentHP", attackerCurrentHP)
                putString("targetType", targetType)
                putInt("targetTeam", targetTeam)
                putInt("targetMaxHP", targetMaxHP)
                putInt("targetCurrentHP", targetCurrentHP)
                putInt("damage", damage)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            attackerType = it.getString("attackerType", "")
            attackerTeam = it.getInt("attackerTeam", 0)
            attackerMaxHP = it.getInt("attackerMaxHP", 10)
            attackerCurrentHP = it.getInt("attackerCurrentHP", 10)
            targetType = it.getString("targetType", "")
            targetTeam = it.getInt("targetTeam", 0)
            targetMaxHP = it.getInt("targetMaxHP", 10)
            targetCurrentHP = it.getInt("targetCurrentHP", 10)
            damage = it.getInt("damage", 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_battle, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivUnitBlue = view.findViewById(R.id.ivUnitBlue)
        ivUnitRed = view.findViewById(R.id.ivUnitRed)
        pbHealthBlue = view.findViewById(R.id.pbHealthBlue)
        pbHealthRed = view.findViewById(R.id.pbHealthRed)
        ivFireEffect = view.findViewById(R.id.ivFireEffect)

        // Mirror blue units so they face right
        ivUnitBlue.scaleX = -1f

        setupInitialState()
        startBattleSequence()
    }

    private fun playGunshotSound() {
        context?.let { ctx ->
            try {
                val mediaPlayer = MediaPlayer.create(ctx, R.raw.sonido_disparo)
                mediaPlayer?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .build()
                )
                mediaPlayer?.setOnCompletionListener { 
                    it.stop()
                    it.release() 
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupInitialState() {
        val (blueUnit, redUnit) = if (attackerTeam == 1) {
            Pair(attackerType, targetType)
        } else {
            Pair(targetType, attackerType)
        }

        ivUnitBlue.setImageResource(getIdleRes(blueUnit, 1))
        ivUnitRed.setImageResource(getIdleRes(redUnit, 0))

        if (attackerTeam == 1) {
            pbHealthBlue.max = attackerMaxHP * 10
            pbHealthBlue.progress = attackerCurrentHP * 10
            pbHealthRed.max = targetMaxHP * 10
            pbHealthRed.progress = targetCurrentHP * 10
        } else {
            pbHealthBlue.max = targetMaxHP * 10
            pbHealthBlue.progress = targetCurrentHP * 10
            pbHealthRed.max = attackerMaxHP * 10
            pbHealthRed.progress = attackerCurrentHP * 10
        }
    }

    private fun getIdleRes(type: String, team: Int): Int {
        val teamStr = if (team == 0) "red" else "blue"
        var resId = resources.getIdentifier("${type}idle${teamStr}", "drawable", requireContext().packageName)
        if (resId == 0) {
            resId = resources.getIdentifier("$type$teamStr", "drawable", requireContext().packageName)
        }
        return resId
    }

    private fun startBattleSequence() {
        if (attackerType == "tank" || attackerType == "jet") {
            animateTankJetBattle()
        } else {
            animateInfantryBattle()
        }
    }

    private fun animateTankJetBattle() {
        val targetImg = if (attackerTeam == 1) ivUnitRed else ivUnitBlue
        val targetPb = if (attackerTeam == 1) pbHealthRed else pbHealthBlue

        var frame = 1
        val fireRunnable = object : Runnable {
            override fun run() {
                val resName = "fire$frame"
                val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
                
                // Siempre intentamos reproducir el sonido en el primer frame de la acción
                if (frame == 1) playGunshotSound()

                if (resId != 0) {
                    ivFireEffect.setImageResource(resId)
                    ivFireEffect.visibility = View.VISIBLE
                    ivFireEffect.x = targetImg.x + (targetImg.width / 4)
                    ivFireEffect.y = targetImg.y + (targetImg.height / 4)
                }

                if (frame == 1) {
                    frame = 2
                    handler.postDelayed(this, 500)
                } else {
                    handler.postDelayed({
                        ivFireEffect.visibility = View.GONE
                        applyDamageAnimation(targetImg, targetPb)
                    }, 500)
                }
            }
        }
        handler.post(fireRunnable)
    }

    private fun animateInfantryBattle() {
        val attackerImg = if (attackerTeam == 1) ivUnitBlue else ivUnitRed
        val teamStr = if (attackerTeam == 0) "red" else "blue"
        val targetImg = if (attackerTeam == 1) ivUnitRed else ivUnitBlue
        val targetPb = if (attackerTeam == 1) pbHealthRed else pbHealthBlue

        startAssemblyAnimation(attackerImg, teamStr, targetImg, targetPb)
    }

    private fun startAssemblyAnimation(attackerImg: ImageView, teamStr: String, targetImg: ImageView, targetPb: ProgressBar) {
        val originalScaleX = attackerImg.scaleX
        val originalScaleY = attackerImg.scaleY
        attackerImg.scaleX = originalScaleX * 1.5f
        attackerImg.scaleY = originalScaleY * 1.5f

        var assemblyFrame = 1
        val assemblyRunnable = object : Runnable {
            override fun run() {
                var resId = resources.getIdentifier("assembly${attackerType}$teamStr$assemblyFrame", "drawable", requireContext().packageName)
                if (resId == 0) {
                    resId = resources.getIdentifier("assembly${attackerType}aim$teamStr$assemblyFrame", "drawable", requireContext().packageName)
                }

                if (assemblyFrame == 1) playGunshotSound()

                if (resId != 0) {
                    attackerImg.setImageResource(resId)
                    assemblyFrame++
                    handler.postDelayed(this, 500)
                } else {
                    attackerImg.scaleX = originalScaleX
                    attackerImg.scaleY = originalScaleY
                    attackerImg.setImageResource(getIdleRes(attackerType, attackerTeam))
                    applyDamageAnimation(targetImg, targetPb)
                }
            }
        }
        handler.post(assemblyRunnable)
    }

    private fun applyDamageAnimation(targetImg: ImageView, targetPb: ProgressBar) {
        val flashRunnable = object : Runnable {
            var count = 0
            override fun run() {
                targetImg.visibility = if (targetImg.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE
                if (count < 8) {
                    count++
                    handler.postDelayed(this, 100)
                } else {
                    targetImg.visibility = View.VISIBLE
                    val newProgress = Math.max(0, targetPb.progress - (damage * 10))
                    targetPb.progress = newProgress
                    
                    handler.postDelayed({
                        parentFragmentManager.setFragmentResult("battle_done", Bundle())
                        parentFragmentManager.popBackStack()
                    }, 1000)
                }
            }
        }
        handler.post(flashRunnable)
    }
}
