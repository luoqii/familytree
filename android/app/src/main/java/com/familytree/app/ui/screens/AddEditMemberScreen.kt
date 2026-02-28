package com.familytree.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familytree.app.ui.viewmodel.FamilyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMemberScreen(
    viewModel: FamilyViewModel,
    onNavigateBack: () -> Unit,
    memberId: String? = null
) {
    val isEditing = memberId != null
    val allMembers by viewModel.allMembers.collectAsState()

    var lastName by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("male") }
    var birthDate by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var deathDate by remember { mutableStateOf("") }
    var deathPlace by remember { mutableStateOf("") }
    var fatherId by remember { mutableStateOf<String?>(null) }
    var motherId by remember { mutableStateOf<String?>(null) }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(memberId) {
        if (isEditing && !loaded) {
            viewModel.loadMember(memberId!!)
        }
    }

    val selectedMember by viewModel.selectedMember.collectAsState()

    LaunchedEffect(selectedMember) {
        if (isEditing && !loaded && selectedMember != null && selectedMember!!.id == memberId) {
            val m = selectedMember!!
            lastName = m.lastName
            firstName = m.firstName
            gender = m.gender
            birthDate = m.birthDate ?: ""
            birthPlace = m.birthPlace ?: ""
            deathDate = m.deathDate ?: ""
            deathPlace = m.deathPlace ?: ""
            fatherId = m.fatherId
            motherId = m.motherId
            notes = m.notes ?: ""
            loaded = true
        }
    }

    val canSave = lastName.isNotBlank() && firstName.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "编辑成员" else "添加成员",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (canSave) {
                                if (isEditing) {
                                    viewModel.updateMember(
                                        id = memberId!!,
                                        lastName = lastName,
                                        firstName = firstName,
                                        gender = gender,
                                        birthDate = birthDate,
                                        birthPlace = birthPlace,
                                        deathDate = deathDate,
                                        deathPlace = deathPlace,
                                        fatherId = fatherId,
                                        motherId = motherId,
                                        notes = notes
                                    )
                                } else {
                                    viewModel.addMember(
                                        lastName = lastName,
                                        firstName = firstName,
                                        gender = gender,
                                        birthDate = birthDate,
                                        birthPlace = birthPlace,
                                        deathDate = deathDate,
                                        deathPlace = deathPlace,
                                        fatherId = fatherId,
                                        motherId = motherId,
                                        notes = notes
                                    )
                                }
                                onNavigateBack()
                            }
                        },
                        enabled = canSave
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "基本信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("姓 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("名 *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("性别", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                FilterChip(
                    selected = gender == "male",
                    onClick = { gender = "male" },
                    label = { Text("男") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = gender == "female",
                    onClick = { gender = "female" },
                    label = { Text("女") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "生卒信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it },
                label = { Text("出生日期") },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = birthPlace,
                onValueChange = { birthPlace = it },
                label = { Text("出生地点") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = deathDate,
                onValueChange = { deathDate = it },
                label = { Text("死亡日期") },
                placeholder = { Text("YYYY-MM-DD (在世则留空)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = deathPlace,
                onValueChange = { deathPlace = it },
                label = { Text("死亡地点") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "家族关系",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val availableParents = allMembers.filter { it.id != memberId }

            ParentSelector(
                label = "父亲",
                selectedId = fatherId,
                candidates = availableParents.filter { it.gender == "male" },
                onSelect = { fatherId = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            ParentSelector(
                label = "母亲",
                selectedId = motherId,
                candidates = availableParents.filter { it.gender == "female" },
                onSelect = { motherId = it }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "其他",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("备注") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentSelector(
    label: String,
    selectedId: String?,
    candidates: List<com.familytree.app.data.model.FamilyMember>,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = candidates.find { it.id == selectedId }?.fullName ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("无") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            candidates.forEach { member ->
                DropdownMenuItem(
                    text = { Text(member.fullName) },
                    onClick = {
                        onSelect(member.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
