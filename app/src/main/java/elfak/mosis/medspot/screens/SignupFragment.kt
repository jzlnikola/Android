package elfak.mosis.medspot.screens

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import elfak.mosis.medspot.R
import elfak.mosis.medspot.activities.MainActivity
import elfak.mosis.medspot.databinding.FragmentSignupBinding
import elfak.mosis.medspot.helpers.ActionState
import elfak.mosis.medspot.models.LoginAndSignupViewModel

class SignupFragment : Fragment() {

    private val userViewModel: LoginAndSignupViewModel by viewModels()
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    var SELECT_PICTURE: Int = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userViewModel = userViewModel

        binding.buttonSignup.setOnClickListener{userViewModel.createAccount()}
        binding.buttonLogin.setOnClickListener{findNavController().navigate(R.id.action_SignupFragment_to_LoginFragment)}
        binding.buttonAddpicture.setOnClickListener{chooseImage()}

        checkActionState()
    }

    private fun chooseImage(){
        val i: Intent = Intent()
        i.setType("image/*")
        i.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(i, "Add picture"), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            if(requestCode == SELECT_PICTURE){
                val selectedImageUri: Uri = data?.getData()!!
                binding.uploadImage.setImageURI(selectedImageUri)
                binding.uploadImage.isDrawingCacheEnabled = true
                binding.uploadImage.buildDrawingCache()
                val bitmap: Bitmap = (binding.uploadImage.drawable as BitmapDrawable).bitmap
                userViewModel.setImage(bitmap)
            }
        }
    }

    private fun checkActionState(){
        val actionState = Observer<ActionState> { state ->
            if(state == ActionState.Success){
                val i: Intent = Intent(activity, MainActivity::class.java)
                startActivity(i)
                activity?.finish()
            }
            else if(state is ActionState.ActionError){
                Toast.makeText(view?.context, state.message, Toast.LENGTH_SHORT).show()
            }
        }
        userViewModel.actionState.observe(viewLifecycleOwner, actionState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}