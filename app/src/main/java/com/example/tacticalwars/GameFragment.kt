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
import java.util.LinkedList
import java.util.Queue

class GameFragment : Fragment() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var gameId: String = ""
    private var firestoreListener: ListenerRegistration? = null
    private var localTeam: Int = 1
    private var isMultiplayer: Boolean = false

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
        val baseAttack: Int,
        val abilityName: String,
        val prefix: String
    ) {
        INFANTRY("Infantería", 3, 1, 16, 4, "Potenciar", "infantry"),
        BAZOOKA ("Bazuca",     2, 2, 20, 6, "Reactivar", "bazooka"),
        TANK    ("Tanque",     2, 1, 36, 5, "Empujar",   "tank"),
        PLANE   ("Avión",      4, 1, 12, 3, "Curar",     "jet")
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
        var attackPower: Int = type.baseAttack,
        var view: ImageView? = null,
        var state: UnitState = UnitState.STILL
    )

    companion object {
        private const val ARG_MAP_ID      = "map_id"
        private const val ARG_AI_MODE     = "ai_mode"
        private const val ARG_DIFFICULTY  = "difficulty"
        private const val ARG_GAME_ID     = "game_id"
        private const val ARG_MULTIPLAYER = "multiplayer"
        private const val ARG_LOCAL_TEAM  = "local_team"

        private val MAP_TILES = mapOf(
            1 to listOf(
                1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2, 1,
                1, 1, 2, 1, 1, 1,
                1, 2, 1, 1, 2, 2,
                1, 2, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 1,
                1, 1, 1, 1, 2, 1,
                2, 1, 2, 1, 1, 1
            ),
            2 to listOf(
                1, 1, 1, 1, 4, 4,
                1, 1, 2, 4, 4, 4,
                4, 5, 4, 4, 1, 1,
                1, 1, 1, 6, 1, 1,
                1, 1, 1, 4, 4, 2,
                1, 1, 1, 1, 4, 1,
                1, 1, 1, 1, 6, 1,
                2, 1, 2, 1, 4, 1
            ),
            3 to listOf(
                1, 3, 1, 1, 1, 2,
                1, 1, 1, 1, 1, 3,
                4, 4, 1, 1, 1, 1,
                1, 4, 4, 5, 4, 4,
                1, 1, 1, 1, 2, 1,
                1, 2, 1, 1, 1, 1,
                1, 3, 1, 1, 1, 1,
                1, 1, 1, 1, 1, 2
            ),
            4 to listOf(
                1, 1, 1, 1, 2, 4,
                4, 5, 4, 5, 4, 4,
                4, 5, 4, 5, 4, 4,
                2, 1, 1, 1, 2, 2,
                1, 1, 2, 1, 1, 1,
                4, 5, 4, 5, 4, 4,
                4, 5, 4, 5, 4, 4,
                4, 1, 1, 1, 1, 2
            ),
            5 to listOf(
                3, 1, 1, 1, 2, 2,
                3, 3, 4, 1, 4, 1,
                1, 1, 1, 1, 1, 1,
                5, 4, 4, 5, 5, 4,
                1, 2, 6, 1, 1, 2,
                3, 2, 4, 1, 2, 2,
                1, 1, 1, 1, 1, 1,
                2, 1, 2, 2, 1, 1
            )
        )

        fun newInstance(
            mapId: Int,
            aiMode: Boolean = false,
            difficulty: String = "Normal",
            gameId: String = "",
            multiplayer: Boolean = false,
            localTeam: Int = 1
        ): GameFragment = GameFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_MAP_ID, mapId)
                putBoolean(ARG_AI_MODE, aiMode)
                putString(ARG_DIFFICULTY, difficulty)
                putString(ARG_GAME_ID, gameId)
                putBoolean(ARG_MULTIPLAYER, multiplayer)
                putInt(ARG_LOCAL_TEAM, localTeam)
            }
        }

        fun getTileRes(mapId: Int, index: Int): Int = when (getTileNum(mapId, index)) {
            2    -> R.drawable.tile2
            3    -> R.drawable.tile3
            4    -> R.drawable.tile4
            5    -> R.drawable.tile5
            6    -> R.drawable.tile6
            else -> R.drawable.tile1
        }

        fun getTileNum(mapId: Int, index: Int): Int {
            val tiles = MAP_TILES[mapId] ?: return 1
            return tiles.getOrElse(index) { 1 }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedMapId = it.getInt(ARG_MAP_ID)
            isAiMode      = it.getBoolean(ARG_AI_MODE)
            difficulty    = it.getString(ARG_DIFFICULTY, "Normal")
            gameId        = it.getString(ARG_GAME_ID, "")
            isMultiplayer = it.getBoolean(ARG_MULTIPLAYER)
            localTeam     = it.getInt(ARG_LOCAL_TEAM, 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupBackground(view)
        setupBoard(view.findViewById(R.id.glBoard))
        setupButtonListeners()
        if (!isMultiplayer) setupBattleResultListener()
        updateTurnUI()
        initFirestore()
        attachFirestoreListener()
        handler.post(animationRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(animationRunnable)
        firestoreListener?.remove()
    }

    private fun bindViews(view: View) {
        tvTurnTitle  = view.findViewById(R.id.tvTurnTitle)
        tvHealthInfo = view.findViewById(R.id.tvHealthInfo)
        tvUnitDetail = view.findViewById(R.id.tvUnitDetail)
        llActions    = view.findViewById(R.id.llActions)
        btnAttack    = view.findViewById(R.id.btnAttack)
        btnSecondary = view.findViewById(R.id.btnSecondary)
        btnWait      = view.findViewById(R.id.btnWait)
    }

    private fun setupBackground(view: View) {
        val ivBackground = view.findViewById<ImageView>(R.id.ivGameBackground)
        if (selectedMapId in 1..5) ivBackground.visibility = View.GONE
        else ivBackground.setImageResource(R.drawable.background4)
    }

    private fun setupButtonListeners() {
        btnWait.setOnClickListener {
            selectedUnit?.let { unit -> grayOutUnit(unit); finishUnitTurn() }
        }
        btnAttack.setOnClickListener    { selectedUnit?.let { showAttackRange(it) } }
        btnSecondary.setOnClickListener { selectedUnit?.let { showSecondaryRange(it) } }
    }

    private fun setupBattleResultListener() {
        parentFragmentManager.setFragmentResultListener("battle_done", viewLifecycleOwner) { _, _ ->
            isBattleInProgress    = false
            isInteractionLocked   = false
            isWaitingForAction    = false
            isWaitingForSecondary = false
            checkUnitDeaths()
            val pendingUnits = units.filter { it.team == currentTeam && !it.hasActed }
            if (pendingUnits.isNotEmpty()) {
                clearSelection()
                updateTurnUI()
                syncGameStateToFirestore()
            } else {
                finishUnitTurn()
            }
            if (isAiMode && currentTeam == 0) {
                handler.postDelayed({ executeAiTurn() }, 500)
            }
        }
    }

    private fun initFirestore() {
        if (gameId.isBlank()) gameId = db.collection("games").document().id
        db.collection("games").document(gameId)
            .set(buildGameState())
            .addOnFailureListener { e -> showToast("Error al inicializar partida: ${e.message}") }
    }

    private fun syncGameStateToFirestore() {
        if (gameId.isBlank()) return
        db.collection("games").document(gameId)
            .update(buildGameState())
            .addOnFailureListener { e -> showToast("Error al sincronizar: ${e.message}") }
    }

    private fun attachFirestoreListener() {
        if (gameId.isBlank()) return
        firestoreListener = db.collection("games").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                if (snapshot.metadata.hasPendingWrites()) return@addSnapshotListener

                val remoteTeam = (snapshot.getLong("currentTeam") ?: currentTeam.toLong()).toInt()

                if (!isMultiplayer) {
                    snapshot.getString("difficulty")
                        ?.takeIf { it != difficulty }
                        ?.let { difficulty = it }
                    if (remoteTeam != currentTeam && !isBattleInProgress) {
                        currentTeam = remoteTeam
                        units.filter { it.team == currentTeam }.forEach {
                            it.hasActed = false; it.hasMoved = false; resetUnitAppearance(it)
                        }
                        clearSelection()
                        updateTurnUI()
                        if (isAiMode && currentTeam == 0) {
                            handler.postDelayed({ executeAiTurn() }, 1000)
                        }
                    }
                    return@addSnapshotListener
                }

                @Suppress("UNCHECKED_CAST")
                val remoteUnits = snapshot.get("units") as? List<Map<String, Any>>

                if (remoteTeam != currentTeam && !isBattleInProgress) {
                    currentTeam = remoteTeam
                    units.filter { it.team == currentTeam }.forEach {
                        it.hasActed = false; it.hasMoved = false; resetUnitAppearance(it)
                    }
                    clearSelection()
                    updateTurnUI()
                }

                if (!isBattleInProgress && remoteUnits != null) {
                    applyRemoteUnits(remoteUnits)
                }
            }
    }

    private fun buildGameState() = mapOf(
        "currentTeam"        to currentTeam,
        "difficulty"         to difficulty,
        "isAiMode"           to isAiMode,
        "isMultiplayer"      to isMultiplayer,
        "selectedMapId"      to selectedMapId,
        "isBattleInProgress" to isBattleInProgress,
        "units"              to serializeUnits()
    )

    private fun serializeUnits(): List<Map<String, Any>> = units.map { u ->
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

    @Suppress("UNCHECKED_CAST")
    private fun applyRemoteUnits(remoteList: List<Map<String, Any>>) {
        if (remoteList.size != units.size) {
            rebuildUnitsFromRemote(remoteList)
            checkVictory()
            return
        }
        for (remote in remoteList) {
            val rTeam = (remote["team"] as? Long)?.toInt() ?: continue
            val rType = remote["type"] as? String ?: continue
            val unit  = units.find { it.team == rTeam && it.type.name == rType } ?: continue

            val newR = (remote["r"] as? Long)?.toInt() ?: unit.r
            val newC = (remote["c"] as? Long)?.toInt() ?: unit.c

            if (newR != unit.r || newC != unit.c) {
                unit.state = when {
                    newR < unit.r -> UnitState.UP
                    newR > unit.r -> UnitState.DOWN
                    newC < unit.c -> UnitState.LEFT
                    else          -> UnitState.RIGHT
                }
                updateUnitsAnimations()
                moveUnitDirectly(unit, newR, newC)
                handler.postDelayed({ unit.state = UnitState.STILL; updateUnitsAnimations() }, 450)
            }

            unit.health      = (remote["health"]      as? Long)?.toInt() ?: unit.health
            unit.maxHealth   = (remote["maxHealth"]   as? Long)?.toInt() ?: unit.maxHealth
            unit.attackPower = (remote["attackPower"] as? Long)?.toInt() ?: unit.attackPower

            if (unit.team != currentTeam) {
                unit.hasActed = remote["hasActed"] as? Boolean ?: unit.hasActed
                unit.hasMoved = remote["hasMoved"] as? Boolean ?: unit.hasMoved
            }

            if (unit.hasActed) grayOutUnit(unit) else resetUnitAppearance(unit)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun rebuildUnitsFromRemote(remoteList: List<Map<String, Any>>) {
        for (unit in units) (unit.view?.parent as? ViewGroup)?.removeView(unit.view)
        units.clear()
        for (remote in remoteList) {
            val typeName = remote["type"] as? String ?: continue
            val type     = UnitType.values().find { it.name == typeName } ?: continue
            val team     = (remote["team"]        as? Long)?.toInt() ?: continue
            val r        = (remote["r"]           as? Long)?.toInt() ?: continue
            val c        = (remote["c"]           as? Long)?.toInt() ?: continue
            val hp       = (remote["health"]      as? Long)?.toInt() ?: type.maxHP
            val maxHp    = (remote["maxHealth"]   as? Long)?.toInt() ?: type.maxHP
            val acted    = remote["hasActed"]    as? Boolean ?: false
            val moved    = remote["hasMoved"]    as? Boolean ?: false
            val atk      = (remote["attackPower"] as? Long)?.toInt() ?: type.baseAttack
            val unit = GameUnit(type, team, r, c, hp, maxHp, acted, moved, atk)
            val frame = boardCells[r][c] ?: continue
            unit.view = buildUnitView(frame, team, type)
            if (acted) grayOutUnit(unit)
            units.add(unit)
        }
        updateTurnUI()
    }

    private fun executeAiTurn() {
        if (isBattleInProgress || units.none { it.team == 1 }) return
        val aiUnits = units.filter { it.team == 0 && !it.hasActed }
        if (aiUnits.isEmpty()) { checkTurnEnd(); return }

        val unit    = aiUnits.first()
        val enemies = units.filter { it.team == 1 }
        val movePos = chooseMovePosition(unit, enemies)
        val path    = findPath(unit, Pair(unit.r, unit.c), movePos)

        animatePath(unit, path, 1) {
            resolveAiAttack(unit, enemies)
            if (!isBattleInProgress) {
                unit.hasActed = true
                grayOutUnit(unit)
                syncGameStateToFirestore()
                handler.postDelayed({ executeAiTurn() }, 800)
            }
        }
    }

    private fun chooseMovePosition(unit: GameUnit, enemies: List<GameUnit>): Pair<Int, Int> {
        val reachable = collectReachablePositions(unit)
        return when (difficulty) {
            "Fácil"   -> reachable.randomOrNull() ?: Pair(unit.r, unit.c)
            "Difícil" -> {
                val weakest = enemies.minByOrNull { it.health.toFloat() / it.maxHealth }
                    ?: return reachable.randomOrNull() ?: Pair(unit.r, unit.c)
                reachable.minByOrNull { pos -> manhattan(pos.first, pos.second, weakest.r, weakest.c) }
                    ?: Pair(unit.r, unit.c)
            }
            else -> {
                if (enemies.isEmpty()) return reachable.randomOrNull() ?: Pair(unit.r, unit.c)
                reachable.minByOrNull { pos ->
                    enemies.minOf { e -> manhattan(pos.first, pos.second, e.r, e.c) }
                } ?: Pair(unit.r, unit.c)
            }
        }
    }

    private fun resolveAiAttack(unit: GameUnit, enemies: List<GameUnit>) {
        val inRange = enemies.filter { e -> manhattan(unit.r, unit.c, e.r, e.c) <= unit.type.attackRange }
        if (inRange.isEmpty()) return
        if (difficulty == "Fácil" && Math.random() < 0.5) return
        val target = if (difficulty == "Difícil") inRange.minByOrNull { it.health }!! else inRange.first()
        val bonus  = if (difficulty == "Difícil") 1 else 0
        var damage = unit.attackPower + bonus
        if (getTileTypeAt(target.r, target.c) == 2) damage = (damage * 0.5).toInt().coerceAtLeast(1)
        startBattle(unit, target, damage)
    }

    private fun collectReachablePositions(unit: GameUnit): List<Pair<Int, Int>> {
        val result  = mutableListOf(Pair(unit.r, unit.c))
        val queue: Queue<Triple<Int, Int, Int>> = LinkedList()
        val visited = mutableMapOf(Pair(unit.r, unit.c) to 0)
        queue.add(Triple(unit.r, unit.c, 0))
        while (queue.isNotEmpty()) {
            val (r, c, dist) = queue.poll()!!
            if (dist > 0) result.add(Pair(r, c))
            if (dist >= unit.type.moveRange) continue
            for ((nr, nc) in neighbors(r, c)) {
                val key = Pair(nr, nc)
                if (key !in visited && canTraverse(nr, nc, unit.type) &&
                    units.none { it.r == nr && it.c == nc }) {
                    visited[key] = dist + 1
                    queue.add(Triple(nr, nc, dist + 1))
                }
            }
        }
        return result
    }

    private fun setupBoard(gridLayout: GridLayout) {
        gridLayout.removeAllViews()
        gridLayout.rowCount    = rows
        gridLayout.columnCount = cols
        gridLayout.clipChildren  = false
        gridLayout.clipToPadding = false
        gridLayout.post {
            val cellSize = minOf(gridLayout.width / cols, gridLayout.height / rows)
            if (cellSize <= 0) return@post
            buildCells(gridLayout, cellSize)
            placeUnits()
        }
    }

    private fun buildCells(gridLayout: GridLayout, cellSize: Int) {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val frame = FrameLayout(requireContext()).apply {
                    clipChildren = false
                    layoutParams = GridLayout.LayoutParams().apply {
                        rowSpec    = GridLayout.spec(r)
                        columnSpec = GridLayout.spec(c)
                        width  = cellSize
                        height = cellSize
                    }
                    setBackgroundResource(R.drawable.cell_border)
                }
                if (selectedMapId in 1..5) {
                    frame.addView(ImageView(requireContext()).apply {
                        tag          = "tile"
                        layoutParams = FrameLayout.LayoutParams(cellSize, cellSize)
                        setImageResource(getTileRes(selectedMapId, r * cols + c))
                        scaleType    = ImageView.ScaleType.CENTER_CROP
                    })
                }
                boardCells[r][c] = frame
                gridLayout.addView(frame)
                frame.setOnClickListener {
                    val blockedByAi  = isAiMode && currentTeam == 0
                    val blockedByMp  = isMultiplayer && currentTeam != localTeam
                    if (!blockedByAi && !blockedByMp) onCellClicked(r, c)
                }
            }
        }
    }

    private fun placeUnits() {
        if (units.isEmpty()) {
            for (col in 1..4) {
                createUnit(r = 0, c = col, team = 0, typeIndex = col - 1)
                createUnit(r = 7, c = col, team = 1, typeIndex = col - 1)
            }
        } else {
            for (unit in units) {
                unit.view?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                    boardCells[unit.r][unit.c]?.addView(it)
                }
            }
        }
    }

    private fun createUnit(r: Int, c: Int, team: Int, typeIndex: Int) {
        val type = UnitType.values()[typeIndex % 4]
        GameUnit(type, team, r, c, type.maxHP, type.maxHP).also { unit ->
            unit.view = buildUnitView(boardCells[r][c]!!, team, type)
            units.add(unit)
        }
    }

    private fun buildUnitView(container: FrameLayout, team: Int, type: UnitType): ImageView {
        val size    = (resources.displayMetrics.density * 56).toInt()
        val teamStr = if (team == 0) "red" else "blue"
        val resId   = resources.getIdentifier(
            "${type.prefix}still${teamStr}1", "drawable", requireContext().packageName
        )
        return ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(size, size).apply { gravity = Gravity.CENTER }
            if (resId != 0) setImageResource(resId)
            setPadding(4, 4, 4, 4)
            isClickable = false
            container.addView(this)
        }
    }

    private fun onCellClicked(r: Int, c: Int) {
        if (isInteractionLocked || isBattleInProgress) return
        when {
            isWaitingForSecondary -> handleSecondaryClick(r, c)
            isWaitingForAction    -> handleActionClick(r, c)
            else                  -> handleSelectionClick(r, c)
        }
    }

    private fun handleSecondaryClick(r: Int, c: Int) {
        if (Pair(r, c) in abilityTiles) {
            units.find { it.r == r && it.c == c }?.let { target ->
                executeSecondaryAbility(selectedUnit!!, target)
                selectedUnit?.let { u -> u.hasActed = true; grayOutUnit(u) }
                finishUnitTurn()
                return
            }
        }
        clearSecondaryMode()
    }

    private fun handleActionClick(r: Int, c: Int) {
        if (Pair(r, c) in attackableTiles) {
            val target = units.find { it.r == r && it.c == c && it.team != currentTeam }
            target?.let {
                var damage = selectedUnit?.attackPower ?: 3
                if (getTileTypeAt(it.r, it.c) == 2) damage = (damage * 0.5).toInt().coerceAtLeast(1)
                startBattle(selectedUnit!!, it, damage)
                return
            }
        }
        if (selectedUnit?.hasMoved == true) {
            clearHighlights()
            attackableTiles.clear()
            isWaitingForAction   = true
            llActions.visibility = View.VISIBLE
        } else {
            clearSelection()
        }
    }

    private fun handleSelectionClick(r: Int, c: Int) {
        val pos        = Pair(r, c)
        val unitAtCell = units.find { it.r == r && it.c == c }
        val sel        = selectedUnit
        if (sel != null) {
            when {
                pos in reachableTiles && !sel.hasMoved && !sel.hasActed -> moveUnit(sel, r, c)
                r == sel.r && c == sel.c && !sel.hasActed               -> showActionUI()
                else -> {
                    clearSelection()
                    unitAtCell?.takeIf { it.team == currentTeam && !it.hasActed }?.let { selectUnit(it) }
                }
            }
        } else {
            unitAtCell?.takeIf { it.team == currentTeam && !it.hasActed }?.let { selectUnit(it) }
        }
    }

    private fun startBattle(attacker: GameUnit, target: GameUnit, damage: Int) {
        target.health     -= damage
        attacker.hasActed  = true
        grayOutUnit(attacker)

        if (isMultiplayer) {
            checkUnitDeaths()
            val pendingUnits = units.filter { it.team == currentTeam && !it.hasActed }
            if (pendingUnits.isNotEmpty()) {
                isWaitingForAction    = false
                isWaitingForSecondary = false
                isInteractionLocked   = false
                clearSelection()
                updateTurnUI()
                syncGameStateToFirestore()
            } else {
                finishUnitTurn()
            }
            return
        }

        isBattleInProgress = true
        syncGameStateToFirestore()
        parentFragmentManager.beginTransaction()
            .add(
                R.id.fragment_container,
                BattleFragment.newInstance(
                    attacker.type.prefix, attacker.team, attacker.type.maxHP, attacker.health,
                    target.type.prefix,   target.team,   target.type.maxHP,   target.health,
                    damage
                )
            )
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
            UnitType.INFANTRY -> { target.attackPower += 2; showToast("${target.type.displayName} potenciado!") }
            UnitType.BAZOOKA  -> {
                target.hasActed = false; target.hasMoved = false; resetUnitAppearance(target)
                showToast("${target.type.displayName} reactivado!")
            }
            UnitType.TANK -> {
                val nr = target.r + (target.r - user.r)
                val nc = target.c + (target.c - user.c)
                if (nr in 0 until rows && nc in 0 until cols && units.none { it.r == nr && it.c == nc }) {
                    moveUnitDirectly(target, nr, nc); showToast("${target.type.displayName} empujado!")
                } else showToast("No se puede empujar ahí")
            }
            UnitType.PLANE -> {
                target.health = minOf(target.maxHealth, target.health + 5)
                showToast("${target.type.displayName} curado!")
            }
        }
    }

    private fun selectUnit(unit: GameUnit) {
        if (unit.hasActed) return
        selectedUnit      = unit
        tvUnitDetail.text = "UNIDAD: ${unit.type.displayName} | SALUD: ${unit.health}/${unit.maxHealth}"
        btnSecondary.text = unit.type.abilityName
        if (unit.hasMoved) showActionUI() else showMovementRange(unit)
    }

    private fun clearSelection() {
        selectedUnit          = null
        isWaitingForAction    = false
        isWaitingForSecondary = false
        reachableTiles.clear()
        attackableTiles.clear()
        abilityTiles.clear()
        llActions.visibility  = View.INVISIBLE
        tvUnitDetail.text     = "SELECCIONA UNA UNIDAD"
        clearHighlights()
    }

    private fun clearSecondaryMode() {
        isWaitingForSecondary = false
        abilityTiles.clear()
        clearHighlights()
        llActions.visibility = View.VISIBLE
    }

    private fun showActionUI() {
        isWaitingForAction    = false
        isWaitingForSecondary = false
        llActions.visibility  = View.VISIBLE
    }

    private fun showMovementRange(unit: GameUnit) {
        clearHighlights()
        reachableTiles.clear()
        val queue: Queue<Triple<Int, Int, Int>> = LinkedList()
        val visited = mutableMapOf(Pair(unit.r, unit.c) to 0)
        queue.add(Triple(unit.r, unit.c, 0))
        while (queue.isNotEmpty()) {
            val (r, c, dist) = queue.poll()!!
            if (dist > 0) { reachableTiles.add(Pair(r, c)); highlightCell(r, c, Color.argb(128, 0, 100, 255)) }
            if (dist >= unit.type.moveRange) continue
            for ((nr, nc) in neighbors(r, c)) {
                val key = Pair(nr, nc)
                if (key !in visited && canTraverse(nr, nc, unit.type) &&
                    units.none { it.r == nr && it.c == nc }) {
                    visited[key] = dist + 1
                    queue.add(Triple(nr, nc, dist + 1))
                }
            }
        }
    }

    private fun showAttackRange(unit: GameUnit) {
        clearHighlights()
        attackableTiles.clear()
        isWaitingForAction   = true
        llActions.visibility = View.INVISIBLE
        val range = unit.type.attackRange
        for (dr in -range..range) for (dc in -range..range) {
            val dist = Math.abs(dr) + Math.abs(dc)
            if (dist !in 1..range) continue
            val nr = unit.r + dr; val nc = unit.c + dc
            if (nr in 0 until rows && nc in 0 until cols) {
                attackableTiles.add(Pair(nr, nc))
                highlightCell(nr, nc, Color.argb(128, 255, 0, 0))
            }
        }
    }

    private fun showSecondaryRange(unit: GameUnit) {
        clearHighlights()
        abilityTiles.clear()
        isWaitingForSecondary = true
        llActions.visibility  = View.INVISIBLE
        for ((nr, nc) in neighbors(unit.r, unit.c)) {
            abilityTiles.add(Pair(nr, nc))
            highlightCell(nr, nc, Color.argb(128, 0, 255, 0))
        }
    }

    private fun highlightCell(r: Int, c: Int, color: Int) {
        val frame = boardCells[r][c] ?: return
        val overlay = View(requireContext()).apply { tag = "highlight"; setBackgroundColor(color) }
        frame.addView(overlay, if (frame.childCount > 0 && frame.getChildAt(0).tag == "tile") 1 else 0)
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

    private fun findPath(unit: GameUnit, start: Pair<Int, Int>, target: Pair<Int, Int>): List<Pair<Int, Int>> {
        val queue: Queue<List<Pair<Int, Int>>> = LinkedList()
        val visited = mutableSetOf(start)
        queue.add(listOf(start))
        while (queue.isNotEmpty()) {
            val path    = queue.poll()!!
            val current = path.last()
            if (current == target) return path
            for ((nr, nc) in neighbors(current.first, current.second)) {
                val next = Pair(nr, nc)
                if (next !in visited && canTraverse(nr, nc, unit.type) &&
                    units.none { it.r == nr && it.c == nc && it != unit }) {
                    visited.add(next); queue.add(path + next)
                }
            }
        }
        return listOf(start)
    }

    private fun animatePath(unit: GameUnit, path: List<Pair<Int, Int>>, index: Int, onComplete: () -> Unit) {
        if (index >= path.size) { unit.state = UnitState.STILL; updateUnitsAnimations(); onComplete(); return }
        val prev = path[index - 1]; val curr = path[index]
        unit.state = when {
            curr.first  < prev.first  -> UnitState.UP
            curr.first  > prev.first  -> UnitState.DOWN
            curr.second < prev.second -> UnitState.LEFT
            else                      -> UnitState.RIGHT
        }
        updateUnitsAnimations()
        moveUnitDirectly(unit, curr.first, curr.second)
        if (isMultiplayer) syncGameStateToFirestore()
        handler.postDelayed({ animatePath(unit, path, index + 1, onComplete) }, 500)
    }

    private fun moveUnitDirectly(unit: GameUnit, newR: Int, newC: Int) {
        val oldFrame = boardCells[unit.r][unit.c]
        val newFrame = boardCells[newR][newC]
        val view     = unit.view ?: return
        val cellSize = oldFrame?.width?.toFloat() ?: 0f
        oldFrame?.removeView(view)
        newFrame?.addView(view)
        newFrame?.elevation = 100f
        view.translationX = (unit.c - newC) * cellSize
        view.translationY = (unit.r - newR) * cellSize
        view.animate().translationX(0f).translationY(0f).setDuration(400)
            .withEndAction { newFrame?.elevation = 0f }.start()
        unit.r = newR; unit.c = newC
    }

    private fun updateUnitsAnimations() {
        for (unit in units) {
            val teamStr     = if (unit.team == 0) "red" else "blue"
            val statePrefix = when {
                unit.hasActed -> "unavailable"
                unit.state == UnitState.UP   -> "up"
                unit.state == UnitState.DOWN -> "down"
                unit.state == UnitState.LEFT || unit.state == UnitState.RIGHT -> "move"
                else -> "still"
            }
            val resId = resources.getIdentifier(
                "${unit.type.prefix}$statePrefix$teamStr$currentFrame",
                "drawable", requireContext().packageName
            )
            if (resId != 0) unit.view?.setImageResource(resId)
            val scale = if (statePrefix in listOf("up", "down", "move")) 1.4f else 1.0f
            unit.view?.scaleY = scale
            unit.view?.scaleX = when (unit.state) {
                UnitState.RIGHT -> -scale
                UnitState.LEFT  ->  scale
                else            -> if ((unit.view?.scaleX ?: 1f) < 0) -scale else scale
            }
        }
    }

    private fun finishUnitTurn() {
        clearSelection()
        checkTurnEnd()
    }

    private fun checkTurnEnd() {
        if (isBattleInProgress) return
        val teamUnits = units.filter { it.team == currentTeam }
        if (teamUnits.isEmpty() || !teamUnits.all { it.hasActed }) return
        currentTeam = 1 - currentTeam
        units.filter { it.team == currentTeam }.forEach {
            it.hasActed = false; it.hasMoved = false; resetUnitAppearance(it)
        }
        updateTurnUI()
        syncGameStateToFirestore()
        if (!isMultiplayer && isAiMode && currentTeam == 0) {
            handler.postDelayed({ executeAiTurn() }, 1000)
        }
    }

    private fun updateTurnUI() {
        val teamName = when {
            isMultiplayer && currentTeam == localTeam -> "TURNO DEL JUGADOR"
            isMultiplayer                             -> "TURNO DEL RIVAL"
            currentTeam == 0 && isAiMode             -> "TURNO ROJO"
            currentTeam == 0                          -> "TURNO ROJO"
            else                                      -> "TURNO AZUL"
        }
        tvTurnTitle.text = "$teamName"
        tvTurnTitle.setTextColor(if (currentTeam == 0) Color.RED else Color.CYAN)
        tvHealthInfo.text = units
            .filter { it.team == currentTeam }
            .joinToString(" | ") { "${it.type.displayName}: ${it.health}" }
    }

    private fun checkVictory() {
        when {
            units.none { it.team == 0 } -> navigateToVictory(winnerTeam = 1)
            units.none { it.team == 1 } -> navigateToVictory(winnerTeam = 0)
        }
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

    private fun getTileTypeAt(r: Int, c: Int) = getTileNum(selectedMapId, r * cols + c)

    private fun canTraverse(r: Int, c: Int, type: UnitType): Boolean =
        when (getTileTypeAt(r, c)) {
            3    -> false
            4    -> type == UnitType.PLANE
            else -> true
        }

    private fun neighbors(r: Int, c: Int): List<Pair<Int, Int>> =
        listOf(r - 1 to c, r + 1 to c, r to c - 1, r to c + 1)
            .filter { (nr, nc) -> nr in 0 until rows && nc in 0 until cols }
            .map { (nr, nc) -> Pair(nr, nc) }

    private fun manhattan(r1: Int, c1: Int, r2: Int, c2: Int) =
        Math.abs(r1 - r2) + Math.abs(c1 - c2)

    private fun grayOutUnit(unit: GameUnit) { unit.hasActed = true; unit.view?.colorFilter = null }
    private fun resetUnitAppearance(unit: GameUnit) { unit.view?.colorFilter = null }
    private fun showToast(message: String) = Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}