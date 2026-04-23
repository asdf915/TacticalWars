package com.example.tacticalwars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial

class MapSelectionFragment : Fragment() {

    private var difficulty: String = "Normal"

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

        val switchAI = view.findViewById<SwitchMaterial>(R.id.switchAI)
        val glPreview1 = view.findViewById<GridLayout>(R.id.glPreviewMap1)
        val glPreview2 = view.findViewById<GridLayout>(R.id.glPreviewMap2)
        val glPreview3 = view.findViewById<GridLayout>(R.id.glPreviewMap3)

        setupMap1Preview(glPreview1)
        setupMap2Preview(glPreview2)
        setupMap3Preview(glPreview3)

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialCardView>(R.id.cardMap1).setOnClickListener {
            onMapSelected(1, switchAI.isChecked)
        }

        view.findViewById<MaterialCardView>(R.id.cardMap2).setOnClickListener {
            onMapSelected(2, switchAI.isChecked)
        }

        view.findViewById<MaterialCardView>(R.id.cardMap3).setOnClickListener {
            onMapSelected(3, switchAI.isChecked)
        }
    }

    private fun setupMap1Preview(gridLayout: GridLayout) {
        val mapData = listOf(
            1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 2, 1,
            1, 1, 2, 1, 1, 1,
            1, 2, 1, 1, 2, 2,
            1, 2, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 1,
            1, 1, 1, 1, 2, 1,
            2, 1, 2, 1, 1, 1
        )
        fillGridWithTiles(gridLayout, mapData)
    }

    private fun setupMap2Preview(gridLayout: GridLayout) {
        val mapData = listOf(
            1, 1, 1, 4, 4, 4,
            1, 1, 2, 4, 4, 4,
            4, 5, 4, 4, 1, 1,
            1, 1, 1, 6, 1, 1,
            1, 1, 1, 4, 4, 2,
            1, 1, 1, 1, 4, 1,
            1, 1, 1, 1, 6, 1,
            2, 1, 2, 1, 4, 1
        )
        fillGridWithTiles(gridLayout, mapData)
    }

    private fun setupMap3Preview(gridLayout: GridLayout) {
        val mapData = listOf(
            1, 3, 1, 1, 1, 2,
            1, 1, 1, 1, 1, 3,
            4, 4, 1, 1, 1, 1,
            1, 4, 4, 5, 4, 4,
            1, 1, 1, 1, 2, 1,
            1, 2, 1, 1, 1, 1,
            1, 3, 1, 1, 1, 1,
            1, 1, 1, 1, 1, 2
        )
        fillGridWithTiles(gridLayout, mapData)
    }

    private fun fillGridWithTiles(gridLayout: GridLayout, mapData: List<Int>) {
        val rows = 8
        val cols = 6
        gridLayout.post {
            val cellSize = gridLayout.width / cols
            for (i in 0 until (rows * cols)) {
                val tileNum = if (i < mapData.size) mapData[i] else 1
                val ivTile = ImageView(requireContext())
                val params = GridLayout.LayoutParams()
                params.width = cellSize
                params.height = cellSize
                ivTile.layoutParams = params
                ivTile.scaleType = ImageView.ScaleType.CENTER_CROP
                ivTile.setImageResource(getTileDrawable(tileNum))
                gridLayout.addView(ivTile)
            }
        }
    }

    private fun getTileDrawable(tileNum: Int): Int {
        return when (tileNum) {
            1 -> R.drawable.tile1
            2 -> R.drawable.tile2
            3 -> R.drawable.tile3
            4 -> R.drawable.tile4
            5 -> R.drawable.tile5
            6 -> R.drawable.tile6
            else -> R.drawable.tile1
        }
    }

    private fun onMapSelected(mapId: Int, isAiMode: Boolean) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GameFragment.newInstance(mapId, isAiMode, difficulty))
            .addToBackStack(null)
            .commit()
    }
}
