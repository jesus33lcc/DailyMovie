package com.example.dailymovie.fragments

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.dailymovie.R
import com.example.dailymovie.databinding.FragmentSettingsBinding
import com.example.dailymovie.views.LoginA
import java.util.Locale

class Settings : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        settingsViewModel = ViewModelProvider(this).get(SettingsViewModel::class.java)

        val (languageCode, countryCode) = getUserLocale()
        binding.btnRegion.text = "Región: $countryCode, Idioma: $languageCode"

        binding.btnClearHistory.setOnClickListener {
            showConfirmationDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnRegion.setOnClickListener {
            // Acción para cambiar la región
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar borrado")
        builder.setMessage("¿Estás seguro de que deseas borrar todo el historial?")
        builder.setPositiveButton("Sí") { dialog, which ->
            settingsViewModel.clearHistory { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Historial borrado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al borrar el historial", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null)
        val currentPasswordEditText = dialogView.findViewById<EditText>(R.id.editTextCurrentPassword)
        val newPasswordEditText = dialogView.findViewById<EditText>(R.id.editTextNewPassword)
        val confirmPasswordEditText = dialogView.findViewById<EditText>(R.id.editTextConfirmPassword)

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cambiar Contraseña")
        builder.setView(dialogView)
        builder.setPositiveButton("Cambiar") { dialog, which ->
            val currentPassword = currentPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (currentPassword.isNotEmpty() && newPassword.isNotEmpty() && newPassword == confirmPassword) {
                settingsViewModel.changePassword(currentPassword, newPassword) { success, message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Las contraseñas no coinciden o están vacías", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Confirmar cierre de sesión")
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?")
        builder.setPositiveButton("Sí") { dialog, which ->
            settingsViewModel.logoutUser { success ->
                if (success) {
                    Toast.makeText(requireContext(), "Sesión cerrada", Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                } else {
                    Toast.makeText(requireContext(), "Error al cerrar la sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginA::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getUserLocale(): Pair<String, String> {
        val locale = Locale.getDefault()
        val languageCode = locale.language
        val countryCode = locale.country
        return Pair(languageCode, countryCode)
    }
}
