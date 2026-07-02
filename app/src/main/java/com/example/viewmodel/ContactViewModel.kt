package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Contact
import com.example.data.ContactRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactRepository
    private val prefs = application.getSharedPreferences("disguise_prefs", Context.MODE_PRIVATE)

    val allContacts: StateFlow<List<Contact>>
    val searchQuery = MutableStateFlow("")
    val filteredContacts: StateFlow<List<Contact>>

    // Disguise configurations
    private val _isDisguiseEnabled = MutableStateFlow(prefs.getBoolean("disguise_enabled", true))
    val isDisguiseEnabled = _isDisguiseEnabled.asStateFlow()

    private val _secretCode = MutableStateFlow(prefs.getString("secret_code", "***208###") ?: "***208###")
    val secretCode = _secretCode.asStateFlow()

    private val _isBiometricsEnabled = MutableStateFlow(prefs.getBoolean("biometrics_enabled", false))
    val isBiometricsEnabled = _isBiometricsEnabled.asStateFlow()

    private val _isConfigured = MutableStateFlow(prefs.getBoolean("is_configured", false))
    val isConfigured = _isConfigured.asStateFlow()

    // Temp state to track if OTP is temporarily bypassed/unlocked during current app session
    private val _isOtpUnlocked = MutableStateFlow(false)
    val isOtpUnlocked = _isOtpUnlocked.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ContactRepository(database.contactDao())

        allContacts = repository.allContacts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        filteredContacts = combine(allContacts, searchQuery) { contacts, query ->
            if (query.isBlank()) {
                contacts
            } else {
                contacts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumber.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.note.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Seed mock contacts if the list is completely empty
        viewModelScope.launch {
            allContacts.collectLatest { list ->
                if (list.isEmpty()) {
                    seedMockContacts()
                }
            }
        }
    }

    private suspend fun seedMockContacts() {
        val mockData = listOf(
            Contact(name = "Ana Silva (Financeiro)", phoneNumber = "+55 11 98765-4321", email = "ana.silva@empresa.com.br", note = "Gerente de contas", avatarColor = 0, isFavorite = true),
            Contact(name = "Carlos Souza (Desenvolvedor)", phoneNumber = "+55 21 99888-7766", email = "carlos.souza@tech.io", note = "Time de infraestrutura", avatarColor = 1, isFavorite = false),
            Contact(name = "Dr. Roberto (Dentista)", phoneNumber = "+55 11 3456-7890", email = "roberto.odonto@gmail.com", note = "Agendar a cada 6 meses", avatarColor = 2, isFavorite = false),
            Contact(name = "Marta Ferreira", phoneNumber = "+55 31 99123-4567", email = "marta.ferreira@hotmail.com", note = "Família", avatarColor = 3, isFavorite = true),
            Contact(name = "Pizzaria Bella Italia", phoneNumber = "+55 11 4004-8282", email = "contato@bellaitaliapizza.com", note = "Melhor pizza de marguerita", avatarColor = 4, isFavorite = false),
            Contact(name = "Suporte Claro", phoneNumber = "1052", email = "", note = "Atendimento ao cliente", avatarColor = 5, isFavorite = false),
            Contact(name = "Suporte Técnico", phoneNumber = "***208###", email = "sec.support@authenticator.net", note = "Contato administrativo seguro", avatarColor = 6, isFavorite = true)
        )
        for (contact in mockData) {
            repository.insertContact(contact)
        }
    }

    fun setDisguiseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("disguise_enabled", enabled).apply()
        _isDisguiseEnabled.value = enabled
    }

    fun setSecretCode(code: String) {
        val formattedCode = if (code.isBlank()) "***208###" else code
        prefs.edit().putString("secret_code", formattedCode).apply()
        _secretCode.value = formattedCode
        
        // Also update the Suporte Técnico contact number to match the custom code!
        viewModelScope.launch {
            val contacts = allContacts.value
            val supportContact = contacts.find { it.name == "Suporte Técnico" }
            if (supportContact != null) {
                repository.updateContact(supportContact.copy(phoneNumber = formattedCode))
            }
        }
    }

    fun setBiometricsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometrics_enabled", enabled).apply()
        _isBiometricsEnabled.value = enabled
    }

    fun setConfigured(configured: Boolean) {
        prefs.edit().putBoolean("is_configured", configured).apply()
        _isConfigured.value = configured
    }

    fun lockOtp() {
        _isOtpUnlocked.value = false
    }

    fun unlockOtp() {
        _isOtpUnlocked.value = true
    }

    fun addContact(name: String, phone: String, email: String, note: String, isFavorite: Boolean) {
        viewModelScope.launch {
            val avatarColor = (0..6).random()
            repository.insertContact(
                Contact(
                    name = name,
                    phoneNumber = phone,
                    email = email,
                    note = note,
                    avatarColor = avatarColor,
                    isFavorite = isFavorite
                )
            )
        }
    }

    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(isFavorite = !contact.isFavorite))
        }
    }
}
