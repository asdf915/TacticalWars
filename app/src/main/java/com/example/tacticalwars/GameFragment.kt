package com.example.tacticalwars

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import java.util.LinkedList
import java.util.Queue

class GameFragment : Fragment() {

    private var selectedMapResId: Int = 0
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
    
    private var currentTeam: Int = 1 // 1: Azul (empieza), 0: Rojo
    private var isWaitingForAction = false
    private var isWaitingForSecondary = false

    private lateinit var tvTurnTitle: TextView
    private lateinit var tvHealthInfo: TextView
    private lateinit var tvUnitDetail: TextView
    private lateinit var llActions: LinearLayout
    private lateinit var btnAttack: Button
    private lateinit var btnSecondary: Button
    private lateinit var btnWait: Button

    enum class UnitType(val displayName: String, val moveRange: Int, val attackRange: Int, val maxHP: Int, val abilityName: String) {
        INFANTRY("Infantería", 3, 1, 10, "Potenciar"),
        BAZOOKA("Bazuca", 3, 2, 12, "Reactivar"),
        TANK("Tanque", 2, 1, 20, "Empujar"),
        PLANE("Avión", 4, 1, 8, "Curar")
    }

    data class GameUnit(
        val type: UnitType,
        val team: Int, // 0: Rojo, 1: Azul
        var r: Int,
        var c: Int,
        var health: Int,
        var maxHealth: Int,
        var hasActed: Boolean = false,
        var hasMoved: Boolean = false,
        var attackPower: Int = 3,
        var view: ImageView? = null
    )

    companion object {
        private const val ARG_MAP_RES_ID = "map_res_id"
        private const val ARG_AI_MODE = "ai_mode"
        private const val ARG_DIFFICULTY = "difficulty"

        fun newInstance(mapResId: Int, aiMode: Boolean = false, difficulty: String = "Normal"): GameFragment {
            val fragment = GameFragment()
            val args = Bundle()
            args.putInt(ARG_MAP_RES_ID, mapResId)
            args.putBoolean(ARG_AI_MODE, aiMode)
            args.putString(ARG_DIFFICULTY, difficulty)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedMapResId = it.getInt(ARG_MAP_RES_ID)
            isAiMode = it.getBoolean(ARG_AI_MODE)
            difficulty = it.getString(ARG_DIFFICULTY, "Normal")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTurnTitle = view.findViewById(R.id.tvTurnTitle)
        tvHealthInfo = view.findViewById(R.id.tvHealthInfo)
        tvUnitDetail = view.findViewById(R.id.tvUnitDetail)
        llActions = view.findViewById(R.id.llActions)
        btnAttack = view.findViewById(R.id.btnAttack)
        btnSecondary = view.findViewById(R.id.btnSecondary)
        btnWait = view.findViewById(R.id.btnWait)

        val ivBackground = view.findViewById<ImageView>(R.id.ivGameBackground)
        if (selectedMapResId != 0) {
            ivBackground.setImageResource(selectedMapResId)
        }

        val glBoard = view.findViewById<GridLayout>(R.id.glBoard)
        setupBoard(glBoard)
        updateTurnUI()

        btnWait.setOnClickListener {
            selectedUnit?.let { unit ->
                unit.hasActed = true
                grayOutUnit(unit)
                finishUnitTurn()
            }
        }

        btnAttack.setOnClickListener {
            selectedUnit?.let { showAttackRange(it) }
        }
        
        btnSecondary.setOnClickListener {
            selectedUnit?.let { showSecondaryRange(it) }
        }
    }

    private fun updateTurnUI() {
        val teamName = if (currentTeam == 0) (if (isAiMode) "ROJO (IA)" else "ROJO") else "AZUL"
        val teamColor = if (currentTeam == 0) Color.RED else Color.CYAN
        tvTurnTitle.text = "TURNO JUGADOR $teamName"
        tvTurnTitle.setTextColor(teamColor)
        
        val healthText = units.filter { it.team == currentTeam }
            .joinToString(" | ") { "${it.type.displayName}: ${it.health}" }
        tvHealthInfo.text = healthText
    }

    private fun setupBoard(gridLayout: GridLayout) {
        gridLayout.rowCount = rows
        gridLayout.columnCount = cols

        gridLayout.post {
            val cellWidth = gridLayout.width / cols
            val cellHeight = gridLayout.height / rows
            val cellSize = Math.min(cellWidth, cellHeight)

            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val frame = FrameLayout(requireContext())
                    val params = GridLayout.LayoutParams()
                    params.rowSpec = GridLayout.spec(r)
                    params.columnSpec = GridLayout.spec(c)
                    params.width = cellSize
                    params.height = cellSize
                    frame.layoutParams = params
                    frame.setBackgroundResource(R.drawable.cell_border)
                    
                    boardCells[r][c] = frame
                    gridLayout.addView(frame)

                    frame.setOnClickListener {
                        if (!(isAiMode && currentTeam == 0)) {
                            onCellClicked(r, c)
                        }
                    }
                }
            }

            for (c in 1..4) {
                createUnit(0, c, 0, c - 1)
                createUnit(7, c, 1, c - 1)
            }
        }
    }

    private fun createUnit(r: Int, c: Int, team: Int, typeIndex: Int) {
        val type = when (typeIndex % 4) {
            0 -> UnitType.INFANTRY
            1 -> UnitType.BAZOOKA
            2 -> UnitType.TANK
            else -> UnitType.PLANE
        }
        val unit = GameUnit(type, team, r, c, type.maxHP, type.maxHP)
        val unitView = addUnitToUI(boardCells[r][c]!!, if (team == 0) Color.RED else Color.BLUE, getUnitIcon(typeIndex))
        unit.view = unitView
        units.add(unit)
    }

    private fun addUnitToUI(container: FrameLayout, color: Int, iconRes: Int): ImageView {
        val unitView = ImageView(requireContext())
        val size = (resources.displayMetrics.density * 32).toInt()
        val params = FrameLayout.LayoutParams(size, size)
        params.gravity = Gravity.CENTER
        unitView.layoutParams = params
        unitView.setBackgroundColor(color)
        unitView.setImageResource(iconRes)
        unitView.setPadding(8, 8, 8, 8)
        unitView.isClickable = false
        container.addView(unitView)
        return unitView
    }

    private fun getUnitIcon(index: Int): Int {
        return when (index % 4) {
            0 -> android.R.drawable.ic_menu_info_details
            1 -> android.R.drawable.ic_menu_gallery
            2 -> android.R.drawable.ic_menu_camera
            else -> android.R.drawable.ic_menu_compass
        }
    }

    private fun onCellClicked(r: Int, c: Int) {
        if (isWaitingForSecondary) {
            if (Pair(r, c) in abilityTiles) {
                val target = units.find { it.r == r && it.c == c }
                if (target != null) {
                    executeSecondaryAbility(selectedUnit!!, target)
                    selectedUnit?.hasActed = true
                    selectedUnit?.let { grayOutUnit(it) }
                    finishUnitTurn()
                    return
                }
            }
            clearSecondaryMode()
            return
        }

        if (isWaitingForAction) {
            if (Pair(r, c) in attackableTiles) {
                val target = units.find { it.r == r && it.c == c && it.team != currentTeam }
                target?.let {
                    it.health -= selectedUnit?.attackPower ?: 3
                    if (it.health <= 0) {
                        it.view?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
                        units.remove(it)
                        checkVictory()
                    }
                    selectedUnit?.hasActed = true
                    selectedUnit?.let { u -> grayOutUnit(u) }
                    finishUnitTurn()
                    return
                }
            } else {
                if (selectedUnit?.hasMoved == true) {
                    clearHighlights()
                    attackableTiles.clear()
                    isWaitingForAction = true
                    llActions.visibility = View.VISIBLE
                    return
                }
                clearSelection()
            }
            return
        }

        val unitAtCell = units.find { it.r == r && it.c == c }

        if (selectedUnit != null) {
            if (Pair(r, c) in reachableTiles && !selectedUnit!!.hasMoved) {
                moveUnit(selectedUnit!!, r, c)
                showActionUI()
            } else if (r == selectedUnit!!.r && c == selectedUnit!!.c) {
                showActionUI()
            } else {
                clearSelection()
                if (unitAtCell != null && unitAtCell.team == currentTeam && !unitAtCell.hasActed) {
                    selectUnit(unitAtCell)
                }
            }
        } else if (unitAtCell != null && unitAtCell.team == currentTeam && !unitAtCell.hasActed) {
            selectUnit(unitAtCell)
        }
    }

    private fun executeSecondaryAbility(user: GameUnit, target: GameUnit) {
        when (user.type) {
            UnitType.INFANTRY -> {
                target.attackPower += 2
                Toast.makeText(requireContext(), "${target.type.displayName} potenciado!", Toast.LENGTH_SHORT).show()
            }
            UnitType.BAZOOKA -> {
                target.hasActed = false
                target.hasMoved = false
                resetUnitAppearance(target)
                Toast.makeText(requireContext(), "${target.type.displayName} reactivado!", Toast.LENGTH_SHORT).show()
            }
            UnitType.TANK -> {
                val dr = target.r - user.r
                val dc = target.c - user.c
                val nr = target.r + dr
                val nc = target.c + dc
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
        unit.view?.let {
            oldFrame?.removeView(it)
            newFrame?.addView(it)
        }
        unit.r = newR
        unit.c = newC
    }

    private fun selectUnit(unit: GameUnit) {
        selectedUnit = unit
        tvUnitDetail.text = "UNIDAD: ${unit.type.displayName} | SALUD: ${unit.health}/${unit.maxHealth}"
        btnSecondary.text = unit.type.abilityName
        if (unit.hasMoved) {
            showActionUI()
        } else {
            showMovementRange(unit)
        }
    }

    private fun clearSelection() {
        selectedUnit = null
        reachableTiles.clear()
        attackableTiles.clear()
        abilityTiles.clear()
        isWaitingForAction = false
        isWaitingForSecondary = false
        llActions.visibility = View.INVISIBLE
        tvUnitDetail.text = "SELECCIONA UNA UNIDAD"
        clearHighlights()
    }

    private fun clearSecondaryMode() {
        isWaitingForSecondary = false
        abilityTiles.clear()
        clearHighlights()
        llActions.visibility = View.VISIBLE
    }

    private fun showMovementRange(unit: GameUnit) {
        clearHighlights()
        reachableTiles.clear()
        
        val queue: Queue<Triple<Int, Int, Int>> = LinkedList()
        queue.add(Triple(unit.r, unit.c, 0))
        
        val visited = mutableMapOf<Pair<Int, Int>, Int>()
        visited[Pair(unit.r, unit.c)] = 0

        while (queue.isNotEmpty()) {
            val (currR, currC, dist) = queue.poll()!!

            if (dist > 0) {
                reachableTiles.add(Pair(currR, currC))
                highlightCell(currR, currC, Color.argb(128, 0, 100, 255))
            }

            if (dist < unit.type.moveRange) {
                val neighbors = arrayOf(
                    Pair(currR - 1, currC), Pair(currR + 1, currC),
                    Pair(currR, currC - 1), Pair(currR, currC + 1)
                )

                for (neighbor in neighbors) {
                    val nr = neighbor.first
                    val nc = neighbor.second
                    if (nr in 0 until rows && nc in 0 until cols) {
                        val unitAtNeighbor = units.find { it.r == nr && it.c == nc }
                        if (unitAtNeighbor == null || unitAtNeighbor.team == unit.team) {
                            val nextDist = dist + 1
                            if (visited[Pair(nr, nc)] == null || visited[Pair(nr, nc)]!! > nextDist) {
                                visited[Pair(nr, nc)] = nextDist
                                queue.add(Triple(nr, nc, nextDist))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showAttackRange(unit: GameUnit) {
        clearHighlights()
        attackableTiles.clear()
        isWaitingForAction = true
        llActions.visibility = View.INVISIBLE

        val range = unit.type.attackRange
        for (dr in -range..range) {
            for (dc in -range..range) {
                val dist = Math.abs(dr) + Math.abs(dc)
                if (dist in 1..range) {
                    val nr = unit.r + dr
                    val nc = unit.c + dc
                    if (nr in 0 until rows && nc in 0 until cols) {
                        attackableTiles.add(Pair(nr, nc))
                        highlightCell(nr, nc, Color.argb(128, 255, 0, 0))
                    }
                }
            }
        }
    }

    private fun showSecondaryRange(unit: GameUnit) {
        clearHighlights()
        abilityTiles.clear()
        isWaitingForSecondary = true
        llActions.visibility = View.INVISIBLE

        val neighbors = arrayOf(Pair(unit.r - 1, unit.c), Pair(unit.r + 1, unit.c), Pair(unit.r, unit.c - 1), Pair(unit.r, unit.c + 1))
        for (n in neighbors) {
            if (n.first in 0 until rows && n.second in 0 until cols) {
                abilityTiles.add(n)
                highlightCell(n.first, n.second, Color.argb(128, 0, 255, 0))
            }
        }
    }

    private fun highlightCell(r: Int, c: Int, color: Int) {
        val frame = boardCells[r][c]
        val highlight = View(requireContext())
        highlight.tag = "highlight"
        highlight.setBackgroundColor(color)
        frame?.addView(highlight, 0)
    }

    private fun clearHighlights() {
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val frame = boardCells[r][c] ?: continue
                val highlight = frame.findViewWithTag<View>("highlight")
                if (highlight != null) frame.removeView(highlight)
            }
        }
    }

    private fun moveUnit(unit: GameUnit, newR: Int, newC: Int) {
        moveUnitDirectly(unit, newR, newC)
        unit.hasMoved = true
        clearHighlights()
        reachableTiles.clear()
    }

    private fun showActionUI() {
        llActions.visibility = View.VISIBLE
        isWaitingForAction = false
        isWaitingForSecondary = false
    }

    private fun finishUnitTurn() {
        clearSelection()
        checkTurnEnd()
        updateTurnUI()
    }

    private fun checkTurnEnd() {
        val currentTeamUnits = units.filter { it.team == currentTeam }
        if (currentTeamUnits.isNotEmpty() && currentTeamUnits.all { it.hasActed }) {
            currentTeam = 1 - currentTeam
            units.filter { it.team == currentTeam }.forEach { 
                it.hasActed = false 
                it.hasMoved = false
                resetUnitAppearance(it)
            }
            if (isAiMode && currentTeam == 0) {
                Handler(Looper.getMainLooper()).postDelayed({ executeAiTurn() }, 1000)
            }
        }
    }

    private fun executeAiTurn() {
        if (units.none { it.team == 1 }) return // Player already lost

        val aiUnits = units.filter { it.team == 0 && !it.hasActed }
        if (aiUnits.isEmpty()) {
            checkTurnEnd()
            updateTurnUI()
            return
        }

        val unit = aiUnits.first()
        val enemyUnits = units.filter { it.team == 1 }

        val movePos = findAiMovePosition(unit, enemyUnits, difficulty != "Fácil")
        moveUnitDirectly(unit, movePos.first, movePos.second)
        
        aiAttackIfPossible(unit, enemyUnits)

        unit.hasActed = true
        grayOutUnit(unit)
        
        Handler(Looper.getMainLooper()).postDelayed({ executeAiTurn() }, 800)
    }

    private fun findAiMovePosition(unit: GameUnit, enemies: List<GameUnit>, aggressive: Boolean): Pair<Int, Int> {
        val possibleMoves = mutableListOf(Pair(unit.r, unit.c))
        val queue: Queue<Triple<Int, Int, Int>> = LinkedList()
        queue.add(Triple(unit.r, unit.c, 0))
        val visited = mutableMapOf(Pair(unit.r, unit.c) to 0)

        while (queue.isNotEmpty()) {
            val (currR, currC, dist) = queue.poll()!!
            if (dist > 0) possibleMoves.add(Pair(currR, currC))
            if (dist < unit.type.moveRange) {
                for (n in arrayOf(Pair(currR-1, currC), Pair(currR+1, currC), Pair(currR, currC-1), Pair(currR, currC+1))) {
                    if (n.first in 0 until rows && n.second in 0 until cols) {
                        if (units.none { it.r == n.first && it.c == n.second } && visited[n] == null) {
                            visited[n] = dist + 1
                            queue.add(Triple(n.first, n.second, dist + 1))
                        }
                    }
                }
            }
        }

        if (!aggressive || enemies.isEmpty()) return possibleMoves.random()

        return possibleMoves.minByOrNull { pos ->
            enemies.minOf { enemy -> Math.abs(pos.first - enemy.r) + Math.abs(pos.second - enemy.c) }
        } ?: Pair(unit.r, unit.c)
    }

    private fun aiAttackIfPossible(unit: GameUnit, enemies: List<GameUnit>) {
        val target = enemies.find { enemy ->
            val dist = Math.abs(unit.r - enemy.r) + Math.abs(unit.c - enemy.c)
            dist <= unit.type.attackRange
        }
        target?.let {
            it.health -= unit.attackPower
            if (it.health <= 0) {
                it.view?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
                units.remove(it)
                checkVictory()
            }
        }
    }

    private fun checkVictory() {
        val redUnits = units.filter { it.team == 0 }
        val blueUnits = units.filter { it.team == 1 }

        if (redUnits.isEmpty()) {
            navigateToVictory(1)
        } else if (blueUnits.isEmpty()) {
            navigateToVictory(0)
        }
    }

    private fun navigateToVictory(winnerTeam: Int) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, VictoryFragment.newInstance(winnerTeam))
            .commit()
    }

    private fun grayOutUnit(unit: GameUnit) {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        unit.view?.colorFilter = filter
        unit.view?.setBackgroundColor(Color.GRAY)
    }

    private fun resetUnitAppearance(unit: GameUnit) {
        unit.view?.colorFilter = null
        val originalColor = if (unit.team == 0) Color.RED else Color.BLUE
        unit.view?.setBackgroundColor(originalColor)
    }
}
