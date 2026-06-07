package com.example.tacticalwars

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MultiplayerLobbyFragment : Fragment() {


    private val db = FirebaseFirestore.getInstance()
    private var lobbyListener: ListenerRegistration? = null


    private var gameId: String = ""
    private var selectedMapId: Int = 1
    private var localTeam: Int = 0

    private lateinit var layoutMenu: LinearLayout
    private lateinit var layoutCreate: LinearLayout
    private lateinit var layoutJoin: LinearLayout
    private lateinit var layoutWaiting: LinearLayout

    private lateinit var tvGameCode: TextView
    private lateinit var btnMap1: Button
    private lateinit var btnMap2: Button
    private lateinit var btnMap3: Button
    private lateinit var btnMap4: Button
    private lateinit var btnMap5: Button
    private lateinit var btnCancelCreate: Button
    private lateinit var progressWaiting: ProgressBar
    private lateinit var tvWaitingStatus: TextView


    private lateinit var etGameCode: EditText
    private lateinit var btnJoinConfirm: Button
    private lateinit var btnCancelJoin: Button


    companion object {
        private const val ARG_MAP_ID = "preselected_map_id"

        fun newInstance(preselectedMapId: Int): MultiplayerLobbyFragment = MultiplayerLobbyFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_MAP_ID, preselectedMapId)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedMapId = it.getInt(ARG_MAP_ID, 1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_multiplayer_lobby, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        showMenu()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lobbyListener?.remove()
    }


    private fun bindViews(view: View) {
        layoutMenu    = view.findViewById(R.id.layoutMenu)
        layoutCreate  = view.findViewById(R.id.layoutCreate)
        layoutJoin    = view.findViewById(R.id.layoutJoin)
        layoutWaiting = view.findViewById(R.id.layoutWaiting)

        tvGameCode      = view.findViewById(R.id.tvGameCode)
        btnMap1         = view.findViewById(R.id.btnMap1)
        btnMap2         = view.findViewById(R.id.btnMap2)
        btnMap3         = view.findViewById(R.id.btnMap3)
        btnMap4         = view.findViewById(R.id.btnMap4)
        btnMap5         = view.findViewById(R.id.btnMap5)
        btnCancelCreate = view.findViewById(R.id.btnCancelCreate)
        progressWaiting = view.findViewById(R.id.progressWaiting)
        tvWaitingStatus = view.findViewById(R.id.tvWaitingStatus)

        etGameCode    = view.findViewById(R.id.etGameCode)
        btnJoinConfirm = view.findViewById(R.id.btnJoinConfirm)
        btnCancelJoin  = view.findViewById(R.id.btnCancelJoin)


        view.findViewById<Button>(R.id.btnCreateGame).setOnClickListener { 

            createGame(selectedMapId)
        }
        view.findViewById<Button>(R.id.btnJoinGame).setOnClickListener  { showJoin() }
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        listOf(btnMap1, btnMap2, btnMap3, btnMap4, btnMap5).forEachIndexed { idx, btn ->
            btn.setOnClickListener { createGame(mapId = idx + 1) }
        }
        btnCancelCreate.setOnClickListener {
            cancelCreatedGame()
            showMenu()
        }
        btnJoinConfirm.setOnClickListener {
            val code = etGameCode.text.toString().trim().uppercase()
            if (code.length == 6) joinGame(code) else showToast("Código de 6 caracteres")
        }
        btnCancelJoin.setOnClickListener { showMenu() }
    }


    private fun showMenu() {
        lobbyListener?.remove()
        gameId = ""
        layoutMenu.visibility    = View.VISIBLE
        layoutCreate.visibility  = View.GONE
        layoutJoin.visibility    = View.GONE
        layoutWaiting.visibility = View.GONE
    }

    private fun showCreate() {
        layoutMenu.visibility    = View.GONE
        layoutCreate.visibility  = View.VISIBLE
        layoutJoin.visibility    = View.GONE
        layoutWaiting.visibility = View.GONE
    }

    private fun showJoin() {
        layoutMenu.visibility    = View.GONE
        layoutCreate.visibility  = View.GONE
        layoutJoin.visibility    = View.VISIBLE
        layoutWaiting.visibility = View.GONE
        etGameCode.setText("")
    }

    private fun showWaiting() {
        layoutMenu.visibility    = View.GONE
        layoutCreate.visibility  = View.GONE
        layoutJoin.visibility    = View.GONE
        layoutWaiting.visibility = View.VISIBLE
    }


    private fun createGame(mapId: Int) {
        selectedMapId = mapId
        localTeam = 0
        val code = generateRoomCode()
        gameId = code

        val lobbyData = mapOf(
            "status"       to "waiting",
            "selectedMapId" to mapId,
            "createdAt"    to System.currentTimeMillis()
        )

        db.collection("lobbies").document(gameId)
            .set(lobbyData)
            .addOnSuccessListener {
                tvGameCode.text = gameId
                showWaiting()
                tvWaitingStatus.text = "Esperando que el rival se una..."
                progressWaiting.visibility = View.VISIBLE
                listenForOpponent()
            }
            .addOnFailureListener { e ->
                showToast("Error al crear partida: ${e.message}")
            }
    }

    private fun listenForOpponent() {
        lobbyListener?.remove()
        lobbyListener = db.collection("lobbies").document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val status = snapshot.getString("status") ?: return@addSnapshotListener
                if (status == "ready") {
                    lobbyListener?.remove()
                    startMultiplayerGame()
                }
            }
    }

    private fun cancelCreatedGame() {
        lobbyListener?.remove()
        if (gameId.isNotBlank()) {
            db.collection("lobbies").document(gameId).delete()
            gameId = ""
        }
    }


    private fun joinGame(code: String) {
        localTeam = 1
        btnJoinConfirm.isEnabled = false
        tvWaitingStatus.text = "Buscando partida..."

        db.collection("lobbies").document(code).get()
            .addOnSuccessListener { snapshot ->
                btnJoinConfirm.isEnabled = true
                if (!snapshot.exists()) {
                    showToast("Código no encontrado")
                    return@addOnSuccessListener
                }
                val status = snapshot.getString("status")
                if (status != "waiting") {
                    showToast("La partida ya ha comenzado o no está disponible")
                    return@addOnSuccessListener
                }
                gameId = code
                selectedMapId = (snapshot.getLong("selectedMapId") ?: 1).toInt()


                db.collection("lobbies").document(gameId)
                    .update("status", "ready")
                    .addOnSuccessListener {
                        showWaiting()
                        tvWaitingStatus.text = "Conectando..."
                        progressWaiting.visibility = View.VISIBLE

                        view?.postDelayed({ startMultiplayerGame() }, 1200)
                    }
                    .addOnFailureListener { e ->
                        showToast("Error al unirse: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                btnJoinConfirm.isEnabled = true
                showToast("Error al buscar partida: ${e.message}")
            }
    }


    private fun startMultiplayerGame() {

        val gameFragment = GameFragment.newInstance(
            mapId       = selectedMapId,
            aiMode      = false,
            multiplayer = true,
            localTeam   = localTeam,
            gameId      = gameId
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .commit()
    }



    private fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun showToast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}