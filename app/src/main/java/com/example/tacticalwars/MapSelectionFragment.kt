package com.example.tacticalwars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialCardView>(R.id.cardMap1).setOnClickListener {
            onMapSelected(R.drawable.mapa1, switchAI.isChecked)
        }

        view.findViewById<MaterialCardView>(R.id.cardMap2).setOnClickListener {
            onMapSelected(R.drawable.mapa2, switchAI.isChecked)
        }

        view.findViewById<MaterialCardView>(R.id.cardMap3).setOnClickListener {
            onMapSelected(R.drawable.mapa3, switchAI.isChecked)
        }
    }

    private fun onMapSelected(mapResId: Int, isAiMode: Boolean) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GameFragment.newInstance(mapResId, isAiMode, difficulty))
            .addToBackStack(null)
            .commit()
    }
}
