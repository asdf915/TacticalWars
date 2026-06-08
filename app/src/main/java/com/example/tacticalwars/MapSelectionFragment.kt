package com.example.tacticalwars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class MapSelectionFragment : Fragment() {

    private var difficulty: String = "Normal"

    private var selectedMapId: Int? = null

    private lateinit var cards: List<MaterialCardView>
    private lateinit var btnStart: Button
    private lateinit var switchAI: SwitchMaterial

    companion object {
        private const val ARG_DIFFICULTY = "difficulty"

        fun newInstance(difficulty: String): MapSelectionFragment {
            val fragment = MapSelectionFragment()
            val args = Bundle()
            args.putString(ARG_DIFFICULTY, difficulty)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            difficulty = it.getString(ARG_DIFFICULTY, "Normal")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchAI = view.findViewById(R.id.switchAI)
        btnStart = view.findViewById(R.id.btnStart)

        cards = listOf(
            view.findViewById(R.id.cardMap1),
            view.findViewById(R.id.cardMap2),
            view.findViewById(R.id.cardMap3),
            view.findViewById(R.id.cardMap4),
            view.findViewById(R.id.cardMap5)
        )

        setupMap1Preview(view.findViewById(R.id.glPreviewMap1))
        setupMap2Preview(view.findViewById(R.id.glPreviewMap2))
        setupMap3Preview(view.findViewById(R.id.glPreviewMap3))
        setupMap4Preview(view.findViewById(R.id.glPreviewMap4))
        setupMap5Preview(view.findViewById(R.id.glPreviewMap5))

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cards.forEachIndexed { index, card ->
            card.setOnClickListener { selectMap(index + 1) }
        }

        switchAI.setOnCheckedChangeListener { _, _ -> updateStartButton() }

        btnStart.setOnClickListener { onStartPressed() }

        updateStartButton()
    }


    private fun selectMap(mapId: Int) {
        selectedMapId = mapId

        cards.forEachIndexed { index, card ->
            if (index + 1 == mapId) {
                card.strokeWidth = 6
                card.strokeColor = requireContext().getColor(R.color.purple_500)
            } else {
                card.strokeWidth = 0
            }
        }

        btnStart.isEnabled = true
        btnStart.alpha = 1f
    }


    private fun updateStartButton() {
        btnStart.text = if (switchAI.isChecked) "▶  INICIAR VS IA" else "▶  INICIAR MULTIJUGADOR"
    }

    private fun onStartPressed() {
        val mapId = selectedMapId
        if (mapId == null) {
            Toast.makeText(requireContext(), "Elige un mapa primero", Toast.LENGTH_SHORT).show()
            return
        }

        if (switchAI.isChecked) {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    GameFragment.newInstance(
                        mapId      = mapId,
                        aiMode     = true,
                        difficulty = difficulty
                    )
                )
                .addToBackStack(null)
                .commit()
        } else {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    MultiplayerLobbyFragment.newInstance(preselectedMapId = mapId)
                )
                .addToBackStack(null)
                .commit()
        }
    }


    private fun setupMap1Preview(gridLayout: GridLayout) {
        fillGridWithTiles(gridLayout, listOf(
            1,1,1,1,1,1, 1,1,1,1,2,1, 1,1,2,1,1,1, 1,2,1,1,2,2,
            1,2,1,1,1,1, 1,1,1,1,1,1, 1,1,1,1,2,1, 2,1,2,1,1,1
        ))
    }

    private fun setupMap2Preview(gridLayout: GridLayout) {
        fillGridWithTiles(gridLayout, listOf(
            1,1,1,1,4,4, 1,1,2,4,4,4, 4,5,4,4,1,1, 1,1,1,6,1,1,
            1,1,1,4,4,2, 1,1,1,1,4,1, 1,1,1,1,6,1, 2,1,2,1,4,1
        ))
    }

    private fun setupMap3Preview(gridLayout: GridLayout) {
        fillGridWithTiles(gridLayout, listOf(
            3,1,1,1,1,2, 1,1,1,1,1,3, 4,4,1,1,1,1, 1,4,4,5,4,4,
            1,1,1,1,2,1, 1,2,1,1,1,1, 1,3,1,1,1,1, 1,1,1,1,1,2
        ))
    }

    private fun setupMap4Preview(gridLayout: GridLayout) {
        fillGridWithTiles(gridLayout, listOf(
            1,1,1,1,2,4, 4,5,4,5,4,4, 4,5,4,5,4,4, 2,1,1,1,2,2,
            1,1,2,1,1,1, 4,5,4,5,4,4, 4,5,4,5,4,4, 4,1,1,1,1,2
        ))
    }

    private fun setupMap5Preview(gridLayout: GridLayout) {
        fillGridWithTiles(gridLayout, listOf(
            3,1,1,1,2,2, 3,1,4,1,4,1, 1,1,1,1,1,1, 5,4,4,5,5,4,
            1,2,6,1,1,2, 3,2,4,1,2,2, 1,1,1,1,1,1, 2,1,2,2,1,1
        ))
    }

    private fun fillGridWithTiles(gridLayout: GridLayout, mapData: List<Int>) {
        val cols = 6
        gridLayout.post {

            val width = if (gridLayout.width > 0) gridLayout.width else 1080
            val cellSize = width / cols

            mapData.forEach { tileNum ->

                val ivTile = ImageView(gridLayout.context)
                ivTile.layoutParams = GridLayout.LayoutParams().apply {
                    this.width = cellSize
                    this.height = cellSize
                }
                ivTile.scaleType = ImageView.ScaleType.CENTER_CROP
                ivTile.setImageResource(getTileDrawable(tileNum))
                gridLayout.addView(ivTile)
            }
        }
    }
    private fun getTileDrawable(tileNum: Int) = when (tileNum) {
        1 -> R.drawable.tile1
        2 -> R.drawable.tile2
        3 -> R.drawable.tile3
        4 -> R.drawable.tile4
        5 -> R.drawable.tile5
        6 -> R.drawable.tile6
        else -> R.drawable.tile1
    }
}