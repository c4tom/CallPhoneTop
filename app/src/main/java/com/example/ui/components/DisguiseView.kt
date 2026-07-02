package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Contact
import com.example.viewmodel.ContactViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisguiseView(
    contactViewModel: ContactViewModel,
    onUnlockApp: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Dialer, 1: Contacts, 2: Favorites
    var dialNumber by remember { mutableStateOf("") }
    val searchQuery by contactViewModel.searchQuery.collectAsState()
    val filteredContacts by contactViewModel.filteredContacts.collectAsState()
    val isConfigured by contactViewModel.isConfigured.collectAsState()
    val secretCode by contactViewModel.secretCode.collectAsState()
    val isBiometricsEnabled by contactViewModel.isBiometricsEnabled.collectAsState()

    // Dialog & Flow States
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showContactDetails by remember { mutableStateOf<Contact?>(null) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showBiometricAuthSheet by remember { mutableStateOf(false) }
    var simulatedCallContact by remember { mutableStateOf<Contact?>(null) }
    var simulatedCallNumber by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Handle incoming typed/selected calls
    val handleCallAction = { number: String ->
        val trimmedNum = number.replace(" ", "").trim()
        val targetSecret = secretCode.replace(" ", "").trim()

        if ((trimmedNum == "***208###" || trimmedNum == "##208##" || trimmedNum == targetSecret) && !isConfigured) {
            // First time setup trigger
            showOnboarding = true
        } else if (trimmedNum == targetSecret) {
            // Secret access code entered
            if (isBiometricsEnabled) {
                showBiometricAuthSheet = true
            } else {
                contactViewModel.unlockOtp()
                onUnlockApp()
                Toast.makeText(context, "Cofre desbloqueado!", Toast.LENGTH_SHORT).show()
            }
            dialNumber = ""
        } else {
            // Normal number dialed - show simulated calling overlay
            simulatedCallNumber = number
            val associatedContact = filteredContacts.find { it.phoneNumber.replace(" ", "") == trimmedNum }
            simulatedCallContact = associatedContact
            dialNumber = ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Contatos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    if (activeTab == 1) {
                        IconButton(
                            onClick = { showAddContactDialog = true },
                            modifier = Modifier.testTag("add_contact_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar Contato")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Dialpad, contentDescription = "Teclado") },
                    label = { Text("Teclado") },
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Contatos") },
                    label = { Text("Contatos") },
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Favoritos") },
                    label = { Text("Favoritos") },
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 }
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
                    // DIALER SCREEN
                    DialerScreenContent(
                        dialNumber = dialNumber,
                        onNumberChange = { dialNumber = it },
                        onCall = { handleCallAction(dialNumber) }
                    )
                }
                1 -> {
                    // CONTACTS LIST SCREEN
                    ContactsScreenContent(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { contactViewModel.searchQuery.value = it },
                        contacts = filteredContacts,
                        onContactClick = { showContactDetails = it },
                        onToggleFavorite = { contactViewModel.toggleFavorite(it) }
                    )
                }
                2 -> {
                    // FAVORITES SCREEN
                    FavoritesScreenContent(
                        contacts = filteredContacts.filter { it.isFavorite },
                        onContactClick = { showContactDetails = it }
                    )
                }
            }

            // SIMULATED PHONE CALL SCREEN OVERLAY
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

            // CONTACT DETAILS DIALOG
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

            // BIOMETRIC AUTHENTICATION DIALOG (BOTTOM SHEET OR HIGH-FIDELITY MODAL)
            if (showBiometricAuthSheet) {
                BiometricAuthSheet(
                    onDismiss = { showBiometricAuthSheet = false },
                    onSuccess = {
                        showBiometricAuthSheet = false
                        contactScopeUnlock(contactViewModel, onUnlockApp)
                    }
                )
            }
        }
    }
}

private fun contactScopeUnlock(viewModel: ContactViewModel, onUnlock: () -> Unit) {
    viewModel.unlockOtp()
    onUnlock()
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

@Composable
fun DialerScreenContent(
    dialNumber: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Dial display field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
                        .testTag("dialer_display")
                )
                if (dialNumber.isNotEmpty()) {
                    Text(
                        text = "Toque no botão ligar para processar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Dialer grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // Action row (call, backspace)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Invisible spacer to balance layout if no backspace
                Spacer(modifier = Modifier.size(56.dp))

                // Call green button
                IconButton(
                    onClick = onCall,
                    enabled = dialNumber.isNotEmpty(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (dialNumber.isNotEmpty()) Color(0xFF4CAF50) else Color(0xFF4CAF50).copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Discar",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Backspace button
                if (dialNumber.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (dialNumber.isNotEmpty()) {
                                onNumberChange(dialNumber.dropLast(1))
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("backspace_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Apagar",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreenContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("contact_search"),
            placeholder = { Text("Pesquisar contatos...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhum contato encontrado",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group alphabetically
                val grouped = contacts.groupBy { it.name.first().uppercase() }
                
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
                        ContactRow(
                            contact = contact,
                            onClick = { onContactClick(contact) },
                            onToggleFavorite = { onToggleFavorite(contact) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreenContent(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    if (contacts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nenhum favorito selecionado",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Favoritos Frequentes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(contacts) { contact ->
                    ContactRow(
                        contact = contact,
                        onClick = { onContactClick(contact) },
                        onToggleFavorite = null // favorites are static here, can edit from contact details
                    )
                }
            }
        }
    }
}

@Composable
fun ContactRow(
    contact: Contact,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    val colors = listOf(
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
        Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF009688),
        Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF795548)
    )
    val avatarColor = colors.getOrElse(contact.avatarColor) { Color(0xFF607D8B) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("contact_item_${contact.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
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
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.uppercase() ?: "?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Name and number
                Column {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = contact.phoneNumber,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (onToggleFavorite != null) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("favorite_toggle_${contact.id}")
                ) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favoritar",
                        tint = if (contact.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }
    }
}

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
        onDismissRequest = { /* Don't dismiss by tapping outside */ },
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
                // Caller info
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (contact?.name ?: number).firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
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

                // In-call quick actions simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallQuickAction(icon = Icons.Default.MicOff, label = "Mudo")
                    CallQuickAction(icon = Icons.Default.VolumeUp, label = "Alto-falante")
                    CallQuickAction(icon = Icons.Default.GridOn, label = "Teclado")
                }

                // Hangup button
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
                if (!isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detalhes do Contato",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = contact.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider()

                    DetailItem(icon = Icons.Default.Phone, label = "Telefone", value = contact.phoneNumber)

                    if (contact.email.isNotEmpty()) {
                        DetailItem(icon = Icons.Default.Email, label = "E-mail", value = contact.email)
                    }

                    if (contact.note.isNotEmpty()) {
                        DetailItem(icon = Icons.Default.Note, label = "Notas", value = contact.note)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { onCall(contact) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ligar")
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = { onDelete(contact) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Excluir")
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Fechar")
                    }
                } else {
                    Text(
                        text = "Editar Contato",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone") },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onEdit(contact.copy(name = name, phoneNumber = phone, email = email, note = note))
                                isEditing = false
                            },
                            enabled = name.isNotBlank() && phone.isNotBlank()
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(text = value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
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

                Divider()

                when (step) {
                    1 -> {
                        // Secret entry point explanation & selection
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
                        // Biometrics option
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

                        // High fidelity biometric sensor visual scan simulation preview
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
                    .clickable(enabled = false) {} // avoid dismissing when clicking the card itself
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

                    // Authentic material pulse biometric finger/face button
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
