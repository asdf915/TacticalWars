package com.example.tacticalwars

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import java.util.LinkedList
import java.util.Queue

class GameFragment : Fragment() {


    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")
    private var gameId: String = ""
    private var firestoreListener: ListenerRegistration? = null


    private var selectedMapId: Int = 0
    private var isAiMode: Boolean = false
    private var difficulty: String = "Normal"


    private val rows = 8
    private val cols = 6
    private val boardCells = Array(rows) { arrayOfNulls<FrameLayout>(cols) }
    private val units = mutableListOf<GameUnit>()
    private var selectedUnit: GameUnit? = null
    private val reachableTiles = mutableSetOf<Pair<Int, Int>>()
    private val attackableTiles = mutableSetOf<Pair<Int, Int>>()
    private val abilityTiles = mutableSetOf<Pair<Int, Int>>()

    private var currentTeam: Int = 1
    private var isWaitingForAction = false
    private var isWaitingForSecondary = false
    private var isInteractionLocked = false
    private var isBattleInProgress = false


    private lateinit var tvTurnTitle: TextView
    private lateinit var tvHealthInfo: TextView
    private lateinit var tvUnitDetail: TextView
    private lateinit var llActions: LinearLayout
    private lateinit var btnAttack: Button
    private lateinit var btnSecondary: Button
    private lateinit var btnWait: Button


    private var currentFrame = 1
    private val handler = Handler(Looper.getMainLooper())
    private val animationRunnable = object : Runnable {
        override fun run() {
            currentFrame = if (currentFrame >= 3) 1 else currentFrame + 1
            updateUnitsAnimations()
            handler.postDelayed(this, 500)
        }
    }


    enum class UnitState { STILL, UP, DOWN, LEFT, RIGHT }

    enum class UnitType(
        val displayName: String,
        val moveRange: Int,
        val attackRange: Int,
        val maxHP: Int,
        val abilityName: String,
        val prefix: String
    ) {
        INFANTRY("Infantería", 3, 1, 10, "Potenciar",  "infantry"),
        BAZOOKA ("Bazuca",     3, 2, 12, "Reactivar",  "bazooka"),
        TANK    ("Tanque",     2, 1, 20, "Empujar",    "tank"),
        PLANE   ("Avión",      4, 1,  8, "Curar",      "jet")
    }

    data class GameUnit(
        val type: UnitType,
        val team: Int,
        var r: Int,
        var c: Int,
        var health: Int,
        var maxHealth: Int,
        var hasActed: Boolean = false,
        var hasMoved: Boolean = false,
        var attackPower: Int = 3,
        var view: ImageView? = null,
        var state: UnitState = UnitState.STILL
    )


    companion object {
        private const val ARG_MAP_ID    = "map_id"
        private const val ARG_AI_MODE   = "ai_mode"
        private const val ARG_DIFFICULTY = "difficulty"
        private const val ARG_GAME_ID   = "game_id"

        private val MAP1_TILES = listOf(1,1,1,1,1,1, 1,1,1,1,2,1, 1,1,2,1,1,1, 1,2,1,1,2,2, 1,2,1,1,1,1, 1,1,1,1,1,1, 1,1,1,1,2,1, 2,1,2,1,1,1)
        private val MAP2_TILES = listOf(1,1,1,1,4,4, 1,1,2,4,4,4, 4,5,4,4,1,1, 1,1,1,6,1,1, 1,1,1,4,4,2, 1,1,1,1,4,1, 1,1,1,1,6,1, 2,1,2,1,4,1)
        private val MAP3_TILES = listOf(1,3,1,1,1,2, 1,1,1,1,1,3, 4,4,1,1,1,1, 1,4,4,5,4,4, 1,1,1,1,2,1, 1,2,1,1,1,1, 1,3,1,1,1,1, 1,1,1,1,1,2)
        private val MAP4_TILES = listOf(1,1,1,1,2,4, 4,5,4,5,4,4, 4,5,4,5,4,4, 2,1,1,1,2,2, 1,1,2,1,1,1, 4,5,4,5,4,4, 4,5,4,5,4,4, 4,1,1,1,1,2)
        private val MAP5_TILES = listOf(3,1,1,1,2,2, 3,3,4,1,4,1, 1,1,1,1,1,1, 5,4,4,5,5,4, 1,2,6,1,1,2, 3,2,4,1,2,2, 1,1,1,1,1,1, 2,1,2,2,1,1)

        fun newInstance(
            mapId: Int,
            aiMode: Boolean = false,
            difficulty: String = "Normal",
            gameId: String = ""
        ): GameFragment {
            val fragment = GameFragment()
            val args = Bundle()
            args.putInt(ARG_MAP_ID, mapId)
            args.putBoolean(ARG_AI_MODE, aiMode)
            args.putString(ARG_DIFFICULTY, difficulty)
            args.putString(ARG_GAME_ID, gameId)
            fragment.arguments = args
            return fragment
        }

        fun getTileRes(mapId: Int, index: Int): Int = when (getTileNum(mapId, index)) {
            1 -> R.drawable.tile1
            2 -> R.drawable.tile2
            3 -> R.drawable.tile3
            4 -> R.drawable.tile4
            5 -> R.drawable.tile5
            6 -> R.drawable.tile6
            else -> R.drawable.tile1
        }

        fun getTileNum(mapId: Int, index: Int): Int {
            val list = when (mapId) {
                1 -> MAP1_TILES; 2 -> MAP2_TILES; 3 -> MAP3_TILES
                4 -> MAP4_TILES; 5 -> MAP5_TILES; else -> return 1
            }
            return if (index < list.size) list[index] else 1
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedMapId = it.getInt(ARG_MAP_ID)
            isAiMode      = it.getBoolean(ARG_AI_MODE)
            difficulty     = it.getString(ARG_DIFFICULTY, "Normal")
            gameId         = it.getString(ARG_GAME_ID, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTurnTitle  = view.findViewById(R.id.tvTurnTitle)
        tvHealthInfo = view.findViewById(R.id.tvHealthInfo)
        tvUnitDetail = view.findViewById(R.id.tvUnitDetail)
        llActions    = view.findViewById(R.id.llActions)
        btnAttack    = view.findViewById(R.id.btnAttack)
        btnSecondary = view.findViewById(R.id.btnSecondary)
        btnWait      = view.findViewById(R.id.btnWait)

        val ivBackground = view.findViewById<ImageView>(R.id.ivGameBackground)
        if (selectedMapId in 1..5) ivBackground.visibility = View.GONE
        else ivBackground.setImageResource(R.drawable.background4)

        val glBoard = view.findViewById<GridLayout>(R.id.glBoard)
        setupBoard(glBoard)
        updateTurnUI()

        btnWait.setOnClickListener {
            selectedUnit?.let { unit ->
                grayOutUnit(unit)
                finishUnitTurn()
            }
        }
        btnAttack.setOnClickListener    { selectedUnit?.let { showAttackRange(it) } }
        btnSecondary.setOnClickListener { selectedUnit?.let { showSecondaryRange(it) } }

        parentFragmentManager.setFragmentResultListener("battle_done", viewLifecycleOwner) { _, _ ->
            isBattleInProgress = false
            checkUnitDeaths()
            finishUnitTurn()

            syncGameStateToFirestore()
            if (isAiMode && currentTeam == 0) {
                handler.postDelayed({ executeAiTurn() }, 500)
            }
        }

        initFirestore()
        attachFirestoreListener()

        handler.post(animationRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(animationRunnable)
        firestoreListener?.remove()
    }


    private fun initFirestore() {
        if (gameId.isBlank()) {

            gameId = db.collection("games").document().id
        }

        val initialData = mapOf(
            "currentTeam"      to currentTeam,
            "difficulty"       to difficulty,
            "isAiMode"         to isAiMode,
            "selectedMapId"    to selectedMapId,
            "isBattleInProgress" to isBattleInProgress
        )

        db.collection("games").document(gameId)
            .set(initialData)
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Error al inicializar partida: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }


    private fun syncGameStateToFirestore() {
        if (gameId.isBlank()) return

        val update = mapOf(
            "currentTeam"        to currentTeam,
            "difficulty"         to difficulty,
            "isAiMode"           to isAiMode,
            "selectedMapId"      to selectedMapId,
            "isBattleInProgress" to isBattleInProgress
        )

        db.collection("games").document(gameId)
            .update(update)
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Error al sincronizar: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }


    private fun attachFirestoreListener() {
        if (gameId.isBlank()) return

        firestoreListener = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener


                val remoteDifficulty = snapshot.getString("difficulty")
                if (remoteDifficulty != null && remoteDifficulty != difficulty) {
                    difficulty = remoteDifficulty
                    Toast.makeText(requireContext(),
                        "Dificultad actualizada: $difficulty",
                        Toast.LENGTH_SHORT).show()
                }


                val remoteBattle = snapshot.getBoolean("isBattleInProgress") ?: false
                if (remoteBattle != isBattleInProgress && !isBattleInProgress) {
                    isBattleInProgress = remoteBattle
                }


                val remoteTeam = (snapshot.getLong("currentTeam") ?: currentTeam.toLong()).toInt()
                if (remoteTeam != currentTeam && !isBattleInProgress) {
                    currentTeam = remoteTeam
                    updateTurnUI()
                    if (isAiMode && currentTeam == 0) {
                        handler.postDelayed({ executeAiTurn() }, 1000)
                    }
                }
            }
    }





    private fun executeAiTurn() {
        if (isBattleInProgress || units.none { it.team == 1 }) return

        val aiUnits = units.filter { it.team == 0 && !it.hasActed }
        if (aiUnits.isEmpty()) {
            checkTurnEnd()
            updateTurnUI()
            return
        }

        val unit      = aiUnits.first()
        val unitIndex = units.indexOf(unit)


        val unitsPayload = units.map { u ->
            mapOf(
                "type"        to u.type.name,
                "team"        to u.team,
                "r"           to u.r,
                "c"           to u.c,
                "health"      to u.health,
                "maxHealth"   to u.maxHealth,
                "hasActed"    to u.hasActed,
                "hasMoved"    to u.hasMoved,
                "attackPower" to u.attackPower
            )
        }

        val payload = hashMapOf(
            "gameId"      to gameId,
            "units"       to unitsPayload,
            "aiUnitIndex" to unitIndex
        )

        functions
            .getHttpsCallable("calculateAiMove")
            .call(payload)
            .addOnSuccessListener { result ->
                @Suppress("UNCHECKED_CAST")
                val data = result.data as? Map<String, Any> ?: return@addOnSuccessListener
                handleAiMoveResult(unit, data)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Error calculando movimiento IA: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                unit.hasActed = true
                grayOutUnit(unit)
                handler.postDelayed({ executeAiTurn() }, 800)
            }
    }


    @Suppress("UNCHECKED_CAST")
    private fun handleAiMoveResult(unit: GameUnit, data: Map<String, Any>) {
        // Parse path returned by the Cloud Function
        val rawPath = data["path"] as? List<Map<String, Any>> ?: emptyList()
        val path = rawPath.map { step ->
            Pair(
                (step["r"] as? Long)?.toInt() ?: unit.r,
                (step["c"] as? Long)?.toInt() ?: unit.c
            )
        }


        val rawAttack  = data["attackTarget"] as? Map<String, Any>
        val attackPos  = rawAttack?.let {
            Pair(
                (it["r"] as? Long)?.toInt() ?: -1,
                (it["c"] as? Long)?.toInt() ?: -1
            )
        }
        val damage = (data["damage"] as? Long)?.toInt() ?: 0


        animatePath(unit, path, 1) {
            if (attackPos != null && damage > 0) {
                val target = units.find { it.team == 1 && it.r == attackPos.first && it.c == attackPos.second }
                target?.let { startBattle(unit, it, damage) }
            }

            if (!isBattleInProgress) {
                unit.hasActed = true
                grayOutUnit(unit)
                syncGameStateToFirestore()
                handler.postDelayed({ executeAiTurn() }, 800)
            }
        }
    }



    private fun updateUnitsAnimations() {
        for (unit in units) {
            val teamStr = if (unit.team == 0) "red" else "blue"
            val statePrefix = when {
                unit.hasActed          -> "unavailable"
                unit.state == UnitState.UP    -> "up"
                unit.state == UnitState.DOWN  -> "down"
                unit.state == UnitState.LEFT || unit.state == UnitState.RIGHT -> "move"
                else -> "still"
            }
            val resName = "${unit.type.prefix}$statePrefix$teamStr$currentFrame"
            val resId = resources.getIdentifier(resName, "drawable", requireContext().packageName)
            if (resId != 0) unit.view?.setImageResource(resId)

            val scale = if (statePrefix in listOf("up","down","move")) 1.4f else 1.0f
            unit.view?.scaleY = scale
            if (unit.state == UnitState.RIGHT) {
                unit.view?.scaleX = -scale
            } else if (unit.state == UnitState.LEFT) {
                unit.view?.scaleX = scale
            } else {
                val cx = unit.view?.scaleX ?: 1f
                unit.view?.scaleX = if (cx < 0) -scale else scale
            }
        }
    }

    private fun getTileTypeAt(r: Int, c: Int) =
        getTileNum(selectedMapId, r * cols + c)

    private fun updateTurnUI() {
        val teamName  = if (currentTeam == 0) (if (isAiMode) "ROJO (IA)" else "ROJO") else "AZUL"
        val teamColor = if (currentTeam == 0) Color.RED else Color.CYAN
        tvTurnTitle.text = "TURNO JUGADOR $teamName"
        tvTurnTitle.setTextColor(teamColor)
        val healthText = units.filter { it.team == currentTeam }
            .joinToString(" | ") { "${it.type.displayName}: ${it.health}" }
        tvHealthInfo.text = healthText
    }

    private fun setupBoard(gridLayout: GridLayout) {
        gridLayout.removeAllViews()
        gridLayout.rowCount  = rows
        gridLayout.columnCount = cols
        gridLayout.clipChildren   = false
        gridLayout.clipToPadding  = false

        gridLayout.post {
            val cellSize = Math.min(gridLayout.width / cols, gridLayout.height / rows)
            if (cellSize <= 0) return@post

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val frame = FrameLayout(requireContext())
                    frame.clipChildren = false
                    val params = GridLayout.LayoutParams().apply {
                        rowSpec    = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                        width  = cellSize; height = cellSize
                    }
                    frame.layoutParams = params

                    if (selectedMapId in 1..5) {
                        val tileView = ImageView(requireContext()).apply {
                            tag = "tile"
                            layoutParams = FrameLayout.LayoutParams(cellSize, cellSize)
                            setImageResource(getTileRes(selectedMapId, r * cols + c))
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        frame.addView(tileView)
                    }

                    frame.setBackgroundResource(R.drawable.cell_border)
                    boardCells[r][c] = frame
                    gridLayout.addView(frame)

                    frame.setOnClickListener {
                        if (!(isAiMode && currentTeam == 0)) onCellClicked(r, c)
                    }
                }
            }

            if (units.isEmpty()) {
                for (c in 1..4) {
                    createUnit(0, c, 0, c - 1)
                    createUnit(7, c, 1, c - 1)
                }
            } else {
                for (unit in units) {
                    val frame = boardCells[unit.r][unit.c]
                    unit.view?.let {
                        (it.parent as? ViewGroup)?.removeView(it)
                        frame?.addView(it)
                    }
                }
            }
        }
    }

    private fun createUnit(r: Int, c: Int, team: Int, typeIndex: Int) {
        val type = UnitType.values()[typeIndex % 4]
        val unit = GameUnit(type, team, r, c, type.maxHP, type.maxHP)
        unit.view = addUnitToUI(boardCells[r][c]!!, team, type)
        units.add(unit)
    }

    private fun addUnitToUI(container: FrameLayout, team: Int, type: UnitType): ImageView {
        val size   = (resources.displayMetrics.density * 56).toInt()
        val params = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
        val teamStr = if (team == 0) "red" else "blue"
        val resId  = resources.getIdentifier("${type.prefix}still${teamStr}1", "drawable", requireContext().packageName)
        return ImageView(requireContext()).apply {
            layoutParams = params
            if (resId != 0) setImageResource(resId)
            setPadding(4, 4, 4, 4)
            isClickable = false
            container.addView(this)
        }
    }

    private fun onCellClicked(r: Int, c: Int) {
        if (isInteractionLocked || isBattleInProgress) return

        if (isWaitingForSecondary) {
            if (Pair(r, c) in abilityTiles) {
                val target = units.find { it.r == r && it.c == c }
                if (target != null) {
                    executeSecondaryAbility(selectedUnit!!, target)
                    selectedUnit?.let { u -> u.hasActed = true; grayOutUnit(u) }
                    finishUnitTurn()
                    return
                }
            }
            clearSecondaryMode(); return
        }

        if (isWaitingForAction) {
            if (Pair(r, c) in attackableTiles) {
                val target = units.find { it.r == r && it.c == c && it.team != currentTeam }
                target?.let {
                    var damage = selectedUnit?.attackPower ?: 3
                    if (getTileTypeAt(it.r, it.c) == 2) damage = (damage * 0.5).toInt().coerceAtLeast(1)
                    startBattle(selectedUnit!!, it, damage); return
                }
            } else {
                if (selectedUnit?.hasMoved == true) {
                    clearHighlights(); attackableTiles.clear()
                    isWaitingForAction = true; llActions.visibility = View.VISIBLE; return
                }
                clearSelection()
            }
            return
        }

        val unitAtCell = units.find { it.r == r && it.c == c }
        if (selectedUnit != null) {
            when {
                Pair(r, c) in reachableTiles && !selectedUnit!!.hasMoved -> moveUnit(selectedUnit!!, r, c)
                r == selectedUnit!!.r && c == selectedUnit!!.c           -> showActionUI()
                else -> {
                    clearSelection()
                    if (unitAtCell != null && unitAtCell.team == currentTeam && !unitAtCell.hasActed)
                        selectUnit(unitAtCell)
                }
            }
        } else if (unitAtCell != null && unitAtCell.team == currentTeam && !unitAtCell.hasActed) {
            selectUnit(unitAtCell)
        }
    }

    private fun startBattle(attacker: GameUnit, target: GameUnit, damage: Int) {
        isBattleInProgress = true
        syncGameStateToFirestore()          // ← persist battle start

        val battleFragment = BattleFragment.newInstance(
            attacker.type.prefix, attacker.team, attacker.type.maxHP, attacker.health,
            target.type.prefix,   target.team,   target.type.maxHP,   target.health,
            damage
        )
        target.health -= damage
        attacker.hasActed = true
        grayOutUnit(attacker)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, battleFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun checkUnitDeaths() {
        val iterator = units.iterator()
        while (iterator.hasNext()) {
            val unit = iterator.next()
            if (unit.health <= 0) {
                (unit.view?.parent as? ViewGroup)?.removeView(unit.view)
                iterator.remove()
            }
        }
        checkVictory()
    }

    private fun executeSecondaryAbility(user: GameUnit, target: GameUnit) {
        when (user.type) {
            UnitType.INFANTRY -> {
                target.attackPower += 2
                Toast.makeText(requireContext(), "${target.type.displayName} potenciado!", Toast.LENGTH_SHORT).show()
            }
            UnitType.BAZOOKA -> {
                target.hasActed = false; target.hasMoved = false
                resetUnitAppearance(target)
                Toast.makeText(requireContext(), "${target.type.displayName} reactivado!", Toast.LENGTH_SHORT).show()
            }
            UnitType.TANK -> {
                val dr = target.r - user.r; val dc = target.c - user.c
                val nr = target.r + dr;     val nc = target.c + dc
                if (nr in 0 until rows && nc in 0 until cols && units.none { it.r == nr && it.c == nc }) {
                    moveUnitDirectly(target, nr, nc)
                    Toast.makeText(requireContext(), "${target.type.displayName} empujado!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "No se puede empujar ahí", Toast.LENGTH_SHORT).show()
                }
            }
            UnitType.PLANE -> {
                target.health = Math.min(target.maxHealth, target.health + 5)
                Toast.makeText(requireContext(), "${target.type.displayName} curado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun moveUnitDirectly(unit: GameUnit, newR: Int, newC: Int) {
        val oldFrame = boardCells[unit.r][unit.c]
        val newFrame = boardCells[newR][newC]
        val unitView = unit.view ?: return
        val cellSize = oldFrame?.width?.toFloat() ?: 0f

        oldFrame?.removeView(unitView)
        newFrame?.addView(unitView)
        newFrame?.elevation = 100f

        unitView.translationX = (unit.c - newC) * cellSize
        unitView.translationY = (unit.r - newR) * cellSize
        unitView.animate().translationX(0f).translationY(0f).setDuration(400)
            .withEndAction { newFrame?.elevation = 0f }.start()

        unit.r = newR; unit.c = newC
    }

    private fun selectUnit(unit: GameUnit) {
        selectedUnit = unit
        tvUnitDetail.text = "UNIDAD: ${unit.type.displayName} | SALUD: ${unit.health}/${unit.maxHealth}"
        btnSecondary.text = unit.type.abilityName
        if (unit.hasMoved) showActionUI() else showMovementRange(unit)
    }

    private fun clearSelection() {
        selectedUnit = null
        reachableTiles.clear(); attackableTiles.clear(); abilityTiles.clear()
        isWaitingForAction = false; isWaitingForSecondary = false
        llActions.visibility = View.INVISIBLE
        tvUnitDetail.text = "SELECCIONA UNA UNIDAD"
        clearHighlights()
    }

    private fun clearSecondaryMode() {
        isWaitingForSecondary = false; abilityTiles.clear()
        clearHighlights(); llActions.visibility = View.VISIBLE
    }

    private fun showMovementRange(unit: GameUnit) {
        clearHighlights(); reachableTiles.clear()
        val queue: Queue<Triple<Int, Int, Int>> = LinkedList()
        queue.add(Triple(unit.r, unit.c, 0))
        val visited = mutableMapOf(Pair(unit.r, unit.c) to 0)

        while (queue.isNotEmpty()) {
            val (currR, currC, dist) = queue.poll()!!
            if (dist > 0) { reachableTiles.add(Pair(currR, currC)); highlightCell(currR, currC, Color.argb(128, 0, 100, 255)) }
            if (dist < unit.type.moveRange) {
                for (n in arrayOf(Pair(currR-1,currC),Pair(currR+1,currC),Pair(currR,currC-1),Pair(currR,currC+1))) {
                    if (n.first in 0 until rows && n.second in 0 until cols) {
                        val tt = getTileTypeAt(n.first, n.second)
                        val ok = when (tt) { 3->false; 4->unit.type==UnitType.PLANE; else->true }
                        if (ok && units.none { it.r==n.first && it.c==n.second } && visited[n]==null) {
                            visited[n] = dist+1; queue.add(Triple(n.first,n.second,dist+1))
                        }
                    }
                }
            }
        }
    }

    private fun showAttackRange(unit: GameUnit) {
        clearHighlights(); attackableTiles.clear()
        isWaitingForAction = true; llActions.visibility = View.INVISIBLE
        val range = unit.type.attackRange
        for (dr in -range..range) for (dc in -range..range) {
            val dist = Math.abs(dr) + Math.abs(dc)
            if (dist in 1..range) {
                val nr = unit.r + dr; val nc = unit.c + dc
                if (nr in 0 until rows && nc in 0 until cols) {
                    attackableTiles.add(Pair(nr, nc)); highlightCell(nr, nc, Color.argb(128, 255, 0, 0))
                }
            }
        }
    }

    private fun showSecondaryRange(unit: GameUnit) {
        clearHighlights(); abilityTiles.clear()
        isWaitingForSecondary = true; llActions.visibility = View.INVISIBLE
        for (n in arrayOf(Pair(unit.r-1,unit.c),Pair(unit.r+1,unit.c),Pair(unit.r,unit.c-1),Pair(unit.r,unit.c+1))) {
            if (n.first in 0 until rows && n.second in 0 until cols) {
                abilityTiles.add(n); highlightCell(n.first, n.second, Color.argb(128, 0, 255, 0))
            }
        }
    }

    private fun highlightCell(r: Int, c: Int, color: Int) {
        val frame = boardCells[r][c] ?: return
        val highlight = View(requireContext()).apply { tag = "highlight"; setBackgroundColor(color) }
        val insertIndex = if (frame.childCount > 0 && frame.getChildAt(0).tag == "tile") 1 else 0
        frame.addView(highlight, insertIndex)
    }

    private fun clearHighlights() {
        for (r in 0 until rows) for (c in 0 until cols) {
            val frame = boardCells[r][c] ?: continue
            frame.findViewWithTag<View>("highlight")?.let { frame.removeView(it) }
        }
    }

    private fun moveUnit(unit: GameUnit, newR: Int, newC: Int) {
        val path = findPath(unit, Pair(unit.r, unit.c), Pair(newR, newC))
        if (path.size <= 1) { unit.hasMoved = true; showActionUI(); return }
        clearHighlights(); reachableTiles.clear(); isInteractionLocked = true
        animatePath(unit, path, 1) { unit.hasMoved = true; isInteractionLocked = false; showActionUI() }
    }

    private fun findPath(unit: GameUnit, start: Pair<Int,Int>, target: Pair<Int,Int>): List<Pair<Int,Int>> {
        val queue: Queue<List<Pair<Int,Int>>> = LinkedList()
        queue.add(listOf(start)); val visited = mutableSetOf(start)
        while (queue.isNotEmpty()) {
            val path = queue.poll()!!; val current = path.last()
            if (current == target) return path
            for (n in listOf(Pair(current.first-1,current.second),Pair(current.first+1,current.second),Pair(current.first,current.second-1),Pair(current.first,current.second+1))) {
                if (n.first in 0 until rows && n.second in 0 until cols && n !in visited) {
                    val tt = getTileTypeAt(n.first, n.second)
                    val ok = when(tt) { 3->false; 4->unit.type==UnitType.PLANE; else->true }
                    if (ok && units.none { it.r==n.first && it.c==n.second && it!=unit }) {
                        visited.add(n); queue.add(path + n)
                    }
                }
            }
        }
        return listOf(start)
    }

    private fun animatePath(unit: GameUnit, path: List<Pair<Int,Int>>, index: Int, onComplete: () -> Unit) {
        if (index >= path.size) { unit.state = UnitState.STILL; updateUnitsAnimations(); onComplete(); return }
        val prev = path[index - 1]; val curr = path[index]
        unit.state = when {
            curr.first  < prev.first  -> UnitState.UP
            curr.first  > prev.first  -> UnitState.DOWN
            curr.second < prev.second -> UnitState.LEFT
            curr.second > prev.second -> UnitState.RIGHT
            else -> UnitState.STILL
        }
        updateUnitsAnimations()
        moveUnitDirectly(unit, curr.first, curr.second)
        handler.postDelayed({ animatePath(unit, path, index + 1, onComplete) }, 500)
    }

    private fun showActionUI() {
        llActions.visibility = View.VISIBLE
        isWaitingForAction = false; isWaitingForSecondary = false
    }

    private fun finishUnitTurn() {
        clearSelection()
        checkTurnEnd()
        updateTurnUI()
        syncGameStateToFirestore()          // ← persist after every turn
    }

    private fun checkTurnEnd() {
        if (isBattleInProgress) return
        val currentTeamUnits = units.filter { it.team == currentTeam }
        if (currentTeamUnits.isNotEmpty() && currentTeamUnits.all { it.hasActed }) {
            currentTeam = 1 - currentTeam
            units.filter { it.team == currentTeam }.forEach {
                it.hasActed = false; it.hasMoved = false; resetUnitAppearance(it)
            }
            syncGameStateToFirestore()
            if (isAiMode && currentTeam == 0) {
                handler.postDelayed({ executeAiTurn() }, 1000)
            }
        }
    }

    private fun checkVictory() {
        val redUnits  = units.filter { it.team == 0 }
        val blueUnits = units.filter { it.team == 1 }
        if (redUnits.isEmpty())  navigateToVictory(1)
        else if (blueUnits.isEmpty()) navigateToVictory(0)
    }

    private fun navigateToVictory(winnerTeam: Int) {
        handler.removeCallbacks(animationRunnable)
        firestoreListener?.remove()

        if (gameId.isNotBlank()) {
            db.collection("games").document(gameId)
                .update("winner", winnerTeam, "finished", true)
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, VictoryFragment.newInstance(winnerTeam))
            .commit()
    }

    private fun grayOutUnit(unit: GameUnit) { unit.hasActed = true; unit.view?.colorFilter = null }
    private fun resetUnitAppearance(unit: GameUnit) { unit.view?.colorFilter = null }
}