package kr.jinsil.panbyeolgi.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kr.jinsil.panbyeolgi.data.Settings
import kr.jinsil.panbyeolgi.data.SettingsStore
import kr.jinsil.panbyeolgi.ui.components.BackLink
import kr.jinsil.panbyeolgi.ui.components.Card
import kr.jinsil.panbyeolgi.ui.components.Heading
import kr.jinsil.panbyeolgi.ui.components.ScreenColumn
import kr.jinsil.panbyeolgi.ui.components.SecondaryButton
import kr.jinsil.panbyeolgi.ui.components.screenBackground
import kr.jinsil.panbyeolgi.ui.theme.LineIcons
import kr.jinsil.panbyeolgi.ui.theme.Palette
import kr.jinsil.panbyeolgi.ui.theme.Type
import kr.jinsil.panbyeolgi.util.Permissions
import kr.jinsil.panbyeolgi.util.ProtectionAccess

// docs/design/screens/Settings.dc.html
@Composable
fun SettingsScreen(
    settings: Settings,
    access: ProtectionAccess,
    store: SettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var editingGuardian by rememberSaveable { mutableStateOf(false) }

    fun open(intent: Intent) {
        runCatching { context.startActivity(intent) }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val messagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!Permissions.read(context).notificationListener) open(Permissions.notificationListenerSettings(context))
    }
    val callPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!Permissions.read(context).callScreeningRole) {
            Permissions.callScreeningRoleRequest(context)?.let { roleLauncher.launch(it) }
        }
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) open(Permissions.appDetailsSettings(context))
    }

    fun setUpMessageCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Permissions.granted(context, Manifest.permission.POST_NOTIFICATIONS)) {
            messagePermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!access.notificationListener) {
            open(Permissions.notificationListenerSettings(context))
        }
    }

    fun setUpCallWarning() {
        val needed = buildList {
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.ANSWER_PHONE_CALLS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.filterNot { Permissions.granted(context, it) }
        if (needed.isNotEmpty()) {
            callPermissions.launch(needed.toTypedArray())
        } else if (!access.callScreeningRole) {
            Permissions.callScreeningRoleRequest(context)?.let { roleLauncher.launch(it) }
        }
    }

    val smsOn = settings.smsCheck && access.notificationListener
    val kakaoOn = settings.kakaoCheck && access.notificationListener
    val callOn = settings.callWarning && access.callScreeningRole && access.contacts

    ScreenColumn(modifier = Modifier.screenBackground().windowInsetsPadding(WindowInsets.safeDrawing)) {
        BackLink(onBack)
        Heading("자동 보호 설정", Type.ScreenTitle, color = Palette.Ink)

        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Palette.Surface)
                .border(2.dp, Palette.Border, shape),
        ) {
            ToggleRow("문자 자동 확인", "문자가 오면 바로 검사해요", smsOn) { on ->
                store.update { it.copy(smsCheck = on) }
                if (on) setUpMessageCheck()
            }
            HorizontalDivider(thickness = 2.dp, color = Palette.Divider)
            ToggleRow("카카오톡 알림 확인", "카톡 알림에 보이는 글을 검사해요", kakaoOn) { on ->
                store.update { it.copy(kakaoCheck = on) }
                if (on) setUpMessageCheck()
            }
            HorizontalDivider(thickness = 2.dp, color = Palette.Divider)
            ToggleRow(
                title = "모르는 전화 경고",
                subtitle = if (access.callScreeningSupported) "저장 안 된 번호면 미리 알려요" else "안드로이드 10 이상에서 쓸 수 있어요",
                checked = callOn,
                enabled = access.callScreeningSupported,
            ) { on ->
                store.update { it.copy(callWarning = on) }
                if (on) setUpCallWarning()
            }
        }

        val missing = missingAccess(settings, access)
        if (missing.isNotEmpty()) {
            Card(color = Palette.CautionBg, borderColor = null) {
                Heading("허용이 더 필요해요", Type.SettingTitle, color = Palette.CautionInk)
                Text("아래 버튼을 눌러 진실판별기를 허용해 주세요.", style = Type.Notice, color = Palette.CautionInk)
                for (item in missing) {
                    SecondaryButton(item.label, onClick = {
                        when (item) {
                            Missing.LISTENER -> open(Permissions.notificationListenerSettings(context))
                            Missing.NOTIFICATIONS -> open(Permissions.appNotificationSettings(context))
                            Missing.FULL_SCREEN -> open(Permissions.fullScreenIntentSettings(context))
                            Missing.CALL_ROLE -> Permissions.callScreeningRoleRequest(context)?.let { roleLauncher.launch(it) }
                            Missing.CONTACTS -> contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                        }
                    })
                }
            }
        }

        Card(spacing = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(LineIcons.People, contentDescription = null, tint = Palette.Primary, modifier = Modifier.size(30.dp))
                Heading("보호자 연결", Type.SettingTitle, color = Palette.Ink)
            }
            Text("위험한 문자가 오면 이분께 알려드려요.", style = Type.Notice, color = Palette.InkMuted)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Palette.Background)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (settings.guardianPhone.isNotBlank()) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Palette.PrimarySoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(settings.guardianName.take(1).ifBlank { "보" }, style = Type.Badge, color = Palette.Primary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(settings.guardianName.ifBlank { "보호자" }, style = Type.Link.copy(lineHeight = Type.BodyStrong.lineHeight), color = Palette.Ink)
                        Text(settings.guardianPhone, style = Type.Small, color = Palette.InkMuted)
                    }
                } else {
                    Text("아직 정하지 않았어요. 정하지 않으면 공유 화면에서 고를 수 있어요.", style = Type.Small, color = Palette.InkMuted)
                }
            }
            SecondaryButton(
                if (settings.guardianPhone.isBlank()) "보호자 정하기" else "보호자 바꾸기",
                onClick = { editingGuardian = true },
                minHeight = 64.dp,
            )
        }

        Card(spacing = 14.dp) {
            Heading("글씨 크기", Type.SettingTitle, color = Palette.Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextSizeOption("크게", selected = !settings.extraLargeText, Modifier.weight(1f)) {
                    store.update { it.copy(extraLargeText = false) }
                }
                TextSizeOption("아주 크게", selected = settings.extraLargeText, Modifier.weight(1f)) {
                    store.update { it.copy(extraLargeText = true) }
                }
            }
        }

        Text(
            "문자와 알림 내용은 이 휴대폰 안에서만 검사해요. 직접 확인할 때만 주민번호·카드번호를 가린 뒤 서버로 보내요.",
            style = Type.Note.copy(lineHeight = Type.Note.fontSize * 1.55f),
            color = Palette.InkMuted,
        )
    }

    if (editingGuardian) {
        GuardianDialog(
            initialName = settings.guardianName,
            initialPhone = settings.guardianPhone,
            onDismiss = { editingGuardian = false },
            onSave = { name, phone ->
                store.update { it.copy(guardianName = name.trim(), guardianPhone = phone.trim()) }
                editingGuardian = false
            },
        )
    }
}

private enum class Missing(val label: String) {
    LISTENER("알림 접근 허용하기"),
    NOTIFICATIONS("알림 보내기 허용하기"),
    FULL_SCREEN("전체 화면 경고 허용하기"),
    CALL_ROLE("전화 확인 앱으로 정하기"),
    CONTACTS("연락처 확인 허용하기"),
}

private fun missingAccess(settings: Settings, access: ProtectionAccess): List<Missing> = buildList {
    val messages = settings.smsCheck || settings.kakaoCheck
    val call = settings.callWarning && access.callScreeningSupported
    if (messages && !access.notificationListener) add(Missing.LISTENER)
    if ((messages || call) && !access.postNotifications) add(Missing.NOTIFICATIONS)
    if (messages && !access.fullScreenAlerts) add(Missing.FULL_SCREEN)
    if (call && !access.callScreeningRole) add(Missing.CALL_ROLE)
    if (call && !access.contacts) add(Missing.CONTACTS)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onToggle)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Type.SettingTitle, color = if (enabled) Palette.Ink else Palette.InkMuted)
            Text(subtitle, style = Type.Small, color = Palette.InkMuted)
        }
        // 64×38 스위치. 켜짐/꺼짐은 색뿐 아니라 동그라미 위치로도 구분된다.
        Box(
            Modifier
                .width(64.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(if (checked) Palette.Primary else Palette.SwitchOff)
                .padding(4.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(Palette.Surface))
        }
    }
}

@Composable
private fun TextSizeOption(label: String, selected: Boolean, modifier: Modifier, onSelect: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(if (selected) Palette.PrimarySoft else Palette.Surface)
            .border(BorderStroke(if (selected) 3.dp else 2.dp, if (selected) Palette.Primary else Palette.ButtonBorder), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = if (selected) Type.CardTitle else Type.Link,
            color = if (selected) Palette.PrimarySoftInk else Palette.Ink,
        )
    }
}

@Composable
private fun GuardianDialog(
    initialName: String,
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initialName) }
    var phone by rememberSaveable { mutableStateOf(initialPhone) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data
        if (result.resultCode == Activity.RESULT_OK && uri != null) {
            readPickedContact(context, uri)?.let { (pickedName, pickedPhone) ->
                name = pickedName
                phone = pickedPhone
            }
        }
    }
    val fieldStyle = remember { Type.Body.copy(color = Palette.Ink) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text("보호자 연결", style = Type.SettingTitle, color = Palette.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(
                    "연락처에서 고르기",
                    onClick = {
                        runCatching {
                            picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                        }
                    },
                    icon = LineIcons.People,
                    minHeight = 64.dp,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름 (예: 큰아들)", style = Type.Small) },
                    textStyle = fieldStyle,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("전화번호", style = Type.Small) },
                    textStyle = fieldStyle,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, phone) }, contentPadding = PaddingValues(16.dp)) {
                Text("저장", style = Type.Link, color = Palette.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, contentPadding = PaddingValues(16.dp)) {
                Text("취소", style = Type.Link, color = Palette.InkMuted)
            }
        },
    )
}

/** 연락처 고르기로 받은 전화번호 항목 (고른 항목만 잠시 읽을 수 있어 연락처 권한이 필요 없다) */
private fun readPickedContact(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    context.contentResolver.query(
        uri,
        arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
        null,
        null,
        null,
    )?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        cursor.getString(0).orEmpty() to cursor.getString(1).orEmpty()
    }
}.getOrNull()
