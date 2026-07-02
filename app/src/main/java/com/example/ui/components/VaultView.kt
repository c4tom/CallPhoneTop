package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultEntry
import com.example.viewmodel.PasswordStrength
import com.example.viewmodel.SecurityReport
import com.example.viewmodel.VaultViewModel

@Composable
fun VaultMainView(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSet by viewModel.isMasterPasswordSet.collectAsStateWithLifecycle()
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()

    var showSetupDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (!isSet) {
            // First time onboarding setup
            VaultOnboardingSetup(
                onSetupClick = { showSetupDialog = true }
            )
        } else if (!isUnlocked) {
            // Secure Lock Screen
            VaultLockScreen(viewModel = viewModel)
        } else {
            // Unlocked fully functional password vault
            VaultUnlockedDashboard(viewModel = viewModel)
        }

        if (showSetupDialog) {
            MasterPasswordSetupDialog(
                onDismiss = { showSetupDialog = false },
                onSave = { pwd ->
                    viewModel.setMasterPassword(pwd)
                    showSetupDialog = false
                    Toast.makeText(context, "Cofre protegido com sucesso!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
fun VaultOnboardingSetup(onSetupClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Cofre de Senhas Desativado",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Adicione uma camada de criptografia de nível militar (AES-GCM) para armazenar suas credenciais com segurança absoluta no seu dispositivo.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onSetupClick,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("setup_master_password_btn")
        ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("Ativar Cofre de Senhas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun VaultLockScreen(viewModel: VaultViewModel) {
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Cofre de Senhas Bloqueado",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Insira sua senha mestre para acessar as credenciais criptografadas.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                showError = false
            },
            label = { Text("Senha Mestre") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Visualizar senha"
                    )
                }
            },
            isError = showError,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("master_password_input"),
            shape = RoundedCornerShape(16.dp)
        )

        if (showError) {
            Text(
                text = "Senha mestre incorreta. Tente novamente.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = {
                val matches = viewModel.verifyMasterPassword(password)
                if (!matches) {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("unlock_vault_btn"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(imageVector = Icons.Default.LockOpen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Desbloquear Cofre", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun VaultUnlockedDashboard(viewModel: VaultViewModel) {
    val entries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val onlyFavorites by viewModel.onlyFavorites.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<VaultEntry?>(null) }

    val categories = listOf("Todos", "Login", "Cartão", "Nota Segura", "Identidade")

    Column(modifier = Modifier.fillMaxSize()) {
        // Aesthetic Header with actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Cofre de Senhas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Seus segredos protegidos localmente",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showBackupDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Backup e Sincronização",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { viewModel.lockVault() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloquear Cofre",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Search & Category Filters
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search OutlinedTextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Buscar credenciais...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("vault_search_field"),
                shape = RoundedCornerShape(28.dp),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )

            // Category Filter pills row + Favorite selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorites toggle
                IconButton(
                    onClick = { viewModel.onlyFavorites.value = !onlyFavorites },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (onlyFavorites) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                ) {
                    Icon(
                        imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Filtrar Favoritos",
                        tint = if (onlyFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Row of category pills
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { viewModel.selectedCategory.value = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // List representation
        Box(modifier = Modifier.weight(1f)) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nenhuma credencial encontrada",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toque no botão '+' abaixo para salvar um login, cartão ou nota segura.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        VaultEntryCard(
                            entry = entry,
                            viewModel = viewModel,
                            onEditClick = { editingEntry = entry },
                            onDeleteClick = { viewModel.deleteVaultEntry(entry) }
                        )
                    }
                }
            }

            // Floating action button inside the box
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_vault_entry_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Adicionar credencial")
            }
        }
    }

    if (showAddDialog) {
        AddEditVaultEntryDialog(
            entry = null,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    if (editingEntry != null) {
        AddEditVaultEntryDialog(
            entry = editingEntry,
            viewModel = viewModel,
            onDismiss = { editingEntry = null }
        )
    }

    if (showBackupDialog) {
        BackupAndCloudDialog(
            viewModel = viewModel,
            onDismiss = { showBackupDialog = false }
        )
    }
}

@Composable
fun VaultEntryCard(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isPasswordVisible by remember { mutableStateOf(false) }

    val username = remember(entry) { viewModel.decryptField(entry.usernameEncrypted, entry.usernameIv) }
    val password = remember(entry) { viewModel.decryptField(entry.passwordEncrypted, entry.passwordIv) }
    val notes = remember(entry) { viewModel.decryptField(entry.notesEncrypted, entry.notesIv) }

    val strength = remember(password) { viewModel.evaluatePasswordStrength(password) }

    val icon = when (entry.category) {
        "Cartão" -> Icons.Default.CreditCard
        "Nota Segura" -> Icons.Default.Description
        "Identidade" -> Icons.Default.Badge
        else -> Icons.Default.VpnKey
    }

    val categoryColor = when (entry.category) {
        "Cartão" -> Color(0xFF9C27B0)
        "Nota Segura" -> Color(0xFFFF9800)
        "Identidade" -> Color(0xFF4CAF50)
        else -> Color(0xFF005AC1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_card_${entry.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = entry.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.url.isNotEmpty()) {
                            Text(
                                text = entry.url,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Header Icons (Favorite, Edit, Delete)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { viewModel.toggleFavorite(entry) }) {
                        Icon(
                            imageVector = if (entry.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorito",
                            tint = if (entry.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Excluir",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body Fields (Username, Password, Notes)
            if (entry.category != "Nota Segura" && username.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Usuário",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = username,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(username))
                            Toast.makeText(context, "Usuário copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar Usuário",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (entry.category != "Nota Segura" && password.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Senha",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = if (isPasswordVisible) password else "••••••••••••",
                            fontSize = 14.sp,
                            fontFamily = if (isPasswordVisible) FontFamily.Monospace else FontFamily.Default,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Mostrar Senha",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(password))
                                Toast.makeText(context, "Senha copiada com segurança!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Senha",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Password strength indicator pill
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(strength.colorHex))
                    )
                    Text(
                        text = "Força: ${strength.label}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(strength.colorHex)
                    )
                }
            }

            if (entry.category == "Nota Segura" && notes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nota Segura",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(notes))
                                Toast.makeText(context, "Nota copiada!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar Nota",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notes,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditVaultEntryDialog(
    entry: VaultEntry?,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val isEdit = entry != null
    
    var title by remember { mutableStateOf(entry?.title ?: "") }
    var username by remember { mutableStateOf(if (isEdit) viewModel.decryptField(entry!!.usernameEncrypted, entry.usernameIv) else "") }
    var password by remember { mutableStateOf(if (isEdit) viewModel.decryptField(entry!!.passwordEncrypted, entry.passwordIv) else "") }
    var url by remember { mutableStateOf(entry?.url ?: "") }
    var notes by remember { mutableStateOf(if (isEdit) viewModel.decryptField(entry!!.notesEncrypted, entry.notesIv) else "") }
    var category by remember { mutableStateOf(entry?.category ?: "Login") }
    var isFavorite by remember { mutableStateOf(entry?.isFavorite ?: false) }

    var passwordVisible by remember { mutableStateOf(false) }

    val strength = remember(password) { viewModel.evaluatePasswordStrength(password) }

    val categories = listOf("Login", "Cartão", "Nota Segura", "Identidade")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isEdit) "Editar Credencial" else "Nova Credencial",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Category selection row
                Text(
                    text = "Tipo de item",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                )
                                .clickable { category = cat }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.split(" ")[0], // short label
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Serviço (ex: Google, Banco)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Render fields selectively based on Category
                if (category != "Nota Segura") {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuário / E-mail") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Visualizar"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            // Password strength bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val strengthInt = when (strength) {
                                    PasswordStrength.VERY_WEAK -> 1
                                    PasswordStrength.WEAK -> 2
                                    PasswordStrength.MEDIUM -> 3
                                    PasswordStrength.STRONG -> 4
                                    PasswordStrength.VERY_STRONG -> 5
                                }
                                for (i in 1..5) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (i <= strengthInt) Color(strength.colorHex)
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                            )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Nível da senha: ${strength.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(strength.colorHex)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Website URL (Opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (category == "Nota Segura") "Nota Segura" else "Notas adicionais / Observações") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                // Favorite option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isFavorite, onCheckedChange = { isFavorite = it })
                    Text(
                        text = "Marcar como favorito",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            if (isEdit) {
                                viewModel.updateVaultEntry(
                                    entry!!.id,
                                    title,
                                    username,
                                    password,
                                    url,
                                    notes,
                                    category,
                                    isFavorite
                                )
                            } else {
                                viewModel.addVaultEntry(
                                    title,
                                    username,
                                    password,
                                    url,
                                    notes,
                                    category,
                                    isFavorite
                                )
                            }
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

@Composable
fun MasterPasswordSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Configurar Senha Mestre",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "A senha mestre será necessária toda vez que você abrir a tela do cofre de senhas. Escolha uma senha forte que você não irá esquecer.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    label = { Text("Senha Mestre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Alternar visibilidade"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        confirm = it
                        error = ""
                    },
                    label = { Text("Confirmar Senha Mestre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (password.isBlank()) {
                                error = "A senha não pode estar vazia."
                                return@Button
                            }
                            if (password.length < 6) {
                                error = "A senha deve ter pelo menos 6 caracteres."
                                return@Button
                            }
                            if (password != confirm) {
                                error = "As senhas digitadas não coincidem."
                                return@Button
                            }
                            onSave(password)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ativar")
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordGeneratorView(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val generatedPassword by viewModel.generatedPassword.collectAsStateWithLifecycle()
    val length by viewModel.genLength.collectAsStateWithLifecycle()
    val upper by viewModel.genUseUppercase.collectAsStateWithLifecycle()
    val lower by viewModel.genUseLowercase.collectAsStateWithLifecycle()
    val digits by viewModel.genUseNumbers.collectAsStateWithLifecycle()
    val symbols by viewModel.genUseSymbols.collectAsStateWithLifecycle()

    val strength = remember(generatedPassword) { viewModel.evaluatePasswordStrength(generatedPassword) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Visual Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = generatedPassword.ifEmpty { "Selecione pelo menos um grupo" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (generatedPassword.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Copy and Regenerate Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (generatedPassword.isNotEmpty()) {
                                clipboardManager.setText(AnnotatedString(generatedPassword))
                                Toast.makeText(context, "Senha gerada copiada!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = generatedPassword.isNotEmpty(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.generatePassword() },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gerar Nova")
                    }
                }

                if (generatedPassword.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Força da Senha: ${strength.label}",
                        fontWeight = FontWeight.Bold,
                        color = Color(strength.colorHex),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Settings Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configurações da Senha",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Length selection Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Comprimento", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "$length caracteres", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = length.toFloat(),
                        onValueChange = {
                            viewModel.genLength.value = it.toInt()
                            viewModel.generatePassword()
                        },
                        valueRange = 8f..64f,
                        steps = 56
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // Custom Checkbox parameters
                GeneratorToggleItem(
                    label = "Letras Maiúsculas (A-Z)",
                    checked = upper,
                    onCheckedChange = {
                        viewModel.genUseUppercase.value = it
                        viewModel.generatePassword()
                    }
                )

                GeneratorToggleItem(
                    label = "Letras Minúsculas (a-z)",
                    checked = lower,
                    onCheckedChange = {
                        viewModel.genUseLowercase.value = it
                        viewModel.generatePassword()
                    }
                )

                GeneratorToggleItem(
                    label = "Números (0-9)",
                    checked = digits,
                    onCheckedChange = {
                        viewModel.genUseNumbers.value = it
                        viewModel.generatePassword()
                    }
                )

                GeneratorToggleItem(
                    label = "Símbolos Especiais (!@#$)",
                    checked = symbols,
                    onCheckedChange = {
                        viewModel.genUseSymbols.value = it
                        viewModel.generatePassword()
                    }
                )
            }
        }
    }
}

@Composable
fun GeneratorToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SecurityHealthView(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = remember(viewModel.allEntries.collectAsStateWithLifecycle().value) {
        viewModel.getSecurityHealthReport()
    }

    val isSet by viewModel.isMasterPasswordSet.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Safety Score ring Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Índice de Saúde do Cofre",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sua pontuação de segurança baseada na complexidade e reuso de senhas.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Circular Rating gauge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    val progress = report.safetyScore / 100f
                    val sweepAnimation by animateFloatAsState(
                        targetValue = progress * 360f,
                        animationSpec = tween(1200)
                    )

                    val gaugeColor = when {
                        report.safetyScore >= 80 -> Color(0xFF10B981) // Green
                        report.safetyScore >= 50 -> Color(0xFFF59E0B) // Orange
                        else -> Color(0xFFEF4444)                     // Red
                    }

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background circle
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress Arc
                        drawArc(
                            color = gaugeColor,
                            startAngle = -90f,
                            sweepAngle = sweepAnimation,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    Text(
                        text = "${report.safetyScore}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor
                    )
                }
            }
        }

        // Analytics details cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecurityStatCard(
                label = "Senhas Fracas",
                value = report.weakPasswords.toString(),
                color = Color(0xFFEF4444),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )

            SecurityStatCard(
                label = "Reutilizadas",
                value = report.reusedPasswords.toString(),
                color = Color(0xFFF59E0B),
                icon = Icons.Default.Loop,
                modifier = Modifier.weight(1f)
            )

            SecurityStatCard(
                label = "Senhas Fortes",
                value = report.strongPasswords.toString(),
                color = Color(0xFF10B981),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f)
            )
        }

        // Auto-lock status and live countdown
        val secondsRemaining by viewModel.secondsRemaining.collectAsStateWithLifecycle()
        val minutes = secondsRemaining / 60
        val seconds = secondsRemaining % 60
        val countdownStr = String.format("%02d:%02d", minutes, seconds)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Bloqueio por Inatividade",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Proteção ativa de 5 minutos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
                ) {
                    Text(
                        text = countdownStr,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Recommendations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Dicas de Segurança",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                SecurityRecommendationItem(
                    title = "Ative Senhas Únicas",
                    desc = "Nunca reuse a mesma senha em múltiplos serviços. Se um deles vazar, todos estarão em risco."
                )

                SecurityRecommendationItem(
                    title = "Use o Gerador Integrado",
                    desc = "Prefira senhas aleatórias de pelo menos 14 caracteres incluindo símbolos e números."
                )

                SecurityRecommendationItem(
                    title = "Cuidado com o Clipboard",
                    desc = "Sempre que copiar uma senha, limpe a área de transferência logo após usá-la."
                )
            }
        }

        // Security settings actions (like resetting/clearing Master Password)
        if (isSet) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.clearMasterPassword()
                            Toast.makeText(context, "Configurações do cofre redefinidas.", Toast.LENGTH_LONG).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Desativar Proteção Mestre",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Remove a senha mestre. Seus itens no cofre permanecerão criptografados, mas acessíveis sem senha.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            lineHeight = 14.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Disguise Settings Card (Camada de Disfarce)
        val contactViewModel: com.example.viewmodel.ContactViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        val disguiseEnabled by contactViewModel.isDisguiseEnabled.collectAsStateWithLifecycle()
        val bioEnabled by contactViewModel.isBiometricsEnabled.collectAsStateWithLifecycle()
        val secretPin by contactViewModel.secretCode.collectAsStateWithLifecycle()

        var showEditPinDialog by remember { mutableStateOf(false) }
        var tempPin by remember { mutableStateOf(secretPin) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Camada de Disfarce",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mascarar aplicativo como um discador telefônico.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Divider()

                // Row 1: Enable disguise switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Habilitar Mascaramento",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Abre o discador ao iniciar o aplicativo.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Switch(
                        checked = disguiseEnabled,
                        onCheckedChange = { contactViewModel.setDisguiseEnabled(it) },
                        modifier = Modifier.testTag("disguise_enable_switch")
                    )
                }

                if (disguiseEnabled) {
                    // Row 2: Biometrics switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autenticação Biométrica",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Solicitar digital ou face após discar o código.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = bioEnabled,
                            onCheckedChange = { contactViewModel.setBiometricsEnabled(it) },
                            modifier = Modifier.testTag("disguise_bio_switch")
                        )
                    }

                    // Row 3: Customize secret code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Código Secreto de Entrada",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Código ativo atual: $secretPin",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                tempPin = secretPin
                                showEditPinDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp).testTag("disguise_change_pin_btn")
                        ) {
                            Text("Alterar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (showEditPinDialog) {
            AlertDialog(
                onDismissRequest = { showEditPinDialog = false },
                title = { Text("Alterar Código Secreto") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Digite seu novo código secreto de entrada (padrão: ***208###). Recomendamos usar caracteres como * ou #.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        OutlinedTextField(
                            value = tempPin,
                            onValueChange = { tempPin = it },
                            label = { Text("Novo Código") },
                            modifier = Modifier.fillMaxWidth().testTag("disguise_new_pin_input"),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (tempPin.isNotBlank()) {
                                contactViewModel.setSecretCode(tempPin.trim())
                                showEditPinDialog = false
                                Toast.makeText(context, "Código alterado com sucesso!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = tempPin.isNotBlank(),
                        modifier = Modifier.testTag("disguise_confirm_new_pin_btn")
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditPinDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun SecurityStatCard(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SecurityRecommendationItem(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), lineHeight = 16.sp)
        }
    }
}

@Composable
fun BackupAndCloudDialog(
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cloudConfig by viewModel.cloudConfig.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Local Backup, 1: Nuvem Config

    // Export states
    var exportPassword by remember { mutableStateOf("") }
    var generatedBackupText by remember { mutableStateOf("") }
    var exportError by remember { mutableStateOf("") }

    // Import states
    var importJsonInput by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf("") }

    // Cloud input states
    var isCloudEnabled by remember { mutableStateOf(cloudConfig.isEnabled) }
    var selectedProvider by remember { mutableStateOf(cloudConfig.provider) }
    var folderName by remember { mutableStateOf(cloudConfig.folderName) }
    var clientId by remember { mutableStateOf(cloudConfig.clientId) }
    var clientSecret by remember { mutableStateOf(cloudConfig.clientSecret) }
    var customAccessToken by remember { mutableStateOf(cloudConfig.customAccessToken) }
    var s3BucketName by remember { mutableStateOf(cloudConfig.s3BucketName) }
    var s3Region by remember { mutableStateOf(cloudConfig.s3Region) }
    var s3AccessKey by remember { mutableStateOf(cloudConfig.s3AccessKey) }
    var s3SecretKey by remember { mutableStateOf(cloudConfig.s3SecretKey) }
    var sftpHost by remember { mutableStateOf(cloudConfig.sftpHost) }
    var sftpPort by remember { mutableStateOf(cloudConfig.sftpPort.toString()) }
    var sftpUser by remember { mutableStateOf(cloudConfig.sftpUser) }
    var sftpPass by remember { mutableStateOf(cloudConfig.sftpPass) }
    var sftpPath by remember { mutableStateOf(cloudConfig.sftpPath) }

    // Cloud action password state
    var cloudPassword by remember { mutableStateOf("") }

    var isSyncing by remember { mutableStateOf(false) }

    // Update VM config whenever local states change
    LaunchedEffect(isCloudEnabled, selectedProvider, folderName, clientId, clientSecret, customAccessToken, 
        s3BucketName, s3Region, s3AccessKey, s3SecretKey, sftpHost, sftpPort, sftpUser, sftpPass, sftpPath) {
        val portInt = sftpPort.toIntOrNull() ?: 22
        viewModel.updateCloudConfig(
            com.example.util.CloudConfig(
                provider = selectedProvider,
                isEnabled = isCloudEnabled,
                folderName = folderName,
                clientId = clientId,
                clientSecret = clientSecret,
                customAccessToken = customAccessToken,
                s3BucketName = s3BucketName,
                s3Region = s3Region,
                s3AccessKey = s3AccessKey,
                s3SecretKey = s3SecretKey,
                sftpHost = sftpHost,
                sftpPort = portInt,
                sftpUser = sftpUser,
                sftpPass = sftpPass,
                sftpPath = sftpPath
            )
        )
    }

    Dialog(
        onDismissRequest = {
            viewModel.clearCloudSyncStatus()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Backup & Sincronização",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Salve seus dados de forma portável e segura",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.clearCloudSyncStatus()
                        onDismiss()
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Selector (Local vs Nuvem)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Backup Local", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Backup em Nuvem", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0) {
                        LocalBackupTabContent(
                            exportPassword = exportPassword,
                            onExportPasswordChange = { exportPassword = it },
                            generatedBackupText = generatedBackupText,
                            onGenerateBackup = {
                                val backup = viewModel.generateEncryptedBackup(exportPassword)
                                if (backup != null) {
                                    generatedBackupText = backup
                                    exportError = ""
                                    clipboardManager.setText(AnnotatedString(backup))
                                    Toast.makeText(context, "Backup copiado para a área de transferência!", Toast.LENGTH_LONG).show()
                                } else {
                                    generatedBackupText = ""
                                    exportError = "Senha Master incorreta. Verifique e tente novamente."
                                }
                            },
                            exportError = exportError,
                            importJsonInput = importJsonInput,
                            onImportJsonChange = { importJsonInput = it },
                            importPassword = importPassword,
                            onImportPasswordChange = { importPassword = it },
                            importStatus = importStatus,
                            onImportClick = {
                                if (importJsonInput.isBlank() || importPassword.isBlank()) {
                                    importStatus = "Erro: Preencha o payload e a senha."
                                } else {
                                    val success = viewModel.restoreFromEncryptedBackup(importJsonInput, importPassword)
                                    if (success) {
                                        importStatus = "Importação bem-sucedida! Seus dados foram restaurados."
                                        importJsonInput = ""
                                        importPassword = ""
                                        Toast.makeText(context, "Banco de dados importado!", Toast.LENGTH_LONG).show()
                                    } else {
                                        importStatus = "Erro: Falha na descriptografia. Senha master ou payload inválido."
                                    }
                                }
                            }
                        )
                    } else {
                        CloudBackupTabContent(
                            isCloudEnabled = isCloudEnabled,
                            onEnabledChange = { isCloudEnabled = it },
                            selectedProvider = selectedProvider,
                            onProviderChange = { selectedProvider = it },
                            folderName = folderName,
                            onFolderNameChange = { folderName = it },
                            clientId = clientId,
                            onClientIdChange = { clientId = it },
                            clientSecret = clientSecret,
                            onClientSecretChange = { clientSecret = it },
                            customAccessToken = customAccessToken,
                            onCustomAccessTokenChange = { customAccessToken = it },
                            s3BucketName = s3BucketName,
                            onS3BucketNameChange = { s3BucketName = it },
                            s3Region = s3Region,
                            onS3RegionChange = { s3Region = it },
                            s3AccessKey = s3AccessKey,
                            onS3AccessKeyChange = { s3AccessKey = it },
                            s3SecretKey = s3SecretKey,
                            onS3SecretKeyChange = { s3SecretKey = it },
                            sftpHost = sftpHost,
                            onSftpHostChange = { sftpHost = it },
                            sftpPort = sftpPort,
                            onSftpPortChange = { sftpPort = it },
                            sftpUser = sftpUser,
                            onSftpUserChange = { sftpUser = it },
                            sftpPass = sftpPass,
                            onSftpPassChange = { sftpPass = it },
                            sftpPath = sftpPath,
                            onSftpPathChange = { sftpPath = it },
                            cloudPassword = cloudPassword,
                            onCloudPasswordChange = { cloudPassword = it },
                            cloudSyncStatus = cloudSyncStatus,
                            isSyncing = isSyncing,
                            onUploadClick = {
                                if (cloudPassword.isBlank()) {
                                    Toast.makeText(context, "Insira a senha master para criptografar.", Toast.LENGTH_LONG).show()
                                } else {
                                    isSyncing = true
                                    viewModel.uploadBackupToCloud(
                                        password = cloudPassword,
                                        onSuccess = {
                                            isSyncing = false
                                            Toast.makeText(context, "Backup enviado para a nuvem!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { err ->
                                            isSyncing = false
                                            Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            },
                            onDownloadClick = {
                                if (cloudPassword.isBlank()) {
                                    Toast.makeText(context, "Insira a senha master correspondente para descriptografar.", Toast.LENGTH_LONG).show()
                                } else {
                                    isSyncing = true
                                    viewModel.downloadBackupFromCloud(
                                        password = cloudPassword,
                                        onSuccess = {
                                            isSyncing = false
                                            Toast.makeText(context, "Backup baixado e restaurado com sucesso!", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { err ->
                                            isSyncing = false
                                            Toast.makeText(context, "Erro: $err", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocalBackupTabContent(
    exportPassword: String,
    onExportPasswordChange: (String) -> Unit,
    generatedBackupText: String,
    onGenerateBackup: () -> Unit,
    exportError: String,
    importJsonInput: String,
    onImportJsonChange: (String) -> Unit,
    importPassword: String,
    onImportPasswordChange: (String) -> Unit,
    importStatus: String,
    onImportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Exportar Tudo (Cofre + MFA/OTP)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gera um bloco criptografado por AES-256-GCM com chave derivada por PBKDF2. Insira sua Senha Master para autorizar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = onExportPasswordChange,
                        label = { Text("Senha Master") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (exportError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = exportError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onGenerateBackup,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = exportPassword.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exportar & Copiar Criptografado")
                    }

                    if (generatedBackupText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = generatedBackupText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Backup Criptografado (Payload)") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. Importar de Backup Criptografado",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Cole o bloco criptografado de backup e forneça a Senha Master com a qual ele foi criptografado para descriptografar e restaurar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = onImportJsonChange,
                        label = { Text("Cole o Payload Criptografado") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = onImportPasswordChange,
                        label = { Text("Senha Master do Backup") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (importStatus.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = importStatus,
                            color = if (importStatus.startsWith("Erro")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onImportClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = importJsonInput.isNotEmpty() && importPassword.isNotEmpty()
                    ) {
                        Icon(imageVector = Icons.Default.SystemUpdateAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decriptografar & Restaurar Dados")
                    }
                }
            }
        }
    }
}

@Composable
fun CloudBackupTabContent(
    isCloudEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedProvider: com.example.util.CloudProvider,
    onProviderChange: (com.example.util.CloudProvider) -> Unit,
    folderName: String,
    onFolderNameChange: (String) -> Unit,
    clientId: String,
    onClientIdChange: (String) -> Unit,
    clientSecret: String,
    onClientSecretChange: (String) -> Unit,
    customAccessToken: String,
    onCustomAccessTokenChange: (String) -> Unit,
    s3BucketName: String,
    onS3BucketNameChange: (String) -> Unit,
    s3Region: String,
    onS3RegionChange: (String) -> Unit,
    s3AccessKey: String,
    onS3AccessKeyChange: (String) -> Unit,
    s3SecretKey: String,
    onS3SecretKeyChange: (String) -> Unit,
    sftpHost: String,
    onSftpHostChange: (String) -> Unit,
    sftpPort: String,
    onSftpPortChange: (String) -> Unit,
    sftpUser: String,
    onSftpUserChange: (String) -> Unit,
    sftpPass: String,
    onSftpPassChange: (String) -> Unit,
    sftpPath: String,
    onSftpPathChange: (String) -> Unit,
    cloudPassword: String,
    onCloudPasswordChange: (String) -> Unit,
    cloudSyncStatus: com.example.util.CloudStorageManager.SyncStatus,
    isSyncing: Boolean,
    onUploadClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Toggle card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Habilitar Armazenamento em Nuvem", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "Sincroniza automaticamente ou sob demanda", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(checked = isCloudEnabled, onCheckedChange = onEnabledChange)
                }
            }
        }

        if (isCloudEnabled) {
            // Provider Selection Row
            item {
                Text(text = "Selecione o Provedor de Nuvem", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providers = com.example.util.CloudProvider.values()
                    for (provider in providers) {
                        val isSel = selectedProvider == provider
                        Button(
                            onClick = { onProviderChange(provider) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when(provider) {
                                    com.example.util.CloudProvider.GOOGLE_DRIVE -> "Drive"
                                    com.example.util.CloudProvider.ONEDRIVE -> "OneDrive"
                                    com.example.util.CloudProvider.AWS_S3 -> "S3"
                                    com.example.util.CloudProvider.SFTP -> "SFTP"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Folder details
            item {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = onFolderNameChange,
                    label = { Text("Nome da Pasta / Diretório de Backup") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Conditional parameters
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Credenciais e Conexão (Seguras & Desacopladas)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        when (selectedProvider) {
                            com.example.util.CloudProvider.GOOGLE_DRIVE, com.example.util.CloudProvider.ONEDRIVE -> {
                                OutlinedTextField(
                                    value = clientId,
                                    onValueChange = onClientIdChange,
                                    label = { Text("Client ID (OAuth2)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = clientSecret,
                                    onValueChange = onClientSecretChange,
                                    label = { Text("Client Secret (OAuth2)") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = customAccessToken,
                                    onValueChange = onCustomAccessTokenChange,
                                    label = { Text("Token de Acesso customizado / Refresh Token") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            com.example.util.CloudProvider.AWS_S3 -> {
                                OutlinedTextField(
                                    value = s3BucketName,
                                    onValueChange = onS3BucketNameChange,
                                    label = { Text("Nome do Bucket S3") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = s3Region,
                                    onValueChange = onS3RegionChange,
                                    label = { Text("Região (ex: us-east-1)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = s3AccessKey,
                                    onValueChange = onS3AccessKeyChange,
                                    label = { Text("AWS Access Key ID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = s3SecretKey,
                                    onValueChange = onS3SecretKeyChange,
                                    label = { Text("AWS Secret Access Key") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            com.example.util.CloudProvider.SFTP -> {
                                OutlinedTextField(
                                    value = sftpHost,
                                    onValueChange = onSftpHostChange,
                                    label = { Text("Host SFTP (IP ou Domínio)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sftpPort,
                                    onValueChange = onSftpPortChange,
                                    label = { Text("Porta SFTP") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(
                                    value = sftpUser,
                                    onValueChange = onSftpUserChange,
                                    label = { Text("Usuário SFTP") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sftpPass,
                                    onValueChange = onSftpPassChange,
                                    label = { Text("Senha ou Passphrase SFTP") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sftpPath,
                                    onValueChange = onSftpPathChange,
                                    label = { Text("Diretório Remoto (Caminho)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }

            // Sync actions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Executar Sincronização",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Insira sua Senha Master para criptografar/descriptografar na nuvem:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = cloudPassword,
                            onValueChange = onCloudPasswordChange,
                            label = { Text("Senha Master") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isSyncing) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Comunicando com o servidor remoto seguro...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onUploadClick,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = cloudPassword.isNotEmpty()
                                ) {
                                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salvar na Nuvem", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = onDownloadClick,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = cloudPassword.isNotEmpty()
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Baixar da Nuvem", fontSize = 11.sp)
                                }
                            }
                        }

                        // Display cloud storage status
                        when (cloudSyncStatus) {
                            is com.example.util.CloudStorageManager.SyncStatus.Success -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = (cloudSyncStatus as com.example.util.CloudStorageManager.SyncStatus.Success).message,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            is com.example.util.CloudStorageManager.SyncStatus.Error -> {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = (cloudSyncStatus as com.example.util.CloudStorageManager.SyncStatus.Error).message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
