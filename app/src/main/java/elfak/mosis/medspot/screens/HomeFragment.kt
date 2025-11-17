package elfak.mosis.medspot.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import elfak.mosis.medspot.models.data.User
import android.widget.Button
import androidx.navigation.fragment.findNavController

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class HomeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        val welcomeText: TextView = requireView().findViewById(R.id.welcome_textView)
        val priorityText: TextView = requireView().findViewById(R.id.points_textView)


        if(auth.currentUser?.email?.isNullOrBlank() == false) {
            welcomeText.text = " Welcome \n" + auth.currentUser?.email?.substringBefore("@")
        }
        else
        {
            welcomeText.text = "Welcome"
        }

        val database = Firebase.database("https://nijo-medspot-id-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference.child("users").child(auth.currentUser?.uid.toString())

        val userListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue<User>()
                if(user != null) {
                    val pTxt = "Priority: " + user?.points.toString()
                    priorityText.text = pTxt
                }
                else
                {
                    priorityText.text = ""
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        database.addValueEventListener(userListener)

        val mapButton = view.findViewById<Button>(R.id.button)
        mapButton.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_MapFragment)
        }
    }
}