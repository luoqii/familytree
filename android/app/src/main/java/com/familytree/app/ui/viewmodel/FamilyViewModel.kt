package com.familytree.app.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familytree.app.data.FamilyTreeDatabase
import com.familytree.app.data.model.FamilyMember
import com.familytree.app.data.model.PersonIdGenerator
import com.familytree.app.data.repository.FamilyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GedcomOperationState(
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class FamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FamilyRepository

    val allMembers: StateFlow<List<FamilyMember>>
    val memberCount: StateFlow<Int>

    private val _selectedMember = MutableStateFlow<FamilyMember?>(null)
    val selectedMember: StateFlow<FamilyMember?> = _selectedMember.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FamilyMember>>(emptyList())
    val searchResults: StateFlow<List<FamilyMember>> = _searchResults.asStateFlow()

    private val _treeData = MutableStateFlow<List<TreeNode>>(emptyList())
    val treeData: StateFlow<List<TreeNode>> = _treeData.asStateFlow()

    private val _gedcomState = MutableStateFlow(GedcomOperationState())
    val gedcomState: StateFlow<GedcomOperationState> = _gedcomState.asStateFlow()

    init {
        val dao = FamilyTreeDatabase.getDatabase(application).familyMemberDao()
        repository = FamilyRepository(dao)

        allMembers = repository.allMembers
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        memberCount = repository.memberCount
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }

    fun loadMember(id: String) {
        viewModelScope.launch {
            _selectedMember.value = repository.getMemberById(id)
        }
    }

    fun addMember(
        lastName: String,
        firstName: String,
        gender: String,
        birthDate: String?,
        birthPlace: String?,
        deathDate: String?,
        deathPlace: String?,
        fatherId: String?,
        motherId: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val member = FamilyMember(
                id = PersonIdGenerator.generate(),
                lastName = lastName,
                firstName = firstName,
                gender = gender,
                birthDate = birthDate?.takeIf { it.isNotBlank() },
                birthPlace = birthPlace?.takeIf { it.isNotBlank() },
                deathDate = deathDate?.takeIf { it.isNotBlank() },
                deathPlace = deathPlace?.takeIf { it.isNotBlank() },
                fatherId = fatherId?.takeIf { it.isNotBlank() },
                motherId = motherId?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() }
            )
            repository.insertMember(member)
        }
    }

    fun updateMember(
        id: String,
        lastName: String,
        firstName: String,
        gender: String,
        birthDate: String?,
        birthPlace: String?,
        deathDate: String?,
        deathPlace: String?,
        fatherId: String?,
        motherId: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val existing = repository.getMemberById(id) ?: return@launch
            val updated = existing.copy(
                lastName = lastName,
                firstName = firstName,
                gender = gender,
                birthDate = birthDate?.takeIf { it.isNotBlank() },
                birthPlace = birthPlace?.takeIf { it.isNotBlank() },
                deathDate = deathDate?.takeIf { it.isNotBlank() },
                deathPlace = deathPlace?.takeIf { it.isNotBlank() },
                fatherId = fatherId?.takeIf { it.isNotBlank() },
                motherId = motherId?.takeIf { it.isNotBlank() },
                notes = notes?.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateMember(updated)
        }
    }

    fun deleteMember(id: String) {
        viewModelScope.launch {
            repository.deleteMemberById(id)
        }
    }

    fun searchMembers(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
            } else {
                repository.searchMembers(query).collect { results ->
                    _searchResults.value = results
                }
            }
        }
    }

    fun importGedcom(uri: Uri) {
        viewModelScope.launch {
            _gedcomState.value = GedcomOperationState(isLoading = true)
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val result = repository.importGedcom(inputStream)
                    val msg = "成功导入 ${result.members.size} 位家族成员"
                    val errorInfo = if (result.errors.isNotEmpty()) {
                        "\n警告: ${result.errors.size} 个解析问题"
                    } else ""
                    _gedcomState.value = GedcomOperationState(successMessage = msg + errorInfo)
                } else {
                    _gedcomState.value = GedcomOperationState(errorMessage = "无法读取文件")
                }
            } catch (e: Exception) {
                _gedcomState.value = GedcomOperationState(errorMessage = "导入失败: ${e.message}")
            }
        }
    }

    fun exportGedcom(uri: Uri) {
        viewModelScope.launch {
            _gedcomState.value = GedcomOperationState(isLoading = true)
            try {
                val gedcomContent = repository.exportGedcom()
                val context = getApplication<Application>()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(gedcomContent.toByteArray(Charsets.UTF_8))
                }
                _gedcomState.value = GedcomOperationState(successMessage = "家族数据已成功导出为 GEDCOM 5.5 格式")
            } catch (e: Exception) {
                _gedcomState.value = GedcomOperationState(errorMessage = "导出失败: ${e.message}")
            }
        }
    }

    fun clearGedcomState() {
        _gedcomState.value = GedcomOperationState()
    }

    fun loadTreeData() {
        viewModelScope.launch {
            val all = repository.getAllMembersList()
            val memberMap = all.associateBy { it.id }
            val roots = all.filter { it.fatherId == null && it.motherId == null }
            val nodes = mutableListOf<TreeNode>()

            for (root in roots) {
                buildTreeNode(root, memberMap, all, 0, nodes)
            }
            _treeData.value = nodes
        }
    }

    private fun buildTreeNode(
        member: FamilyMember,
        memberMap: Map<String, FamilyMember>,
        all: List<FamilyMember>,
        depth: Int,
        result: MutableList<TreeNode>
    ) {
        val children = all.filter { it.fatherId == member.id || it.motherId == member.id }
        result.add(TreeNode(member = member, depth = depth, hasChildren = children.isNotEmpty()))
        for (child in children) {
            buildTreeNode(child, memberMap, all, depth + 1, result)
        }
    }
}

data class TreeNode(
    val member: FamilyMember,
    val depth: Int,
    val hasChildren: Boolean
)
