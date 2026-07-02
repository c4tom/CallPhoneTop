package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Contact
import com.example.viewmodel.ContactViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Realistic photos mapping helper
fun getAvatarUrlForName(name: String): String? {
    val clean = name.lowercase().trim()
    return when {
        clean.contains("jane") || clean.contains("cooper") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuB3DDmtAWmPgHDlqg7rKnGYCyizfvUqpDlDHLMMqxekcHp_6G8dr9JDHrlKZ_CPCl7cXSgqHEKy72gXGwmET65exLnQzBH3B47oZ6Y9ZVQTmPBywfeuaHhtQPPSx20yW02eaHDt7Wr69oiYw5SQv88NdjaQ0n5oecENdqtXuxb5ZuNmw9VK_8yh-Xhn8ngCqBSpQ_dO-qP-867-ZVYM0MmUlAppN5viGGKf3ZoSe8JuVBNNkZAMhLuGCeEhMIwdqD6w8Uq88ThA5DM"
        clean.contains("mom") || clean.contains("mãe") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDmwDmP9W1zMfrD25kYuq4XSb4cmUQD2oRkML_V3vhRDVK9VTXxwD8Y8Xrp3uZ0vU8rraDHL7MutRmJHEfbCN2vRwk_P_0MBmVErB0B6UeMe9tFWGURcGDT6JplDFt4pIOAqtFhw7xoUGauWgAVWo1BYEWtWjXEKu1ZalEqopVzSaj8mfPbOzsiZYyBNvYMq2UaBzJZf1P9pG0fkzH9ekIke13Y6RTsEulS0A2hIzcUjieG06FJIf09Eellf75H3Zgh9LaOzqZLm24"
        clean.contains("dad") || clean.contains("pai") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCzMIBYdPRYjMTHV1l-hSdC3QfsoraHCj-vOKcy9nMTxGp0jl415WW0x_Be6fgushx4aBDUQos3cNGXaHTulUP7UccL1jgZN7VcNgs2O3bOTyKqvlhpfkV4OdhSItSSPqQSO_TmbAVzSsDpwaX2QPqmtG2iv9Yrz-TskoaChwo0eaHSlEu3NMLLdupbFv3ktmlboMvnJicE3scihhCpcOv4vhcGgszDD9lQ2LWyAeBUolAveahjq6F4-01ejKcd4uGF4Uh94IcajmQ"
        clean.contains("sarah jenkins") || clean.contains("jenkins") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuB__XLJgcvkn480l7NFOHp6Aa42qONcEHeTonXU2RORI6AR7PJd1KY4IXkgH-yo1Itjco1Dg1po8twFYxco5xBT2esR5pwEBF7k8N0cHBtUMwQHsoH8RaxeRQz1Zx1HtkUSpW8xw4RVlwbpyPNywpMQjNSbHpmDEp12OSCR9btYya_uTIVeuGomNErPj_WyMXWj6t1QyTgVUQq0POM7qiqxFdpA7u25fUdemDhFpAfpXXqNZrGMw0RIg3MzXg419Wz3FnqvdxFcD0Q"
        clean.contains("mike") || clean.contains("ross") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCW5Qp--CTVEtT4Iw4SqlGV2cttH5FWbydvZtZRp2hmtuOH_01pLC9k5w0m-frcRrODv6GUe_YvdzTSUma8t1NAmNuJ8qC6Eje2QRPrTs-WLAiiZfxFBfgAB30L2jqjCW1_04sXPbio0IPcsWWrf-uQcNsAsqE3CKK--JeisLJ7OwjuuDOZq9rhyn2hEFNv8-6lQYS7QQeNp5ci4y3V0faqW9w2C_KJcju14wRz-n2yZc8NzrRXTI8W-POXykeaVq5EQpYwF6sAf-c"
        clean.contains("alex") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuBPqjZsHiDTHga_2TE4cuoImvq3H1WAM__mLyaoecHAMmlrIpdk_Nh_91dUfkVLvCJv4YOToeuaxYjeSt43_XtlOSELb0rQo0uU7dgetSSNpLL7FYDBxJdYCqJdJV8qIVeHj02G51JXyM8yIgo9mMFGG1XzMqgQsCavWcCE6VouRA6AiBYA5XHEFMtgjlcDKdlEsunwrzw1Hn9HmPF1gwEN97fjeWSoA8pRvGBg3K0ts2WacoZeVDcybEkqe--EJ9uUaDonc3kjNWQ"
        clean.contains("sarah") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuB98sXagllep4brC9zC9lFA0XTgZfjhWcNVH23VZDN7G_TXUy9gFaCn_Da9OFaAgvCLSu2aAfHvKKoBigL-_vDtPGy6ImMDPtb042WBk10d03-UpYXAzuyrKds4v4RCVa8uIcSBGzntfoTSlfQPqIFwV39hoMOOrwDyeDrDiJ5V3qglNl4FICG88PpIvnFACLv_4B8Yn1bVifHwNLhrOut967_4c-s6HHU5d8oSSxOcbSlhRL9XPr_qMs7RKKMk1lm46LxpUFG-a_o"
        clean.contains("david") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuAwaNOEouHaBbUcyx8ONlQKJLVh2pnR9QfN61C2YiSWHrvDlZWdPUmmy_8r7WoOm4gwnDeh-2yQrIpKl8w6_DYdGpX6XCnmKNam4dvSLNvy3ArMw9QoCm7DH03VTYcy3xQ0Tc7UNRID7QlaJuQJTLwF0gw2wr29fUDknPn5hmJ_1KzYAO-JhH0FA7qTJKqeqawe-Er2JIyoVejF_4AqWHMefAuaJSJZiv5nxlqtZhNwA9d5lw8pPkCqUKXNGt4g_ci-lFEbPwqAUus"
        clean.contains("elena") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDhYm6vRm0ClnhjqsrFlcPjSY8P0ftkLOHFEqZEQJSKBfmBeo5d6c-rISbjZOeb7mVWeYBg64fvZ34_ng6LiQ-WogHDF7UJ64XwU8gQBQsXw_w6A0QhzvwSQx2yFO9_K2899ppRVTjX5PmVLIVtekDqyaheGEoBSFDyk-m3TQojm0YpZ4Zmb6WSQqHR1iczq7oqPqaq8UY31vMrQ6exVNssW7Wj6yOPvaTrOazqxe3Ym2-gaTQflk4pc8tqfgQTJ5KUQ9lh0Oou3gA"
        clean.contains("aaron") || clean.contains("smith") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDvPBPZuvr3Tf9C0R8KV0CbQ_NwB6lfhAQGSR9WKjzc5376xgB2vjdPfsiWSiR4OzkBRZ-6yORlXKIotSOpsiNY2UQX-egOsGCAForz9_tGOSOcV8-d5Yb2Z1Afw7IPKugv6ScY1jtBflowwN9DI4zjOPFImChIi7vWTwBTPl7yYXeTTPtqoI7b71a4N48Qe_wsSky40RlafuX0hXZaE_1RQy-SfmsRMQWIWsq8lAf8cqgJaHPg62K3KIZ8gTvcPG9EZFHufzyfV8w"
        clean.contains("alice") || clean.contains("brown") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuAmnSjk7xgxgtqYekIbp9cD5DJi-JbSFpaerrNKU4JCAlZ3Vwaf-otWmwrHU4Mg66yOoo0wzeLeKDf7QDJLX3cL3iI-DL1WhEx-ab6DPHFJSyesheqETt4oxYC4XhcPudaRXQr1UVeJ8jGCUrorDBI8IgioIoBFcjS77L97Nzmi5qhumOhgud4tl97hzeFZ6zLENxLwJVwVZ0d6wvbDhw97Im9PGgX2JPiLXcHAPx28HZNzgiGEcBJ2sA_WOVujXUYATk_MDzrcV0U"
        clean.contains("bella") || clean.contains("thorne") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDpNbDczSCuRocvZZYXzozrl1-N8QA4XUETkKfsQ9YO2uW54XvaDpNR3ZHJGPPhWLGefTYlKTFWgH-AWoSryqep7xZFMPtdAggSqFJHz2netZQjta84IQn-yis8USf_xnhwljUmWC1V0qMXIHuzz-p13FBY73Nytbj3_gf3BoiSHqBxzLCCds0USSS5qHfyuixBakeUq0mH2hVyGCE8_BE_HDd6fPJsqbepKZilYjGDkgqoTunsv8NRbV1buCLmWdv8NKNrKPZfHmU"
        clean.contains("caleb") || clean.contains("rivers") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuBBy2Z6xWaxWQol_ldbCvCB9T71DHSDNxYtrqFTZcAKq7VgS07SjKoF7HqLaI-r0ME6-CZ5fEJ3ZD3ZnCBqnTelQ90kMrg-FPGABNY2NHTxD7DhEtsVC_gY4wE1bL7O45pf7h17JX1kgHHL58lg8_XnrPwBN4k2-9Ag6MzXTs0zu1gS0uxKDxxbijq21VU6Pd2b9rMwcctB5q2Oz-rCHNrKnOS16JFUIqVaB28RMbGuMIBF_KVQDO5XazInxHYbTezpWjb7PMxbEpA"
        clean.contains("diana") || clean.contains("prince") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuANEs3G-S8YmiOkGj3WOldMK2kKhL-gfUTMpK4yHPH0hkN8OSrq0s-5fUIO4uRwXRBnTPJ9T5ZKbDCTDjF7u6VlRrG1u2R6V-ZfN-Irvgs8WWZPUCgpLUiNY04--zC7yA5C-B8tiXt6wGVt8x3mNLVuD9C338rIx2NT6U9CIGru1xiyHjqM8mmBhm_R0WWCPGV0GRrqnU74BAWZZPnLLnJNvUxE1dHEdQtHZkJGXo2AdKqkWigOQ-En2PkqfOZqueOe3AtlALxKDQ0"
        clean.contains("james") || clean.contains("wilson") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuDOV6rKcwm3tRhL1RIx46s-k5w0JmyMSVfGNSaMpoQFzbju5EIDnCN2HmDMJNdFXrRj7UxmIA-ziKQR3xEujrFKj6-QzCdSa8A7UzbUOk2aM8tjJu_zO5sdaj3BngPM9T-yR3rade5eMC8ohNF5AzYbVYZBmh27seo5qBaA9nhGm7-C5BzT0pMrDDxXxZbBlmyUJ2OV03zAX_13E95I_KCy5kggTiS90H3ViqKjPIxJhKYlxLJvlkbnvVzLNV2DEcXdyYdbr1qxwlA"
        clean.contains("emily") || clean.contains("blunt") -> "https://lh3.googleusercontent.com/aida-public/AB6AXuAsBytd1xXlJTU3jHQmFzM8yeLdzh89RJHhJyKVtgWPf1dQ1kjHTMgLhO1yZOFMIanZA_os1vLemLAmcjVuq-OMgtKjHPszgDeEogHkMdvSQddSeU0GbZCNiQb1CXryUZJsagPJr7vOT47_XBLEtnLiPjH23wywzTm5eHEz5--upAHxRVuWYN6skSXJuZ_UTvlfMSnwNtU7JtgUtyrKCFzlvh-juUZq5cRw8ZDDvAFmCjQm4N8wJRaxwObcnpuREkNvrDJeDfB4dV0"
        else -> null
    }
}

// Verification Badge Check
fun isVerifiedContact(name: String): Boolean {
    val clean = name.lowercase().trim()
    return clean.contains("jane") || clean.contains("alice") || clean.contains("diana") || clean.contains("suporte") || clean.contains("mom") || clean.contains("mãe") || clean.contains("dad") || clean.contains("pai") || clean.contains("sarah jenkins")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseView(
    contactViewModel: ContactViewModel,
    onUnlockApp: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(1) } // Default to 1: Keypad, 0: Recents, 2: Contacts, 3: Safety
    var dialNumber by remember { mutableStateOf("") }
    val searchQuery by contactViewModel.searchQuery.collectAsState()
    val filteredContacts by contactViewModel.filteredContacts.collectAsState()
    val isConfigured by contactViewModel.isConfigured.collectAsState()
    val secretCode by contactViewModel.secretCode.collectAsState()
    val isBiometricsEnabled by contactViewModel.isBiometricsEnabled.collectAsState()

    // Dialog & Onboarding States
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showContactDetails by remember { mutableStateOf<Contact?>(null) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showBiometricAuthSheet by remember { mutableStateOf(false) }
    var simulatedCallContact by remember { mutableStateOf<Contact?>(null) }
    var simulatedCallNumber by remember { mutableStateOf<String?>(null) }

    val handleCallAction = { number: String ->
        val trimmedNum = number.replace(" ", "").trim()
        val targetSecret = secretCode.replace(" ", "").trim()

        if ((trimmedNum == "***208###" || trimmedNum == "##208##" || trimmedNum == targetSecret) && !isConfigured) {
            showOnboarding = true
        } else if (trimmedNum == targetSecret) {
            if (isBiometricsEnabled) {
                showBiometricAuthSheet = true
            } else {
                contactViewModel.unlockOtp()
                onUnlockApp()
                Toast.makeText(context, "Cofre desbloqueado!", Toast.LENGTH_SHORT).show()
            }
            dialNumber = ""
        } else {
            simulatedCallNumber = number
            val associatedContact = filteredContacts.find { it.phoneNumber.replace(" ", "") == trimmedNum }
            simulatedCallContact = associatedContact
            dialNumber = ""
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.shadow(16.dp)
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "Recentes") },
                    label = { Text("Recentes", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = "Teclado") },
                    label = { Text("Teclado", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contatos") },
                    label = { Text("Contatos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Segurança") },
                    label = { Text("Segurança", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> {
                    RecentsScreenContent(
                        onContactClick = { showContactDetails = it },
                        onDialNumber = { handleCallAction(it) }
                    )
                }
                1 -> {
                    DialerScreenContent(
                        dialNumber = dialNumber,
                        onNumberChange = { dialNumber = it },
                        onCall = { handleCallAction(dialNumber) },
                        onContactClick = { showContactDetails = it },
                        contacts = filteredContacts
                    )
                }
                2 -> {
                    ContactsScreenContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { contactViewModel.searchQuery.value = it },
                        contacts = filteredContacts,
                        onContactClick = { showContactDetails = it },
                        onToggleFavorite = { contactViewModel.toggleFavorite(it) },
                        onAddClick = { showAddContactDialog = true }
                    )
                }
                3 -> {
                    SafetyScreenContent(
                        onConfigureClick = { showOnboarding = true }
                    )
                }
            }

            // SIMULATED CALL OVERLAY
            if (simulatedCallNumber != null || simulatedCallContact != null) {
                SimulatedCallOverlay(
                    contact = simulatedCallContact,
                    number = simulatedCallNumber ?: simulatedCallContact?.phoneNumber ?: "",
                    onHangUp = {
                        simulatedCallNumber = null
                        simulatedCallContact = null
                    }
                )
            }

            // ADD CONTACT DIALOG
            if (showAddContactDialog) {
                AddContactDialog(
                    onDismiss = { showAddContactDialog = false },
                    onSave = { name, phone, email, note, isFavorite ->
                        contactViewModel.addContact(name, phone, email, note, isFavorite)
                        showAddContactDialog = false
                    }
                )
            }

            // FULL CONTACT DETAILS OVERLAY
            if (showContactDetails != null) {
                ContactDetailsDialog(
                    contact = showContactDetails!!,
                    onDismiss = { showContactDetails = null },
                    onCall = { contact ->
                        showContactDetails = null
                        handleCallAction(contact.phoneNumber)
                    },
                    onDelete = { contact ->
                        contactViewModel.deleteContact(contact)
                        showContactDetails = null
                        Toast.makeText(context, "Contato excluído", Toast.LENGTH_SHORT).show()
                    },
                    onEdit = { updatedContact ->
                        contactViewModel.updateContact(updatedContact)
                        showContactDetails = updatedContact
                    }
                )
            }

            // ONBOARDING SETUP DIALOG
            if (showOnboarding) {
                DisguiseSetupDialog(
                    onDismiss = { showOnboarding = false },
                    onComplete = { customCode, enableBio ->
                        contactViewModel.setSecretCode(customCode)
                        contactViewModel.setBiometricsEnabled(enableBio)
                        contactViewModel.setConfigured(true)
                        contactViewModel.unlockOtp()
                        showOnboarding = false
                        onUnlockApp()
                        Toast.makeText(context, "Configuração concluída! Cofre ativado.", Toast.LENGTH_LONG).show()
                    }
                )
            }

            // BIOMETRIC AUTH SHEET
            if (showBiometricAuthSheet) {
                BiometricAuthSheet(
                    onDismiss = { showBiometricAuthSheet = false },
                    onSuccess = {
                        showBiometricAuthSheet = false
                        contactViewModel.unlockOtp()
                        onUnlockApp()
                    }
                )
            }
        }
    }
}

private fun getMaskedDialNumber(dialNumber: String): String {
    var starCount = 0
    var thirdStarIndex = -1
    for (i in dialNumber.indices) {
        if (dialNumber[i] == '*') {
            starCount++
            if (starCount == 3) {
                thirdStarIndex = i
                break
            }
        }
    }
    return if (thirdStarIndex != -1) {
        dialNumber.mapIndexed { index, char ->
            if (index > thirdStarIndex) '*' else char
        }.joinToString("")
    } else {
        dialNumber
    }
}

// ---------------------- 1. RECENTS TAB (CALL HISTORY) ----------------------
@Composable
fun RecentsScreenContent(
    onContactClick: (Contact) -> Unit,
    onDialNumber: (String) -> Unit
) {
    val context = LocalContext.current
    var activeSubFilter by remember { mutableStateOf(0) } // 0: All, 1: Missed, 2: Blocked
    var whoscallEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Histórico",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Search, contentDescription = "Buscar")
            }
        }

        // Filter tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val filters = listOf("Todos", "Perdidos", "Bloqueados")
            filters.forEachIndexed { index, title ->
                Column(
                    modifier = Modifier
                        .clickable { activeSubFilter = index }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (activeSubFilter == index) FontWeight.Bold else FontWeight.Normal,
                        color = if (activeSubFilter == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .background(if (activeSubFilter == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }

        // Whoscall Identification Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ManageSearch, contentDescription = null, tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "Identificar números desconhecidos",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (whoscallEnabled) "Identificação automática Whoscall ativa" else "Auto-identificar chamadores fora da agenda",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Button(
                    onClick = { whoscallEnabled = !whoscallEnabled },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (whoscallEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(if (whoscallEnabled) "Ativo" else "Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Predefined high-fidelity call records
        val allCalls = listOf(
            RecentCall(
                name = "Jane Cooper",
                number = "+1 (555) 0123-4567",
                type = "missed",
                time = "10:45",
                isVerified = true,
                verificationLabel = "Verified Caller",
                contactId = 1
            ),
            RecentCall(
                name = "Robert Fox",
                number = "+1 (555) 093-0101",
                type = "received",
                time = "09:30",
                isVerified = false,
                contactId = 10
            ),
            RecentCall(
                name = "+1 555-0123-456",
                number = "+1 (555) 0123-456",
                type = "made",
                time = "Ontem, 16:20",
                isVerified = false,
                contactId = -1
            ),
            RecentCall(
                name = "Potencial Spam",
                number = "0303 555 4433",
                type = "blocked",
                time = "Segunda, 14:15",
                isVerified = false,
                isSpam = true,
                contactId = -1
            ),
            RecentCall(
                name = "Arlene McCoy",
                number = "+1 (555) 234-9876",
                type = "made",
                time = "Segunda, 11:05",
                isVerified = false,
                contactId = -1
            )
        )

        // Filter the mock logs based on tab selection
        val filteredCalls = when (activeSubFilter) {
            1 -> allCalls.filter { it.type == "missed" }
            2 -> allCalls.filter { it.type == "blocked" }
            else -> allCalls
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(filteredCalls) { call ->
                RecentCallRow(
                    call = call,
                    onRowClick = {
                        if (call.contactId != -1L) {
                            onContactClick(
                                Contact(
                                    id = call.contactId,
                                    name = call.name,
                                    phoneNumber = call.number,
                                    email = if (call.name == "Jane Cooper") "jane.cooper@example.com" else "robert.fox@gmail.com",
                                    note = "Identificado via Whoscall Database",
                                    isFavorite = true
                                )
                            )
                        } else {
                            Toast.makeText(context, "Número: ${call.number}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDial = { onDialNumber(call.number) }
                )
            }
        }
    }
}

data class RecentCall(
    val name: String,
    val number: String,
    val type: String, // missed, received, made, blocked
    val time: String,
    val isVerified: Boolean = false,
    val verificationLabel: String? = null,
    val isSpam: Boolean = false,
    val contactId: Long = -1
)

@Composable
fun RecentCallRow(
    call: RecentCall,
    onRowClick: () -> Unit,
    onDial: () -> Unit
) {
    val context = LocalContext.current
    val rowBgColor = if (call.isSpam) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBgColor)
            .clickable { onRowClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Icon indicators
            val iconTint = when (call.type) {
                "missed" -> MaterialTheme.colorScheme.error
                "received" -> Color(0xFF4CAF50)
                "made" -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            }
            val iconVector = when (call.type) {
                "missed" -> Icons.Default.CallMissed
                "received" -> Icons.Default.CallReceived
                "made" -> Icons.Default.CallMade
                else -> Icons.Default.Block
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconTint.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = call.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (call.type == "missed" || call.isSpam) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                    if (call.isVerified) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Verificado", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                    if (call.isSpam) {
                        Text(
                            text = "SPAM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (call.isSpam) "Aviso de Spam" else "Celular"} • ${call.time}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (call.type != "blocked") {
                IconButton(onClick = onDial) {
                    Icon(Icons.Default.Call, contentDescription = "Ligar", tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onRowClick) {
                Icon(Icons.Default.Info, contentDescription = "Informações", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}


// ---------------------- 2. KEYPAD TAB (DIALER SCREEN WITH FAVORITES) ----------------------
@Composable
fun DialerScreenContent(
    dialNumber: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit,
    onContactClick: (Contact) -> Unit,
    contacts: List<Contact>
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dialer top search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                .clickable { }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            Text("Buscar contatos ou números...", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
            Icon(Icons.Default.Mic, contentDescription = "Voz", tint = MaterialTheme.colorScheme.primary)
        }

        // Favorites Horizontal Grid
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Acesso Rápido", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = { }, contentPadding = PaddingValues(0.dp)) {
                    Text("Ver Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val favs = contacts.filter { it.isFavorite }
                items(favs) { contact ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { onContactClick(contact) }
                            .padding(vertical = 4.dp)
                    ) {
                        val pic = getAvatarUrlForName(contact.name)
                        if (pic != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(pic)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = contact.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = contact.name.split(" ").firstOrNull() ?: contact.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Adicionar", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Center dialed display with verified identification badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = getMaskedDialNumber(dialNumber),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )

                if (dialNumber.isNotEmpty()) {
                    // Check if dialing matches any custom spam or verified rules
                    val isPizzaShop = dialNumber.contains("4004") || dialNumber.contains("8282")
                    val isSecretCode = dialNumber == "***208###" || dialNumber == "##208##"

                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .background(
                                color = if (isSecretCode) MaterialTheme.colorScheme.primaryContainer else if (isPizzaShop) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSecretCode) Icons.Default.Lock else if (isPizzaShop) Icons.Default.VerifiedUser else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isSecretCode) MaterialTheme.colorScheme.primary else if (isPizzaShop) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isSecretCode) "Acesso Seguro Ativado" else if (isPizzaShop) "Bella Italia Pizza (Verificado)" else "Adicionar aos contatos",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSecretCode) MaterialTheme.colorScheme.onPrimaryContainer else if (isPizzaShop) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // T9 Keypad keys layout
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rows = listOf(
                listOf("1" to "", "2" to "A B C", "3" to "D E F"),
                listOf("4" to "G H I", "5" to "J K L", "6" to "M N O"),
                listOf("7" to "P Q R S", "8" to "T U V", "9" to "W X Y Z"),
                listOf("*" to "", "0" to "+", "#" to "")
            )

            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (item in row) {
                        DialKey(
                            digit = item.first,
                            subtext = item.second,
                            onClick = {
                                if (dialNumber.length < 20) {
                                    onNumberChange(dialNumber + item.first)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dialer actions row: Add, Block, Call, Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add contact action
                IconButton(
                    onClick = {
                        if (dialNumber.isNotEmpty()) {
                            onNumberChange(dialNumber)
                        }
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = MaterialTheme.colorScheme.outline)
                        Text("Adicionar", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // Block call action
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Número $dialNumber bloqueado!", Toast.LENGTH_SHORT).show()
                        onNumberChange("")
                    },
                    modifier = Modifier.size(52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Block, contentDescription = "Block", tint = MaterialTheme.colorScheme.error)
                        Text("Bloquear", fontSize = 8.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                // Call green button
                IconButton(
                    onClick = onCall,
                    enabled = dialNumber.isNotEmpty(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (dialNumber.isNotEmpty()) Color(0xFF2B8CEE) else Color(0xFF2B8CEE).copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Discar", modifier = Modifier.size(32.dp))
                }

                // Backspace button
                IconButton(
                    onClick = {
                        if (dialNumber.isNotEmpty()) {
                            onNumberChange(dialNumber.dropLast(1))
                        }
                    },
                    enabled = dialNumber.isNotEmpty(),
                    modifier = Modifier.size(52.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Limpar",
                            tint = if (dialNumber.isNotEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        Text("Apagar", fontSize = 8.sp, color = if (dialNumber.isNotEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}


// ---------------------- 3. CONTACTS TAB WITH VERIFIED BADGES & SYNC ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onAddClick: () -> Unit
) {
    var activeCategory by remember { mutableStateOf("Todos") }
    val categories = listOf("Todos", "Favoritos", "Trabalho", "Família", "Recentes")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header with sync button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Contatos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Novo", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Buscar ${contacts.size} contatos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // Horizontal Category chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val selected = cat == activeCategory
                FilterChip(
                    selected = selected,
                    onClick = { activeCategory = cat },
                    label = { Text(cat) },
                    shape = CircleShape
                )
            }
        }

        // Whoscall Database Sync Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sincronizar com Banco Whoscall", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // List & Alphabet scroller sidebar container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val displayList = when (activeCategory) {
                "Favoritos" -> contacts.filter { it.isFavorite }
                "Trabalho" -> contacts.filter { it.note.lowercase().contains("work") || it.note.lowercase().contains("trabalho") }
                "Família" -> contacts.filter { it.note.lowercase().contains("family") || it.note.lowercase().contains("família") || it.note.lowercase().contains("mãe") || it.note.lowercase().contains("pai") }
                else -> contacts
            }

            if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum contato nesta pasta", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 36.dp),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val grouped = displayList.groupBy { it.name.firstOrNull()?.uppercase() ?: "#" }
                    for ((letter, contactsInGroup) in grouped.entries.sortedBy { it.key }) {
                        item {
                            Text(
                                text = letter,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                            )
                        }

                        items(contactsInGroup) { contact ->
                            ImprovedContactRow(
                                contact = contact,
                                onClick = { onContactClick(contact) }
                            )
                        }
                    }
                }
            }

            // Vertical alphabet scroll bar (styled)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp, top = 16.dp, bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val alphabet = ('A'..'Z').toList()
                alphabet.take(15).forEach { char ->
                    Text(
                        text = char.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { }
                    )
                }
            }
        }
    }
}

@Composable
fun ImprovedContactRow(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Photo loading using AsyncImage or placeholder letter
                val photoUrl = getAvatarUrlForName(contact.name)
                if (photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = contact.name,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    val colors = listOf(
                        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
                        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688),
                        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF795548)
                    )
                    val avatarColor = colors.getOrElse(contact.avatarColor % colors.size) { Color(0xFF607D8B) }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(avatarColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isVerifiedContact(contact.name)) {
                            Icon(Icons.Default.Verified, contentDescription = "Verificado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "Celular • ${contact.phoneNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onClick) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.outline)
            }
        }
    }
}


// ---------------------- 4. SAFETY & BLOCK DASHBOARD TAB ----------------------
@Composable
fun SafetyScreenContent(
    onConfigureClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isSpamChecked by remember { mutableStateOf(true) }
    var isTeleFilterChecked by remember { mutableStateOf(false) }
    var suspiciousInput by remember { mutableStateOf("") }
    var checkerResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Segurança",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = onConfigureClick) {
                Icon(Icons.Default.Settings, contentDescription = "Configurar", tint = MaterialTheme.colorScheme.outline)
            }
        }

        // Hero Stats Card: "Database Status"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "STATUS DO BANCO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "10.2 Milhões",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Números protegidos globalmente",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), CircleShape))
                    Text("Banco Whoscall atualizado há 2m", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }

        // Suspicious Number Scanner Checker
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("IDENTIFICAR NÚMERO SUSPEITO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = suspiciousInput,
                    onValueChange = { suspiciousInput = it },
                    placeholder = { Text("Verificar chamada suspensa...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Button(
                    onClick = {
                        val trimmed = suspiciousInput.replace(" ", "")
                        checkerResult = when {
                            trimmed.isEmpty() -> null
                            trimmed.contains("4004") || trimmed.contains("8282") -> "✓ SEGURO: Pizzaria Bella Italia (Empresa verificada)"
                            trimmed.contains("208") -> "✓ SEGURO: Ativação do Cofre Administrativo"
                            trimmed.contains("1234") || trimmed.contains("5550123") -> "✓ SEGURO: Jane Cooper (Usuário Verificado)"
                            trimmed.contains("0303") || trimmed.contains("3003") -> "⚠ ATENÇÃO: Telemarketing detectado. Recomendado bloquear!"
                            else -> "⚠ DESCONHECIDO: Sem registros de reclamações no momento."
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Checar", fontWeight = FontWeight.Bold)
                }
            }

            if (checkerResult != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (checkerResult!!.contains("SEGURO")) Color(0xFFE8F5E9) else Color(0xFFFEEBEE)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = checkerResult!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (checkerResult!!.contains("SEGURO")) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Protection Switches settings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("AJUSTES DE PROTEÇÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Bloquear Chamadas de Spam", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Rejeitar automaticamente fraudes e golpistas", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = isSpamChecked, onCheckedChange = { isSpamChecked = it })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Filtro de Telemarketing", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Silenciar ou desligar ligações de vendas", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = isTeleFilterChecked, onCheckedChange = { isTeleFilterChecked = it })
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Grid Block stats: Scams, Telemarketing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BLOQUEADOS RECENTEMENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = {}) { Text("Ver Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFFEEBEE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, tint = Color(0xFFD32F2F))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Fraude / Golpes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("12 bloqueados hoje", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFFFF3E0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFEF6C00))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Telemarketing", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("4 bloqueados hoje", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        // Upgrade Promo Card banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), // deep slate dark
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Background icon accent
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.BottomEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Proteção Premium",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Banco de spam atualizado em tempo real e experiência 100% livre de anúncios.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Atualizar Agora", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}


// ---------------------- 5. DETAILED HIGH-FIDELITY CONTACT PROFILE ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onCall: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    onEdit: (Contact) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(contact.name) }
    var phone by remember { mutableStateOf(contact.phoneNumber) }
    var email by remember { mutableStateOf(contact.email) }
    var note by remember { mutableStateOf(contact.note) }
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // App bar header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                    Text("Detalhes do Contato", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                        IconButton(onClick = { onDelete(contact) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (!isEditing) {
                    // Profile Header card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.padding(bottom = 12.dp)) {
                            // Avatar loaded from mockup
                            val pic = getAvatarUrlForName(contact.name)
                            if (pic != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(pic)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = contact.name,
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(CircleShape)
                                        .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (isVerifiedContact(contact.name)) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(34.dp)
                                        .background(Color.White, CircleShape)
                                        .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text(
                            text = contact.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (isVerifiedContact(contact.name)) {
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "Perfil Verificado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "✓ Identificado por CallNix Security",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Action buttons with ripple feedback
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileActionButton(icon = Icons.Default.Call, label = "Ligar", onClick = { onCall(contact) })
                        ProfileActionButton(icon = Icons.Default.Chat, label = "Mensagem", onClick = {})
                        ProfileActionButton(icon = Icons.Default.Videocam, label = "Vídeo", onClick = {})
                        ProfileActionButton(icon = Icons.Default.Block, label = "Bloquear", tint = MaterialTheme.colorScheme.error, onClick = {})
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contact info details card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Informações de Contato", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Column {
                                    Text("Celular", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                    Text(contact.phoneNumber, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (contact.email.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("E-mail", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(contact.email, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }

                            if (contact.note.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.Note, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Column {
                                        Text("Notas", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                                        Text(contact.note, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Specific call log inside profile details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Histórico de Ligações", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                TextButton(onClick = {}) { Text("Ver Tudo", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }

                            CallLogHistoryRow(icon = Icons.Default.CallMade, label = "Chamada Efetuada", time = "Hoje, 10:45", duration = "2 min 14s")
                            HorizontalDivider()
                            CallLogHistoryRow(icon = Icons.Default.CallReceived, label = "Chamada Recebida", tint = Color(0xFF4CAF50), time = "Ontem, 16:20", duration = "12 min 05s")
                            HorizontalDivider()
                            CallLogHistoryRow(icon = Icons.Default.CallMissed, label = "Chamada Perdida", tint = MaterialTheme.colorScheme.error, time = "Segunda, 09:15", duration = "Perdida")
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                } else {
                    // Editing View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "Editar Contato", fontSize = 22.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome Completo") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telefone / Código") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-mail") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Notas") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { isEditing = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    onEdit(contact.copy(name = name, phoneNumber = phone, email = email, note = note))
                                    isEditing = false
                                },
                                enabled = name.isNotBlank() && phone.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Salvar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(tint.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}

@Composable
fun CallLogHistoryRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    time: String,
    duration: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Column {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        Text(duration, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
    }
}


// ---------------------- DIAL KEY KEYPAD COMPONENT ----------------------
@Composable
fun DialKey(
    digit: String,
    subtext: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable { onClick() }
            .testTag("key_$digit"),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtext.isNotEmpty()) {
                Text(
                    text = subtext,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}


// ---------------------- CALL OVERLAY SIMULATION SCREEN ----------------------
@Composable
fun SimulatedCallOverlay(
    contact: Contact?,
    number: String,
    onHangUp: () -> Unit
) {
    var timerSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timerSeconds++
        }
    }

    val formatSeconds = { seconds: Int ->
        val m = seconds / 60
        val s = seconds % 60
        String.format("%02d:%02d", m, s)
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A)) // deep dark call screen
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    val photoUrl = contact?.let { getAvatarUrlForName(it.name) }
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = contact?.name,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(Color(0xFF334155), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (contact?.name ?: number).firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = contact?.name ?: "Número Desconhecido",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = number,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = formatSeconds(timerSeconds),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallQuickAction(icon = Icons.Default.MicOff, label = "Mudo")
                    CallQuickAction(icon = Icons.AutoMirrored.Filled.VolumeUp, label = "Alto-falante")
                    CallQuickAction(icon = Icons.Default.GridOn, label = "Teclado")
                }

                Button(
                    onClick = onHangUp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(80.dp)
                        .testTag("hangup_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Desligar",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CallQuickAction(icon: ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White)
        }
        Text(text = label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}


// ---------------------- 6. ADD CONTACT DIALOG ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String, note: String, isFavorite: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adicionar Contato",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / Código") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_phone"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota / Observação") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        modifier = Modifier.testTag("input_contact_favorite")
                    )
                    Text(text = "Adicionar aos Favoritos")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onSave(name, phone, email, note, isFavorite)
                            }
                        },
                        enabled = name.isNotBlank() && phone.isNotBlank(),
                        modifier = Modifier.testTag("save_contact_btn")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}


// ---------------------- 7. DISGUISE SETUP DIALOG ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseSetupDialog(
    onDismiss: () -> Unit,
    onComplete: (customCode: String, enableBio: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var code by remember { mutableStateOf("***208###") }
    var enableBiometrics by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = step.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Configurar Camada de Disfarce",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider()

                when (step) {
                    1 -> {
                        Text(
                            text = "Este aplicativo é mascarado como um discador totalmente funcional para proteger seus dados contra bisbilhoteiros. Para abrir o cofre real, você precisa digitar um Código Secreto no discador.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Seu Código Secreto") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_code_input"),
                            singleLine = true,
                            placeholder = { Text("Ex: *1234#") }
                        )

                        Text(
                            text = "O código padrão inicial é ***208###. Recomendamos manter caracteres como * ou # para parecer uma chamada de serviço.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { step = 2 },
                            enabled = code.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_next_btn_1")
                        ) {
                            Text("Avançar")
                        }
                    }
                    2 -> {
                        Text(
                            text = "Para aumentar ainda mais a segurança, você pode habilitar a biometria. Após discar o Código Secreto, o aplicativo solicitará sua digital ou reconhecimento facial.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Bloqueio Biométrico",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Digital ou Reconhecimento Facial",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Switch(
                                    checked = enableBiometrics,
                                    onCheckedChange = { enableBiometrics = it },
                                    modifier = Modifier.testTag("setup_bio_toggle")
                                )
                            }
                        }

                        if (enableBiometrics) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { step = 1 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Voltar")
                            }
                            Button(
                                onClick = {
                                    onComplete(code, enableBiometrics)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("setup_finish_btn")
                            ) {
                                Text("Concluir")
                            }
                        }
                    }
                }
            }
        }
    }
}


// ---------------------- 8. BIOMETRIC AUTH SHEET ----------------------
@Composable
fun BiometricAuthSheet(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .testTag("biometric_sheet"),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Autenticação Requerida",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Verifique sua identidade com biometria (digital ou reconhecimento facial) para acessar o cofre.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable { onSuccess() }
                            .testTag("biometric_sensor_click"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Toque para simular biometria",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Text(
                        text = "Toque no sensor acima para autenticar",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }

                        TextButton(
                            onClick = { onSuccess() },
                            modifier = Modifier.testTag("use_pin_auth")
                        ) {
                            Text("Usar PIN alternativo")
                        }
                    }
                }
            }
        }
    }
}
