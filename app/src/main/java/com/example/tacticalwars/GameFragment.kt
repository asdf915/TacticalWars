package com.example.tacticalwars

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import androidx.fragment.app.Fragment

class GameFragment : Fragment() {

    private var selectedMapResId: Int = 0

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

        val ivBackground = view.findViewById<ImageView>(R.id.ivGameBackground)
        if (selectedMapResId != 0) {
            ivBackground.setImageResource(selectedMapResId)
        }

        val glBoard = view.findViewById<GridLayout>(R.id.glBoard)
        setupBoard(glBoard)
    }

    private fun setupBoard(gridLayout: GridLayout) {
        val rows = 8
        val cols = 6
        
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
                    frame.setBackgroundResource(R.drawable.cell_border) // Need to create this


                    if (r == 0 && c in 1..4) {
                        addUnit(frame, Color.RED, getUnitIcon(c - 1))
                    }

                    if (r == 7 && c in 1..4) {
                        addUnit(frame, Color.BLUE, getUnitIcon(c - 1))
                    }

                    gridLayout.addView(frame)
                }
            }
        }
    }

    private fun addUnit(container: FrameLayout, color: Int, iconRes: Int) {
        val unitView = ImageView(requireContext())
        val size = (resources.displayMetrics.density * 32).toInt()
        val params = FrameLayout.LayoutParams(size, size)
        params.gravity = Gravity.CENTER
        unitView.layoutParams = params
        unitView.setBackgroundColor(color)
        unitView.setImageResource(iconRes)
        unitView.setPadding(8, 8, 8, 8)
        container.addView(unitView)
    }

    private fun getUnitIcon(index: Int): Int {
        return when (index) {
            0 -> android.R.drawable.ic_menu_info_details
            1 -> android.R.drawable.ic_menu_gallery
            2 -> android.R.drawable.ic_menu_camera
            else -> android.R.drawable.ic_menu_compass
        }
    }
}
