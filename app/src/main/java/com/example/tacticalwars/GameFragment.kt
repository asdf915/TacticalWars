package com.example.tacticalwars

import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
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
    private val rows = 8
    private val cols = 6
    private val boardCells = Array(rows) { arrayOfNulls<FrameLayout>(cols) }
    private val units = mutableListOf<GameUnit>()
    private var selectedUnit: GameUnit? = null
    private val reachableTiles = mutableSetOf<Pair<Int, Int>>()
    private val attackableTiles = mutableSetOf<Pair<Int, Int>>()
    
    private var currentTeam: Int = 1 // 1: Azul (empieza), 0: Rojo
    private var isWaitingForAction = false

    private lateinit var tvTurnTitle: TextView
    private lateinit var tvHealthInfo: TextView
    private lateinit var tvUnitDetail: TextView
    private lateinit var llActions: LinearLayout
    private lateinit var btnAttack: Button
    private lateinit var btnSecondary: Button
    private lateinit var btnWait: Button

    enum class UnitType(val displayName: String, val moveRange: Int, val attackRange: Int) {
        INFANTRY("Infantería", 3, 1),
        BAZOOKA("Bazuca", 3, 2),
        TANK("Tanque", 2, 1),
        PLANE("Avión", 4, 1)
    }

    data class GameUnit(
        val type: UnitType,
        val team: Int, // 0: Rojo, 1: Azul
        var r: Int,
        var c: Int,
        var health: Int = 10,
        var maxHealth: Int = 10,
        var hasActed: Boolean = false,
        var view: ImageView? = null
    )

    companion object {
        private const val ARG_MAP_RES_ID = "map_res_id"

        fun newInstance(mapResId: Int): GameFragment {
            val fragment = GameFragment()
            val args = Bundle()
            args.putInt(ARG_MAP_RES_ID, mapResId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedMapResId = it.getInt(ARG_MAP_RES_ID)
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
            Toast.makeText(requireContext(), "Habilidad secundaria usada", Toast.LENGTH_SHORT).show()
            selectedUnit?.let { 
                it.hasActed = true
                grayOutUnit(it)
                finishUnitTurn()
            }
        }
    }

    private fun updateTurnUI() {
        val teamName = if (currentTeam == 0) "ROJO" else "AZUL"
        val teamColor = if (currentTeam == 0) Color.RED else Color.CYAN
        tvTurnTitle.text = "TURNO JUGADOR $teamName"
        tvTurnTitle.setTextColor(teamColor)
        
        // Mostrar salud individual de cada unidad viva del equipo actual
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
                        onCellClicked(r, c)
                    }
                }
            }

            // Inicializar unidades
            for (c in 1..4) {
                createUnit(0, c, 0, c - 1) // Equipo Rojo
                createUnit(7, c, 1, c - 1) // Equipo Azul
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
        val unit = GameUnit(type, team, r, c)
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
        if (isWaitingForAction) {
            if (Pair(r, c) in attackableTiles) {
                val target = units.find { it.r == r && it.c == c && it.team != currentTeam }
                target?.let {
                    it.health -= 3
                    if (it.health <= 0) {
                        it.view?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
                        units.remove(it)
                    }
                    selectedUnit?.hasActed = true
                    selectedUnit?.let { u -> grayOutUnit(u) }
                    finishUnitTurn()
                }
            } else {
                clearSelection()
            }
            return
        }

        val unitAtCell = units.find { it.r == r && it.c == c }

        if (selectedUnit != null) {
            if (Pair(r, c) in reachableTiles) {
                moveUnit(selectedUnit!!, r, c)
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

    private fun selectUnit(unit: GameUnit) {
        selectedUnit = unit
        tvUnitDetail.text = "UNIDAD: ${unit.type.displayName} | SALUD: ${unit.health}/${unit.maxHealth}"
        showMovementRange(unit)
    }

    private fun clearSelection() {
        selectedUnit = null
        reachableTiles.clear()
        attackableTiles.clear()
        isWaitingForAction = false
        llActions.visibility = View.INVISIBLE
        tvUnitDetail.text = "SELECCIONA UNA UNIDAD"
        clearHighlights()
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
        val oldFrame = boardCells[unit.r][unit.c]
        val newFrame = boardCells[newR][newC]
        unit.view?.let {
            oldFrame?.removeView(it)
            newFrame?.addView(it)
        }
        unit.r = newR
        unit.c = newC
        clearHighlights()
        reachableTiles.clear()
    }

    private fun showActionUI() {
        llActions.visibility = View.VISIBLE
        isWaitingForAction = true
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
                resetUnitAppearance(it)
            }
        }
    }

    private fun grayOutUnit(unit: GameUnit) {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrix)
        unit.view?.colorFilter = filter
    }

    private fun resetUnitAppearance(unit: GameUnit) {
        unit.view?.colorFilter = null
    }
}
