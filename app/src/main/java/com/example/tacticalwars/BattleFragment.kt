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


    private var isRemote: Boolean = false

    private lateinit var ivUnitBlue: ImageView
    private lateinit var ivUnitRed: ImageView
    private lateinit var pbHealthBlue: ProgressBar
    private lateinit var pbHealthRed: ProgressBar
    private lateinit var ivFireEffect: ImageView

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val KEY_ATTACKER_TYPE       = "attackerType"
        private const val KEY_ATTACKER_TEAM       = "attackerTeam"
        private const val KEY_ATTACKER_MAX_HP     = "attackerMaxHP"
        private const val KEY_ATTACKER_CURRENT_HP = "attackerCurrentHP"
        private const val KEY_TARGET_TYPE         = "targetType"
        private const val KEY_TARGET_TEAM         = "targetTeam"
        private const val KEY_TARGET_MAX_HP       = "targetMaxHP"
        private const val KEY_TARGET_CURRENT_HP   = "targetCurrentHP"
        private const val KEY_DAMAGE              = "damage"
        private const val KEY_IS_REMOTE           = "isRemote"

        fun newInstance(
            attackerType: String,
            attackerTeam: Int,
            attackerMaxHP: Int,
            attackerCurrentHP: Int,
            targetType: String,
            targetTeam: Int,
            targetMaxHP: Int,
            targetCurrentHP: Int,
            damage: Int,
            isRemote: Boolean = false
        ): BattleFragment = BattleFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_ATTACKER_TYPE,       attackerType)
                putInt   (KEY_ATTACKER_TEAM,       attackerTeam)
                putInt   (KEY_ATTACKER_MAX_HP,     attackerMaxHP)
                putInt   (KEY_ATTACKER_CURRENT_HP, attackerCurrentHP)
                putString(KEY_TARGET_TYPE,         targetType)
                putInt   (KEY_TARGET_TEAM,         targetTeam)
                putInt   (KEY_TARGET_MAX_HP,       targetMaxHP)
                putInt   (KEY_TARGET_CURRENT_HP,   targetCurrentHP)
                putInt   (KEY_DAMAGE,              damage)
                putBoolean(KEY_IS_REMOTE,          isRemote)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            attackerType      = it.getString(KEY_ATTACKER_TYPE, "")
            attackerTeam      = it.getInt(KEY_ATTACKER_TEAM, 0)
            attackerMaxHP     = it.getInt(KEY_ATTACKER_MAX_HP, 10)
            attackerCurrentHP = it.getInt(KEY_ATTACKER_CURRENT_HP, 10)
            targetType        = it.getString(KEY_TARGET_TYPE, "")
            targetTeam        = it.getInt(KEY_TARGET_TEAM, 0)
            targetMaxHP       = it.getInt(KEY_TARGET_MAX_HP, 10)
            targetCurrentHP   = it.getInt(KEY_TARGET_CURRENT_HP, 10)
            damage            = it.getInt(KEY_DAMAGE, 0)
            isRemote          = it.getBoolean(KEY_IS_REMOTE, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_battle, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivUnitBlue   = view.findViewById(R.id.ivUnitBlue)
        ivUnitRed    = view.findViewById(R.id.ivUnitRed)
        pbHealthBlue = view.findViewById(R.id.pbHealthBlue)
        pbHealthRed  = view.findViewById(R.id.pbHealthRed)
        ivFireEffect = view.findViewById(R.id.ivFireEffect)

        ivUnitBlue.scaleX = -1f

        setupInitialState()
        startBattleSequence()
    }


    private fun setupInitialState() {
        val (blueType, redType) = if (attackerTeam == 1)
            Pair(attackerType, targetType)
        else
            Pair(targetType, attackerType)

        ivUnitBlue.setImageResource(getIdleRes(blueType, 1))
        ivUnitRed.setImageResource(getIdleRes(redType, 0))

        if (attackerTeam == 1) {
            pbHealthBlue.max      = attackerMaxHP * 10
            pbHealthBlue.progress = attackerCurrentHP * 10
            pbHealthRed.max       = targetMaxHP * 10
            pbHealthRed.progress  = targetCurrentHP * 10
        } else {
            pbHealthBlue.max      = targetMaxHP * 10
            pbHealthBlue.progress = targetCurrentHP * 10
            pbHealthRed.max       = attackerMaxHP * 10
            pbHealthRed.progress  = attackerCurrentHP * 10
        }
    }

    private fun getIdleRes(type: String, team: Int): Int {
        val teamStr = if (team == 0) "red" else "blue"
        var resId = resources.getIdentifier(
            "${type}idle${teamStr}", "drawable", requireContext().packageName
        )
        if (resId == 0) {
            resId = resources.getIdentifier(
                "$type$teamStr", "drawable", requireContext().packageName
            )
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
        val targetPb  = if (attackerTeam == 1) pbHealthRed else pbHealthBlue

        var frame = 1
        val fireRunnable = object : Runnable {
            override fun run() {
                val resId = resources.getIdentifier(
                    "fire$frame", "drawable", requireContext().packageName
                )
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
        val teamStr     = if (attackerTeam == 0) "red" else "blue"
        val targetImg   = if (attackerTeam == 1) ivUnitRed else ivUnitBlue
        val targetPb    = if (attackerTeam == 1) pbHealthRed else pbHealthBlue
        startAssemblyAnimation(attackerImg, teamStr, targetImg, targetPb)
    }

    private fun startAssemblyAnimation(
        attackerImg: ImageView,
        teamStr: String,
        targetImg: ImageView,
        targetPb: ProgressBar
    ) {
        val originalScaleX = attackerImg.scaleX
        val originalScaleY = attackerImg.scaleY
        attackerImg.scaleX = originalScaleX * 1.5f
        attackerImg.scaleY = originalScaleY * 1.5f

        var assemblyFrame = 1
        val assemblyRunnable = object : Runnable {
            override fun run() {
                var resId = resources.getIdentifier(
                    "assembly${attackerType}$teamStr$assemblyFrame",
                    "drawable", requireContext().packageName
                )
                if (resId == 0) {
                    resId = resources.getIdentifier(
                        "assembly${attackerType}aim$teamStr$assemblyFrame",
                        "drawable", requireContext().packageName
                    )
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
                targetImg.visibility =
                    if (targetImg.visibility == View.VISIBLE) View.INVISIBLE else View.VISIBLE

                if (count < 8) {
                    count++
                    handler.postDelayed(this, 100)
                } else {
                    targetImg.visibility = View.VISIBLE
                    val newProgress = maxOf(0, targetPb.progress - (damage * 10))
                    targetPb.progress = newProgress

                    handler.postDelayed({
                        finishBattle()
                    }, 1000)
                }
            }
        }
        handler.post(flashRunnable)
    }


    private fun finishBattle() {
        if (!isRemote) {
            parentFragmentManager.setFragmentResult("battle_done", Bundle())
        }
        parentFragmentManager.popBackStack()
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
}