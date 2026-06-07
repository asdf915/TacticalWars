package com.example.tacticalwars

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
    private lateinit var ivFireEffect: ImageView
    private lateinit var ivMissile: ImageView

    // Top Bar UI
    private lateinit var ivTopUnitLeft: ImageView
    private lateinit var ivTopUnitRight: ImageView
    private lateinit var tvTopHealthLeft: TextView
    private lateinit var tvTopHealthRight: TextView

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
        ivFireEffect = view.findViewById(R.id.ivFireEffect)
        ivMissile    = view.findViewById(R.id.ivMissile)

        ivTopUnitLeft    = view.findViewById(R.id.ivTopUnitLeft)
        ivTopUnitRight   = view.findViewById(R.id.ivTopUnitRight)
        tvTopHealthLeft  = view.findViewById(R.id.tvTopHealthLeft)
        tvTopHealthRight = view.findViewById(R.id.tvTopHealthRight)

        setupInitialState()

        view.post {
            startBattleSequence()
        }
    }

    private fun setupInitialState() {
        val (blueType, redType) = if (attackerTeam == 1)
            Pair(attackerType, targetType)
        else
            Pair(targetType, attackerType)

        updateUnitSprite(ivUnitBlue, blueType, 1, "idle")
        updateUnitSprite(ivUnitRed, redType, 0, "idle")


        setTopBarSprite(ivTopUnitLeft, blueType, 1)
        setTopBarSprite(ivTopUnitRight, redType, 0)

        if (attackerTeam == 1) {
            tvTopHealthLeft.text  = attackerCurrentHP.toString()
            tvTopHealthRight.text = targetCurrentHP.toString()
        } else {
            tvTopHealthLeft.text  = targetCurrentHP.toString()
            tvTopHealthRight.text = attackerCurrentHP.toString()
        }
    }

    private fun setTopBarSprite(imageView: ImageView, type: String, team: Int) {
        val teamStr = if (team == 0) "red" else "blue"
        val resName = "${type}still${teamStr}1"
        val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
        if (resId != 0) {
            imageView.setImageResource(resId)
            imageView.scaleX = if (team == 1) -1f else 1f
        }
    }

    private fun updateUnitSprite(imageView: ImageView, type: String, team: Int, state: String, frame: Int = -1) {
        val teamStr = if (team == 0) "red" else "blue"

        var names = if (state == "idle") {
            listOf("${type}${teamStr}idle", "${type}${teamStr}", "${type}idle${teamStr}")
        } else {
            listOf("${type}${teamStr}fire$frame", "${type}fire${teamStr}$frame", "assembly${type}${teamStr}$frame")
        }

        if (type == "tank") {
            if (state == "idle") {
                names = listOf("${type}fire${teamStr}1") + names.filter { it != "${type}${teamStr}" }
            } else {
                names = names.filter { it != "${type}${teamStr}" }
            }
        }

        var resId = 0
        for (name in names) {
            resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
            if (resId != 0) break
        }

        if (resId != 0) {
            imageView.setImageResource(resId)

            val baseScale = when (type) {
                "infantry" -> 2.0f
                "bazooka"  -> 2.5f
                "tank"     -> 2.0f
                else       -> 1.0f
            }

            imageView.scaleX = baseScale
            imageView.scaleY = baseScale
        }
    }

    private fun startBattleSequence() {
        if (attackerType == "jet") {
            animateJetBattle()
        } else {
            animateUnitFiringSequence()
        }
    }

    private fun animateJetBattle() {
        val attackerImg = if (attackerTeam == 1) ivUnitBlue else ivUnitRed
        val targetImg   = if (attackerTeam == 1) ivUnitRed else ivUnitBlue
        val targetTv    = if (attackerTeam == 1) tvTopHealthRight else tvTopHealthLeft

        val density = resources.displayMetrics.density
        val missileW = if (ivMissile.width > 0) ivMissile.width.toFloat() else 40f * density
        val missileH = if (ivMissile.height > 0) ivMissile.height.toFloat() else 40f * density

        val attackerCenterX = attackerImg.x + (attackerImg.width / 2f)
        val attackerCenterY = attackerImg.y + (attackerImg.height / 2f)

        val attackerVisualWidth = attackerImg.width.toFloat()

        val targetCenterX = targetImg.x + (targetImg.width / 2f)
        val targetVisualWidth = targetImg.width.toFloat()

        val startX: Float
        val targetX: Float

        if (attackerTeam == 1) {
            startX = attackerCenterX + (attackerVisualWidth / 2f)
            targetX = targetCenterX - (targetVisualWidth / 2f) - missileW
            ivMissile.scaleX = 1f
        } else {
            startX = attackerCenterX - (attackerVisualWidth / 2f) - missileW
            targetX = targetCenterX + (targetVisualWidth / 2f)
            ivMissile.scaleX = -1f
        }

        val finalTargetX = if (attackerTeam == 1) {
            maxOf(startX + 10f, targetX)
        } else {
            minOf(startX - 10f, targetX)
        }

        ivMissile.visibility = View.VISIBLE
        ivMissile.y = attackerCenterY - (missileH / 2f)

        val missileAnimation = ObjectAnimator.ofFloat(ivMissile, "x", startX, finalTargetX)
        missileAnimation.duration = 800

        missileAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                ivMissile.visibility = View.GONE
                showExplosionEffect(targetImg, targetTv)
            }
        })

        missileAnimation.start()
        playGunshotSound()
    }

    private fun showExplosionEffect(targetImg: ImageView, targetTv: TextView) {
        var frame = 1
        val fireRunnable = object : Runnable {
            override fun run() {
                val resId = resources.getIdentifier(
                    "fire$frame", "drawable", requireContext().packageName
                )

                if (resId != 0) {
                    ivFireEffect.setImageResource(resId)
                    ivFireEffect.visibility = View.VISIBLE

                    val density = resources.displayMetrics.density
                    val targetCenterX = targetImg.x + (targetImg.width / 2f)
                    val targetCenterY = targetImg.y + (targetImg.height / 2f)
                    val effectW = if (ivFireEffect.width > 0) ivFireEffect.width.toFloat() else 80f * density
                    val effectH = if (ivFireEffect.height > 0) ivFireEffect.height.toFloat() else 80f * density

                    ivFireEffect.x = targetCenterX - (effectW / 2f)
                    ivFireEffect.y = targetCenterY - (effectH / 2f)
                }

                if (frame == 1) {
                    frame = 2
                    handler.postDelayed(this, 300)
                } else {
                    handler.postDelayed({
                        ivFireEffect.visibility = View.GONE
                        applyDamageAnimation(targetImg, targetTv)
                    }, 300)
                }
            }
        }
        handler.post(fireRunnable)
    }

    private fun animateUnitFiringSequence() {
        val attackerImg = if (attackerTeam == 1) ivUnitBlue else ivUnitRed
        val targetImg   = if (attackerTeam == 1) ivUnitRed else ivUnitBlue
        val targetTv    = if (attackerTeam == 1) tvTopHealthRight else tvTopHealthLeft
        startFiringAnimation(attackerImg, targetImg, targetTv)
    }

    private fun startFiringAnimation(
        attackerImg: ImageView,
        targetImg: ImageView,
        targetTv: TextView
    ) {
        var fireFrame = 1
        val firingRunnable = object : Runnable {
            override fun run() {
                val teamStr = if (attackerTeam == 0) "red" else "blue"
                val possibleNames = listOf(
                    "${attackerType}${teamStr}fire$fireFrame",
                    "${attackerType}fire${teamStr}$fireFrame",
                    "assembly${attackerType}${teamStr}$fireFrame"
                )

                var resId = 0
                for (name in possibleNames) {
                    if (resources.getIdentifier(name, "drawable", requireContext().packageName) != 0) {
                        resId = 1
                        break
                    }
                }

                if (resId != 0) {
                    if (fireFrame == 1) playGunshotSound()
                    updateUnitSprite(attackerImg, attackerType, attackerTeam, "fire", fireFrame)
                    fireFrame++
                    handler.postDelayed(this, 500)
                } else {
                    updateUnitSprite(attackerImg, attackerType, attackerTeam, "idle")
                    applyDamageAnimation(targetImg, targetTv)
                }
            }
        }
        handler.post(firingRunnable)
    }

    private fun applyDamageAnimation(targetImg: ImageView, targetTv: TextView) {
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

                    // Solo actualizamos la salud en el Top Bar
                    val currentHP = if (targetTeam == attackerTeam) attackerCurrentHP else targetCurrentHP
                    val newHP = maxOf(0, currentHP - damage)
                    targetTv.text = newHP.toString()

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