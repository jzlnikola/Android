package elfak.mosis.medspot.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.R
import elfak.mosis.medspot.models.LocationViewModel

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class RatingDialog(
    private val markerId: String
) : DialogFragment() {

    private val locationViewModel: LocationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rating_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val ratingBar = view.findViewById<RatingBar>(R.id.ratingBar)
        val submit = view.findViewById<Button>(R.id.submitRatingButton)

        submit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            submitRating(markerId, rating)   // ← ova tvoja funkcija
        }
    }

    private fun submitRating(markerId: String, value: Int) {
        val userId = Firebase.auth.currentUser!!.uid
        val markerRef = Firebase.firestore.collection("markers").document(markerId)
        val ratingRef = markerRef.collection("ratings").document(userId)

        Firebase.firestore.runTransaction { tx ->

            val markerSnap = tx.get(markerRef)
            val oldAvg = markerSnap.getDouble("rating") ?: 0.0
            val oldCount = markerSnap.getLong("ratingCount")?.toInt() ?: 0

            val ratingSnap = tx.get(ratingRef)
            val oldUserRating = ratingSnap.getLong("value")?.toInt()

            val newAvg: Double
            val newCount: Int

            if (oldUserRating == null) {
                newCount = oldCount + 1
                newAvg = ((oldAvg * oldCount) + value) / newCount

            } else {
                newCount = oldCount
                val totalWithoutOld = oldAvg * oldCount - oldUserRating
                val totalWithNew = totalWithoutOld + value
                newAvg = totalWithNew / newCount
            }

            tx.set(ratingRef, mapOf("value" to value))

            tx.update(markerRef, mapOf(
                "rating" to newAvg,
                "ratingCount" to newCount
            ))
        }.addOnSuccessListener {
            context?.let {
                Toast.makeText(it, "Ocena sačuvana!", Toast.LENGTH_SHORT).show()
                locationViewModel.setItemRating(markerId)
                dismiss()
            }
        }
            .addOnFailureListener {
                context?.let {
                    Toast.makeText(it, "Greška!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
    }
}