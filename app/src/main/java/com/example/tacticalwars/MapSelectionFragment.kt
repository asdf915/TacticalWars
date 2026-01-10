package com.example.tacticalwars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView

class MapSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialCardView>(R.id.cardMap1).setOnClickListener {
            onMapSelected("Mapa 1")
        }

        view.findViewById<MaterialCardView>(R.id.cardMap2).setOnClickListener {
            onMapSelected("Mapa 2")
        }

        view.findViewById<MaterialCardView>(R.id.cardMap3).setOnClickListener {
            onMapSelected("Mapa 3")
        }
    }

    private fun onMapSelected(mapName: String) {
        Toast.makeText(requireContext(), "Seleccionado: $mapName", Toast.LENGTH_SHORT).show()

    }
}
