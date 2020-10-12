package net.ivpn.client.v2.signup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import net.ivpn.client.IVPNApplication
import net.ivpn.client.R
import net.ivpn.client.common.utils.ToastUtil
import net.ivpn.client.databinding.FragmentSignUpFinishBinding
import net.ivpn.client.v2.viewmodel.SignUpViewModel
import org.slf4j.LoggerFactory
import javax.inject.Inject


class SignUpAccountCreatedFragment : Fragment() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SignUpAccountCreatedFragment::class.java)
    }

    lateinit var binding: FragmentSignUpFinishBinding

    @Inject
    lateinit var viewModel: SignUpViewModel

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_sign_up_finish, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        IVPNApplication.getApplication().appComponent.provideActivityComponent().create().inject(this)
        initViews()
        initToolbar()
    }

    private fun initViews() {
        binding.contentLayout.viewmodel = viewModel

        binding.contentLayout.continueButton.setOnClickListener {
            continuePurchase()
        }
        binding.contentLayout.copyBtn.setOnClickListener {
            copyUserId()
        }
    }

    private fun initToolbar() {
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
    }

    private fun copyUserId(){
        viewModel.blankAccountID.get()?.let { userId ->
            val myClipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val myClip: ClipData = ClipData.newPlainText("User Id", userId)
            myClipboard.setPrimaryClip(myClip)

            ToastUtil.toast(R.string.account_clipboard)
        }
    }

    private fun continuePurchase() {
        val action = SignUpAccountCreatedFragmentDirections.actionSignUpFinishFragmentToSignUpFragment()
        NavHostFragment.findNavController(this).navigate(action)
    }
}