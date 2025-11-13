package elfak.mosis.medspot.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import elfak.mosis.medspot.R
import elfak.mosis.medspot.activities.MainActivity
import elfak.mosis.medspot.databinding.FragmentLoginBinding
import elfak.mosis.medspot.helpers.ActionState
import elfak.mosis.medspot.models.LoginAndSignupViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class LoginFragment : Fragment() {

    private val userViewModel: LoginAndSignupViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.userViewModel = userViewModel

        binding.buttonLogin.setOnClickListener{userViewModel.login()}
        binding.buttonSignup.setOnClickListener{findNavController().navigate(R.id.action_LoginFragment_to_SignupFragment)}

        checkActionState()
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