package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.OtpEntry
import com.example.ui.theme.*
import com.example.util.QrCodeHelper
import com.example.util.TotpGenerator
import com.example.viewmodel.ImportStatus
import com.example.viewmodel.OtpViewModel
import com.example.viewmodel.VaultViewModel
import com.example.viewmodel.ContactViewModel
import com.example.ui.components.CameraQrScannerDialog
import com.example.ui.components.VaultMainView
import com.example.ui.components.PasswordGeneratorView
import com.example.ui.components.SecurityHealthView
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {
  private var onInteraction: (() -> Unit)? = null

  override fun onUserInteraction() {
    super.onUserInteraction()
    onInteraction?.invoke()
  }

  fun setOnUserInteractionListener(listener: (() -> Unit)?) {
    onInteraction = listener
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
          OtpAppScreen(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpAppScreen(
  modifier: Modifier = Modifier,
  viewModel: OtpViewModel = viewModel()
) {
  val context = LocalContext.current
  val vaultViewModel: VaultViewModel = viewModel()
  val contactViewModel: ContactViewModel = viewModel()
  
  var activeTab by remember { mutableStateOf(0) }
  val isVaultSet by vaultViewModel.isMasterPasswordSet.collectAsStateWithLifecycle()
  val isVaultUnlocked by vaultViewModel.isUnlocked.collectAsStateWithLifecycle()
  val entries by viewModel.filteredEntries.collectAsStateWithLifecycle()
  val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
  val revealedSecrets by viewModel.revealedEntries.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  
  val isOtpUnlocked by contactViewModel.isOtpUnlocked.collectAsStateWithLifecycle()
  val isDisguiseEnabled by contactViewModel.isDisguiseEnabled.collectAsStateWithLifecycle()

  // Track Inactivity & Auto-Lock
  val secondsRemaining by vaultViewModel.secondsRemaining.collectAsStateWithLifecycle()
  val activity = context as? MainActivity

  DisposableEffect(activity) {
    activity?.setOnUserInteractionListener {
      vaultViewModel.resetInactivityTimer()
    }
    onDispose {
      activity?.setOnUserInteractionListener(null)
    }
  }

  LaunchedEffect(secondsRemaining) {
    if (secondsRemaining == 0) {
      if (revealedSecrets.isNotEmpty()) {
        viewModel.hideAllSecrets()
      }
      contactViewModel.lockOtp()
      Toast.makeText(context, "Sessão bloqueada por inatividade de 5 minutos.", Toast.LENGTH_LONG).show()
    }
  }

  // State management for dialogs
  var showCameraScanner by remember { mutableStateOf(false) }
  var showAddManualDialog by remember { mutableStateOf(false) }
  var showImportTextDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var qrCodeToShow by remember { mutableStateOf<String?>(null) }
  var qrCodeTitleToShow by remember { mutableStateOf<String?>(null) }
  var entryToDelete by remember { mutableStateOf<OtpEntry?>(null) }

  // Launcher for QR screenshot picker
  val qrImageLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val bitmap = BitmapFactory.decodeStream(inputStream)
          if (bitmap != null) {
            viewModel.decodeAndAddFromQr(bitmap) { status ->
              when (status) {
                is ImportStatus.ErrorInvalidQr -> {
                  Toast.makeText(context, "QR Code não encontrado ou inválido na imagem.", Toast.LENGTH_LONG).show()
                }
                is ImportStatus.ErrorBulkMigrationNotSupported -> {
                  Toast.makeText(context, "O código QR do Google Authenticator em lote não é suportado. Importe uma conta por vez.", Toast.LENGTH_LONG).show()
                }
                is ImportStatus.ErrorInvalidUri -> {
                  Toast.makeText(context, "Formato do URI OTP inválido.", Toast.LENGTH_LONG).show()
                }
                is ImportStatus.SuccessDuplicateSkipped -> {
                  Toast.makeText(context, "Conta duplicada ignorada.", Toast.LENGTH_SHORT).show()
                }
                is ImportStatus.SuccessAdded -> {
                  Toast.makeText(context, "Conta \"${status.name}\" importada com sucesso!", Toast.LENGTH_LONG).show()
                }
              }
            }
          } else {
            Toast.makeText(context, "Erro ao carregar a imagem.", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Falha ao processar arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Launcher for JSON Backup picker
  val backupFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val reader = BufferedReader(InputStreamReader(inputStream))
          val jsonText = reader.readText()
          viewModel.importBackupJson(jsonText) { imported, skipped ->
            if (imported >= 0) {
              Toast.makeText(context, "$imported contas importadas ($skipped duplicadas/inválidas puladas).", Toast.LENGTH_LONG).show()
            } else {
              Toast.makeText(context, "Erro ao decodificar JSON de backup.", Toast.LENGTH_LONG).show()
            }
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "Falha ao ler o backup: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  if (isDisguiseEnabled && !isOtpUnlocked) {
    com.example.ui.components.DisguiseView(
      contactViewModel = contactViewModel,
      onUnlockApp = {
        contactViewModel.unlockOtp()
      }
    )
  } else {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      bottomBar = {
        NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
      ) {
        NavigationBarItem(
          icon = { Icon(Icons.Default.Shield, contentDescription = "Autenticador") },
          label = { Text("MFA") },
          selected = activeTab == 0,
          onClick = { activeTab = 0 }
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Lock, contentDescription = "Cofre") },
          label = { Text("Cofre") },
          selected = activeTab == 1,
          onClick = { activeTab = 1 }
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Autorenew, contentDescription = "Gerador") },
          label = { Text("Gerador") },
          selected = activeTab == 2,
          onClick = { activeTab = 2 }
        )
        NavigationBarItem(
          icon = { Icon(Icons.Default.Security, contentDescription = "Saúde") },
          label = { Text("Saúde") },
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
    ) {
      if (activeTab == 0) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
    // Elegant Top Bar with brand logo box
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
          )
        }
        Column {
          Text(
            text = stringResource(id = R.string.app_name),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("app_title")
          )
          Text(
            text = "2FA / MFA / TOTP Offline",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
          )
        }
      }

      // Quick action icons
      Row {
        IconButton(
          onClick = { showCameraScanner = true },
          modifier = Modifier.testTag("live_camera_scan_btn")
        ) {
          Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = "Escanear via Câmera",
            tint = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(
          onClick = { qrImageLauncher.launch("image/*") },
          modifier = Modifier.testTag("import_qr_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Image,
            contentDescription = "Importar Imagem QR",
            tint = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(
          onClick = { showAddManualDialog = true },
          modifier = Modifier.testTag("add_manual_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(id = R.string.add_manually),
            tint = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(
          onClick = { showExportDialog = true },
          modifier = Modifier.testTag("export_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Backup,
            contentDescription = "Exportar",
            tint = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(
          onClick = { backupFileLauncher.launch("*/*") },
          modifier = Modifier.testTag("import_btn")
        ) {
          Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = "Importar",
            tint = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    // Modern Search field with generous pill shape
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { viewModel.searchQuery.value = it },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .testTag("search_bar"),
      placeholder = { Text(text = stringResource(id = R.string.search_hint)) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { viewModel.searchQuery.value = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
          }
        }
      },
      shape = RoundedCornerShape(28.dp),
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
      )
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Premium Security Info Banner matching professional HTML theme
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = Color(0xFFE7E0FF)
      ),
      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1C4E9))
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = Color(0xFF21005D),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = "Secrets are safely stored in your OS keychain.",
          color = Color(0xFF21005D),
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // List of cards or onboarding empty state
    if (entries.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(32.dp)
        ) {
          // Abstract Canvas Illustration of Shield & Clock
          val shieldColor = MaterialTheme.colorScheme.primary
          val arcColor = MaterialTheme.colorScheme.secondary
          Canvas(modifier = Modifier.size(160.dp).padding(16.dp)) {
            // Draw visual ambient background rings
            drawCircle(
              color = shieldColor.copy(alpha = 0.08f),
              radius = size.minDimension / 2,
              center = center
            )
            drawCircle(
              color = arcColor.copy(alpha = 0.12f),
              radius = size.minDimension / 3,
              center = center
            )

            // Draw decorative rotating dash clock ring
            drawArc(
              color = arcColor.copy(alpha = 0.4f),
              startAngle = -90f,
              sweepAngle = 240f,
              useCenter = false,
              style = Stroke(width = 4.dp.toPx()),
              topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
              size = size * 0.7f
            )

            // Draw beautiful shield logo outline
            val path = androidx.compose.ui.graphics.Path().apply {
              moveTo(size.width * 0.5f, size.height * 0.25f)
              lineTo(size.width * 0.75f, size.height * 0.35f)
              lineTo(size.width * 0.75f, size.height * 0.60f)
              quadraticTo(
                size.width * 0.75f, size.height * 0.80f,
                size.width * 0.5f, size.height * 0.88f
              )
              quadraticTo(
                size.width * 0.25f, size.height * 0.80f,
                size.width * 0.25f, size.height * 0.60f
              )
              lineTo(size.width * 0.25f, size.height * 0.35f)
              close()
            }
            drawPath(
              path = path,
              color = shieldColor,
              style = Stroke(width = 6.dp.toPx())
            )

            // Central security node
            drawCircle(
              color = shieldColor,
              radius = 8.dp.toPx(),
              center = center
            )
          }

          Spacer(modifier = Modifier.height(24.dp))
          Text(
            text = stringResource(id = R.string.empty_entries),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = stringResource(id = R.string.empty_entries_sub),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
          )

          Spacer(modifier = Modifier.height(32.dp))
          Button(
            onClick = { showAddManualDialog = true },
            modifier = Modifier.testTag("onboarding_add_btn"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.add_manually))
          }
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        items(entries, key = { it.id }) { entry ->
          val isRevealed = revealedSecrets.containsKey(entry.id)
          val revealTimeLeft = revealedSecrets[entry.id] ?: 0

          OtpCard(
            entry = entry,
            currentTime = currentTime,
            isRevealed = isRevealed,
            revealTimeLeft = revealTimeLeft,
            onCopy = { code, previous ->
              val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("OTP", code)
              clipboardManager.setPrimaryClip(clip)
              val msg = if (previous) {
                "Código anterior copiado!"
              } else {
                "Código OTP copiado com sucesso!"
              }
              Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onRevealToggle = {
              if (isRevealed) {
                viewModel.hideSecret(entry.id)
              } else {
                viewModel.revealSecret(entry.id)
              }
            },
            onShowQr = {
              val rawSecret = viewModel.decryptSecret(entry)
              val uri = "otpauth://totp/${entry.issuer.ifBlank { "OTP" }}:${entry.label}?secret=$rawSecret&issuer=${entry.issuer}&algorithm=${entry.algorithm}&digits=${entry.digits}&period=${entry.period}"
              qrCodeToShow = uri
              qrCodeTitleToShow = entry.displayName
            },
            onDelete = { entryToDelete = entry },
            onMoveUp = { viewModel.moveEntryUp(entry) },
            onMoveDown = { viewModel.moveEntryDown(entry) },
            secretText = viewModel.decryptSecret(entry)
          )
        }
      }
    }
    }
  } else if (activeTab == 1) {
        VaultMainView(viewModel = vaultViewModel)
      } else if (activeTab == 2) {
        Column(modifier = Modifier.fillMaxSize()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
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
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.primary)
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Autorenew,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Column {
                Text(
                  text = "Gerador de Senhas",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = "Crie senhas seguras instantaneamente",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
              }
            }
            if (isVaultSet && isVaultUnlocked) {
              IconButton(onClick = { vaultViewModel.lockVault() }) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Bloquear Cofre", tint = MaterialTheme.colorScheme.primary)
              }
            }
          }
          PasswordGeneratorView(viewModel = vaultViewModel)
        }
      } else {
        Column(modifier = Modifier.fillMaxSize()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
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
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.primary)
                  .padding(8.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(24.dp)
                )
              }
              Column {
                Text(
                  text = "Análise de Segurança",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                  text = "Auditoria de senhas e integridade",
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
              }
            }
            if (isVaultSet && isVaultUnlocked) {
              IconButton(onClick = { vaultViewModel.lockVault() }) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Bloquear Cofre", tint = MaterialTheme.colorScheme.primary)
              }
            }
          }
          SecurityHealthView(viewModel = vaultViewModel)
        }
      }
    }
  }

  // DIALOGS

  // Camera QR scanner dialog
  if (showCameraScanner) {
    CameraQrScannerDialog(
      onDismiss = { showCameraScanner = false },
      onQrCodeScanned = { rawQrText ->
        showCameraScanner = false
        viewModel.addFromRawQrText(rawQrText) { status ->
          when (status) {
            is ImportStatus.ErrorInvalidQr -> {
              Toast.makeText(context, "Código QR inválido.", Toast.LENGTH_LONG).show()
            }
            is ImportStatus.ErrorBulkMigrationNotSupported -> {
              Toast.makeText(context, "O código QR do Google Authenticator em lote não é suportado. Importe uma conta por vez.", Toast.LENGTH_LONG).show()
            }
            is ImportStatus.ErrorInvalidUri -> {
              Toast.makeText(context, "Formato do URI OTP inválido.", Toast.LENGTH_LONG).show()
            }
            is ImportStatus.SuccessDuplicateSkipped -> {
              Toast.makeText(context, "Conta duplicada ignorada.", Toast.LENGTH_SHORT).show()
            }
            is ImportStatus.SuccessAdded -> {
              Toast.makeText(context, "Conta \"${status.name}\" importada com sucesso!", Toast.LENGTH_LONG).show()
            }
          }
        }
      }
    )
  }

  // Manual addition dialog
  if (showAddManualDialog) {
    var dName by remember { mutableStateOf("") }
    var dIssuer by remember { mutableStateOf("") }
    var dLabel by remember { mutableStateOf("") }
    var dSecret by remember { mutableStateOf("") }
    var dAlgorithm by remember { mutableStateOf("SHA1") }
    var dDigits by remember { mutableStateOf(6) }
    var dPeriod by remember { mutableStateOf(30) }

    var expandedAlgo by remember { mutableStateOf(false) }
    var expandedDigits by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showAddManualDialog = false },
      title = { Text(text = "Adicionar Conta Manualmente") },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          item {
            OutlinedTextField(
              value = dName,
              onValueChange = { dName = it },
              label = { Text(text = stringResource(id = R.string.label_display_name)) },
              modifier = Modifier.fillMaxWidth().testTag("add_name_input"),
              singleLine = true
            )
          }
          item {
            OutlinedTextField(
              value = dIssuer,
              onValueChange = { dIssuer = it },
              label = { Text(text = stringResource(id = R.string.label_issuer)) },
              modifier = Modifier.fillMaxWidth().testTag("add_issuer_input"),
              singleLine = true
            )
          }
          item {
            OutlinedTextField(
              value = dLabel,
              onValueChange = { dLabel = it },
              label = { Text(text = stringResource(id = R.string.label_account)) },
              modifier = Modifier.fillMaxWidth().testTag("add_account_input"),
              singleLine = true
            )
          }
          item {
            OutlinedTextField(
              value = dSecret,
              onValueChange = { dSecret = it },
              label = { Text(text = stringResource(id = R.string.label_secret)) },
              modifier = Modifier.fillMaxWidth().testTag("add_secret_input"),
              singleLine = true
            )
          }
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                  onClick = { expandedAlgo = true },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(text = dAlgorithm)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                  expanded = expandedAlgo,
                  onDismissRequest = { expandedAlgo = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("SHA1") },
                    onClick = { dAlgorithm = "SHA1"; expandedAlgo = false }
                  )
                  DropdownMenuItem(
                    text = { Text("SHA256") },
                    onClick = { dAlgorithm = "SHA256"; expandedAlgo = false }
                  )
                  DropdownMenuItem(
                    text = { Text("SHA512") },
                    onClick = { dAlgorithm = "SHA512"; expandedAlgo = false }
                  )
                }
              }

              Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                  onClick = { expandedDigits = true },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(text = "$dDigits Dígitos")
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                  expanded = expandedDigits,
                  onDismissRequest = { expandedDigits = false }
                ) {
                  DropdownMenuItem(
                    text = { Text("6 Dígitos") },
                    onClick = { dDigits = 6; expandedDigits = false }
                  )
                  DropdownMenuItem(
                    text = { Text("8 Dígitos") },
                    onClick = { dDigits = 8; expandedDigits = false }
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (dSecret.trim().isEmpty()) {
              Toast.makeText(context, "Chave secreta obrigatória.", Toast.LENGTH_SHORT).show()
              return@Button
            }
            val success = viewModel.addManual(
              displayName = dName.trim(),
              issuer = dIssuer.trim(),
              label = dLabel.trim(),
              secret = dSecret.trim(),
              algorithm = dAlgorithm,
              digits = dDigits,
              period = dPeriod
            )
            if (success) {
              Toast.makeText(context, "Conta adicionada com sucesso!", Toast.LENGTH_SHORT).show()
              showAddManualDialog = false
            } else {
              Toast.makeText(context, "Formato da chave secreta inválido.", Toast.LENGTH_LONG).show()
            }
          },
          modifier = Modifier.testTag("dialog_save_btn")
        ) {
          Text(text = stringResource(id = R.string.btn_save))
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddManualDialog = false }) {
          Text(text = stringResource(id = R.string.btn_cancel))
        }
      }
    )
  }

  // Delete Confirmation Dialog
  entryToDelete?.let { entry ->
    AlertDialog(
      onDismissRequest = { entryToDelete = null },
      title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
      text = { Text(text = stringResource(id = R.string.confirm_delete_msg, entry.displayName)) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteEntry(entry)
            Toast.makeText(context, "Conta removida.", Toast.LENGTH_SHORT).show()
            entryToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("dialog_confirm_delete_btn")
        ) {
          Text(text = stringResource(id = R.string.btn_delete))
        }
      },
      dismissButton = {
        TextButton(onClick = { entryToDelete = null }) {
          Text(text = stringResource(id = R.string.btn_cancel))
        }
      }
    )
  }

  // QR Display Dialog
  qrCodeToShow?.let { data ->
    Dialog(onDismissRequest = { qrCodeToShow = null }) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = qrCodeTitleToShow ?: "Código QR",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Escaneie este QR code com outro aplicativo autenticador para transferir a conta.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(24.dp))

          // Generate and display QR bitmap
          val qrBitmap = remember(data) { QrCodeHelper.generateQrCode(data, 450) }
          if (qrBitmap != null) {
            androidx.compose.foundation.Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "Código QR OTP",
              modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp)
            )
          } else {
            Text(text = "Erro ao gerar código QR.")
          }

          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = { qrCodeToShow = null },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = "Fechar")
          }
        }
      }
    }
  }

  // Backup Export Option Selector / Text view
  if (showExportDialog) {
    AlertDialog(
      onDismissRequest = { showExportDialog = false },
      title = { Text(text = "Exportar Backup Completo") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Isso exportará todas as suas contas cadastradas com os segredos abertos em formato JSON. Mantenha esse arquivo altamente seguro!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
          )
          Text(
            text = "Dica: Para emuladores, o botão 'Copiar para Clipboard' copia os dados em texto puro que você pode colar em seu computador.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      },
      confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
            onClick = {
              val backupJson = viewModel.exportBackupJson()
              val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Backup JSON", backupJson)
              clipboardManager.setPrimaryClip(clip)
              Toast.makeText(context, "Backup JSON copiado para a área de transferência!", Toast.LENGTH_LONG).show()
              showExportDialog = false
            }
          ) {
            Text(text = "Copiar para Clipboard")
          }
          Button(
            onClick = {
              // Export as manual text import window trigger
              showImportTextDialog = true
              showExportDialog = false
            }
          ) {
            Text(text = "Exibir Código")
          }
        }
      },
      dismissButton = {
        TextButton(onClick = { showExportDialog = false }) {
          Text(text = "Cancelar")
        }
      }
    )
  }

  // Raw Backup importer/exporter text dialog (Ideal for Emulator text sharing!)
  if (showImportTextDialog) {
    var rawText by remember { mutableStateOf(viewModel.exportBackupJson()) }
    AlertDialog(
      onDismissRequest = { showImportTextDialog = false },
      title = { Text(text = "Backup em Texto JSON") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Você pode copiar ou colar o JSON abaixo diretamente para Backup e Restauração rápida no emulador:",
            fontSize = 12.sp
          )
          OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(200.dp)
              .testTag("backup_text_area"),
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 10
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (rawText.isBlank()) {
              Toast.makeText(context, "Texto de backup vazio.", Toast.LENGTH_SHORT).show()
              return@Button
            }
            viewModel.importBackupJson(rawText) { imported, skipped ->
              if (imported >= 0) {
                Toast.makeText(context, "$imported contas importadas ($skipped duplicadas/inválidas puladas).", Toast.LENGTH_LONG).show()
                showImportTextDialog = false
              } else {
                Toast.makeText(context, "Erro ao processar JSON. Formato inválido.", Toast.LENGTH_LONG).show()
              }
            }
          }
        ) {
          Text(text = "Importar deste Texto")
        }
      },
      dismissButton = {
        TextButton(onClick = { showImportTextDialog = false }) {
          Text(text = "Fechar")
        }
      }
    )
  }
  }
}

@Composable
fun OtpCard(
  entry: OtpEntry,
  currentTime: Long,
  isRevealed: Boolean,
  revealTimeLeft: Int,
  onCopy: (String, Boolean) -> Unit,
  onRevealToggle: () -> Unit,
  onShowQr: () -> Unit,
  onDelete: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  secretText: String
) {
  var isExpanded by remember { mutableStateOf(true) }

  // Generate TOTP for current period
  val activeCode = remember(currentTime, secretText, entry) {
    TotpGenerator.generateTOTP(
      secret = secretText,
      time = currentTime,
      period = entry.period,
      digits = entry.digits,
      algorithm = entry.algorithm
    )
  }

  // Generate TOTP for previous period (rescue rollover feature)
  val previousCode = remember(currentTime, secretText, entry) {
    TotpGenerator.generateTOTP(
      secret = secretText,
      time = currentTime - entry.period,
      period = entry.period,
      digits = entry.digits,
      algorithm = entry.algorithm
    )
  }

  val remainingSecs = entry.period - (currentTime % entry.period).toInt()
  val progress = remainingSecs.toFloat() / entry.period.toFloat()

  // Dynamic colors based on time remaining
  val timerColor by animateColorAsState(
    targetValue = when {
      remainingSecs <= 5 -> DangerRed
      remainingSecs <= 10 -> AlertOrange
      else -> SuccessGreen
    },
    label = "TimerColorAnimation"
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("otp_card_${entry.id}"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    border = androidx.compose.foundation.BorderStroke(
      width = 1.dp,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Header row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { isExpanded = !isExpanded },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Dynamic visual icon matching issuer brand
          val icon = when (entry.issuer.lowercase().trim()) {
            "github" -> Icons.Default.Code
            "google" -> Icons.Default.Shield
            "slack" -> Icons.Default.Hub
            "aws" -> Icons.Default.Cloud
            "microsoft" -> Icons.Default.Terminal
            else -> Icons.Outlined.Lock
          }
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = entry.displayName,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            if (entry.issuer.isNotEmpty() || entry.label.isNotEmpty()) {
              Text(
                text = listOfNotNull(
                  entry.issuer.ifBlank { null },
                  entry.label.ifBlank { null }
                ).joinToString(" · "),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Clickable Rollover Rescue Previous Code Badge
          Box(
            modifier = Modifier
              .padding(end = 8.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
              .clickable { onCopy(previousCode, true) }
              .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "ANT: ${formatOtp(previousCode)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            }
          }

          // Small circular timer indicator
          Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
          ) {
            CircularProgressIndicator(
              progress = { progress },
              modifier = Modifier.fillMaxSize(),
              color = timerColor,
              strokeWidth = 2.5.dp,
              trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )
            Text(
              text = "$remainingSecs",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = timerColor
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Collapse arrow
          Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Recolher" else "Expandir",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
          )
        }
      }

      // Collapsible body
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
        ) {
          // Large copyable OTP digits
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
              .clickable { onCopy(activeCode, false) }
              .padding(vertical = 16.dp)
              .testTag("otp_number_box_${entry.id}"),
            contentAlignment = Alignment.Center
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                text = formatOtp(activeCode),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = timerColor,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("otp_value_${entry.id}")
              )
              Spacer(modifier = Modifier.width(16.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copiar Código",
                tint = timerColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Row for secret key (masked/revealed)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
              .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isRevealed) secretText.chunked(4).joinToString(" ") else "•••• •••• •••• ••••",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = if (isRevealed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
              )
            }

            TextButton(
              onClick = onRevealToggle,
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(28.dp)
            ) {
              val btnText = if (isRevealed) "Ocultar (${revealTimeLeft}s)" else "Revelar"
              Text(
                text = btnText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Card Action Buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Reordering up/down chevrons
            Row {
              IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowUp,
                  contentDescription = "Mover para Cima",
                  tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
              }
              IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(36.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = "Mover para Baixo",
                  tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
              }
            }

            // Secondary actions (Show QR / Delete)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedButton(
                onClick = onShowQr,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.QrCode,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Exibir QR", fontSize = 12.sp)
              }

              IconButton(
                onClick = onDelete,
                modifier = Modifier
                  .size(36.dp)
                  .background(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                  )
                  .testTag("delete_btn_${entry.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Excluir",
                  tint = MaterialTheme.colorScheme.error
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Beautiful sleek linear progress bar representing the professional OTP theme
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
          ) {
            Box(
              modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(3.dp))
                .background(timerColor)
            )
          }
        }
      }
    }
  }
}

// Spacing utility for high-legibility OTP displays (e.g., "123456" -> "123 456")
private fun formatOtp(code: String): String {
  if (code.length != 6 && code.length != 8) return code
  val mid = code.length / 2
  return "${code.substring(0, mid)} ${code.substring(mid)}"
}
