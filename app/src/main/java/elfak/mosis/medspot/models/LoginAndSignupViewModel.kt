package elfak.mosis.medspot.models

import android.graphics.Bitmap
import android.text.Editable
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import elfak.mosis.medspot.helpers.ActionState
import elfak.mosis.medspot.models.data.Item
import elfak.mosis.medspot.models.data.User
import java.io.ByteArrayOutputStream

class LoginAndSignupViewModel : ViewModel() {

    private var auth : FirebaseAuth = Firebase.auth

    private val _image = MutableLiveData<Bitmap>()
    val image: LiveData<Bitmap> = _image

    private val _firstname = MutableLiveData<String>()
    val firstname : LiveData<String> = _firstname

    private val _lastname = MutableLiveData<String>()
    val lastname : LiveData<String> = _lastname

    private val _username = MutableLiveData<String>()
    val username : LiveData<String> = _username

    private val _password = MutableLiveData<String>()
    val password : LiveData<String> = _password

    private val _phone = MutableLiveData<String>()
    val phone : LiveData<String> = _phone

    private val _actionState = MutableLiveData<ActionState>(ActionState.Idle)
    val actionState: LiveData<ActionState> = _actionState

    fun setImage(image: Bitmap){
        _image.value = image
    }

    fun onFirstNameTextChanged(fName: Editable?){
        _firstname.value = fName.toString()
    }

    fun onLastNameTextChanged(lName: Editable?){
        _lastname.value = lName.toString()
    }

    fun onUsernameTextChanged(username: Editable?){
        _username.value = username.toString()
    }

    fun onPasswordTextChanged(pass: Editable?){
        _password.value = pass.toString()
    }

    fun onPhoneTextChanged(phone: Editable?){
        _phone.value = phone.toString()
    }

    fun createAccount(){
        if(checkInfo(false)){
            val username = "${username.value}"+"@gmail.com"
            auth.createUserWithEmailAndPassword(username, password.value!!)
                .addOnCompleteListener() { task ->
                    if(task.isSuccessful){
                        uploadInfo()
                    }
                    else{
                        val errorMessage = task.exception?.message ?: "Unknown error occurred."
                        _actionState.value = ActionState.ActionError(errorMessage)
                        Log.e("SignupError", "Failed to create user", task.exception)
                    }
                }
        }
    }

    fun login(){
        if(checkInfo(true)){
            val username = "${username.value}"+"@gmail.com"
            auth.signInWithEmailAndPassword(username, password.value!!)
                .addOnCompleteListener() { task ->
                    if(task.isSuccessful){
                        _actionState.value = ActionState.Success
                    }
                    else{
                        val errorMessage = task.exception?.message ?: "Unknown error occurred."
                        _actionState.value = ActionState.ActionError(errorMessage)
                        Log.e("SignupError", "Failed to create user", task.exception)
                    }
                }
        }
    }

    private fun uploadInfo(){
        val userID: String = auth.currentUser?.uid ?: ""
        if(userID == ""){
            Log.w("UerID", "Error!")
        }

        val baos = ByteArrayOutputStream()
        val bitmap = image.value

        if (bitmap != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()
            val base64Image = Base64.encodeToString(data, Base64.DEFAULT)

            val user = User(username.value, password.value, firstname.value, lastname.value, phone.value, base64Image, 0)
            val database = Firebase.database("https://nijo-medspot-id-default-rtdb.europe-west1.firebasedatabase.app/")
            database.reference.child("users").child(userID).setValue(user)
        }
    }

    private fun checkInfo(login: Boolean): Boolean {
        var checked = true

        if(login){
            if(username.value == null || username.value == ""){
                _actionState.value = ActionState.ActionError("Enter Username!")
                checked = false
            }
            else if(password.value == null || password.value == ""){
                _actionState.value = ActionState.ActionError("Enter Password!")
                checked = false
            }
        }

        else{
            if(firstname.value == null || firstname.value == ""){
                _actionState.value = ActionState.ActionError("Enter First Name!")
                checked = false
            }
            else if(lastname.value == null || lastname.value == ""){
                _actionState.value = ActionState.ActionError("Enter Last Name!")
                checked = false
            }
            else if(phone.value == null || phone.value == ""){
                _actionState.value = ActionState.ActionError("Enter Phone Number!")
                checked = false
            }
            else if(username.value == null || username.value == ""){
                _actionState.value = ActionState.ActionError("Enter Username!")
                checked = false
            }
            else if(password.value == null || password.value == ""){
                _actionState.value = ActionState.ActionError("Enter Password!")
                checked = false
            }
            else if(image.value == null){
                _actionState.value = ActionState.ActionError("Upload Image!")
                checked = false
            }
        }

        if(checked && !login){
            _actionState.value = ActionState.Success
        }
        return checked
    }
}