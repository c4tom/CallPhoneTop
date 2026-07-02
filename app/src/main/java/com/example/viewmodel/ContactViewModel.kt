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
            Contact(name = "Jane Cooper", phoneNumber = "+1 (555) 0123-4567", email = "jane.cooper@example.com", note = "Verified Profile ✓ Identified by CallNix Security", avatarColor = 0, isFavorite = true),
            Contact(name = "Aaron Smith", phoneNumber = "+1 (555) 000-1234", email = "aaron.smith@gmail.com", note = "Mobile • Amigo", avatarColor = 1, isFavorite = false),
            Contact(name = "Alice Brown", phoneNumber = "+1 (555) 000-5678", email = "alice.brown@work.com", note = "Work • Verified Contact", avatarColor = 2, isFavorite = true),
            Contact(name = "Bella Thorne", phoneNumber = "+1 (555) 111-2233", email = "bella.t@music.com", note = "Mobile • Faculdade", avatarColor = 3, isFavorite = false),
            Contact(name = "Caleb Rivers", phoneNumber = "+1 (555) 444-5566", email = "caleb.r@home.com", note = "Home • Família", avatarColor = 4, isFavorite = false),
            Contact(name = "Diana Prince", phoneNumber = "+1 (555) 999-0011", email = "diana@justice.org", note = "Work • Verified Contact", avatarColor = 5, isFavorite = true),
            Contact(name = "Mom", phoneNumber = "+1 (555) 888-9999", email = "mom@family.com", note = "Last called 2h ago • Mãe", avatarColor = 6, isFavorite = true),
            Contact(name = "Dad", phoneNumber = "+1 (555) 777-6666", email = "dad@family.com", note = "Home • Mobile • Pai", avatarColor = 7, isFavorite = true),
            Contact(name = "Sarah Jenkins", phoneNumber = "+1 (555) 222-3333", email = "sarah.j@work.com", note = "Work • Manager", avatarColor = 8, isFavorite = true),
            Contact(name = "Mike Ross", phoneNumber = "+1 (555) 444-3333", email = "mike.ross@pearson.com", note = "Personal • Online", avatarColor = 9, isFavorite = true),
            Contact(name = "Pizzaria Bella Italia", phoneNumber = "+55 11 4004-8282", email = "contato@bellaitaliapizza.com", note = "Melhor pizza de marguerita (Verified: Pizza Shop)", avatarColor = 10, isFavorite = false),
            Contact(name = "Suporte Técnico", phoneNumber = "***208###", email = "sec.support@authenticator.net", note = "Contato administrativo seguro", avatarColor = 11, isFavorite = true)
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
