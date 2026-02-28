package com.familytree.app.data.repository

import com.familytree.app.data.FamilyMemberDao
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GedcomRepositoryTest {

    private lateinit var fakeDao: FakeFamilyMemberDao
    private lateinit var repository: FamilyRepository

    @Before
    fun setup() {
        fakeDao = FakeFamilyMemberDao()
        repository = FamilyRepository(fakeDao)
    }

    @Test
    fun importGedcom_fromString_insertsMembers() = runBlocking {
        val gedcom = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 大明 /张/
            1 SEX M
            1 BIRT
            2 DATE 15 MAR 1950
            0 @I2@ INDI
            1 NAME 秀英 /李/
            1 SEX F
            0 TRLR
        """.trimIndent()

        val result = repository.importGedcom(gedcom)

        assertEquals(2, result.members.size)
        val allMembers = repository.getAllMembersList()
        assertEquals(2, allMembers.size)
    }

    @Test
    fun importGedcom_fromStream_insertsMembers() = runBlocking {
        val gedcom = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 测试 /人/
            1 SEX M
            0 TRLR
        """.trimIndent()

        val result = repository.importGedcom(gedcom.byteInputStream())

        assertEquals(1, result.members.size)
        val allMembers = repository.getAllMembersList()
        assertEquals(1, allMembers.size)
    }

    @Test
    fun exportGedcom_withExistingMembers_returnsValidGedcom() = runBlocking {
        repository.insertMember(
            FamilyMember(
                id = "TEST-001", lastName = "张", firstName = "三",
                gender = "male", birthDate = "1990-01-15"
            )
        )
        repository.insertMember(
            FamilyMember(
                id = "TEST-002", lastName = "李", firstName = "四",
                gender = "female", birthDate = "1992-06-20"
            )
        )

        val gedcom = repository.exportGedcom()

        assertTrue(gedcom.contains("0 HEAD"))
        assertTrue(gedcom.contains("2 VERS 5.5"))
        assertTrue(gedcom.contains("0 TRLR"))
        assertTrue(gedcom.contains("INDI"))
        assertTrue(gedcom.contains("张"))
        assertTrue(gedcom.contains("李"))
    }

    @Test
    fun exportGedcom_emptyDatabase_returnsValidGedcom() = runBlocking {
        val gedcom = repository.exportGedcom()

        assertTrue(gedcom.contains("0 HEAD"))
        assertTrue(gedcom.contains("0 TRLR"))
    }

    @Test
    fun importThenExport_preservesData() = runBlocking {
        val gedcom = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 大明 /张/
            2 GIVN 大明
            2 SURN 张
            1 SEX M
            1 BIRT
            2 DATE 15 MAR 1950
            2 PLAC 北京
            0 @I2@ INDI
            1 NAME 秀英 /李/
            2 GIVN 秀英
            2 SURN 李
            1 SEX F
            1 BIRT
            2 DATE 8 JUL 1952
            2 PLAC 南京
            0 @F1@ FAM
            1 HUSB @I1@
            1 WIFE @I2@
            0 TRLR
        """.trimIndent()

        repository.importGedcom(gedcom)
        val exported = repository.exportGedcom()

        assertTrue(exported.contains("张"))
        assertTrue(exported.contains("大明"))
        assertTrue(exported.contains("李"))
        assertTrue(exported.contains("秀英"))
        assertTrue(exported.contains("15 MAR 1950"))
        assertTrue(exported.contains("北京"))
    }

    @Test
    fun importGedcom_addsToExistingData() = runBlocking {
        repository.insertMember(
            FamilyMember(id = "EXIST-001", lastName = "王", firstName = "五", gender = "male")
        )

        val gedcom = """
            0 HEAD
            1 SOUR Test
            1 GEDC
            2 VERS 5.5
            2 FORM LINEAGE-LINKED
            1 CHAR UTF-8
            0 @I1@ INDI
            1 NAME 新人 /赵/
            1 SEX M
            0 TRLR
        """.trimIndent()

        repository.importGedcom(gedcom)

        val allMembers = repository.getAllMembersList()
        assertEquals(2, allMembers.size)
    }
}

private class FakeFamilyMemberDao : FamilyMemberDao {
    private val members = mutableListOf<FamilyMember>()
    private val flow = MutableStateFlow<List<FamilyMember>>(emptyList())

    private fun updateFlow() {
        flow.value = members.toList()
    }

    override fun getAllMembers(): Flow<List<FamilyMember>> = flow

    override suspend fun getMemberById(id: String): FamilyMember? =
        members.find { it.id == id }

    override fun getMemberByIdFlow(id: String): Flow<FamilyMember?> =
        flow.map { list -> list.find { it.id == id } }

    override fun searchMembers(query: String): Flow<List<FamilyMember>> =
        flow.map { list ->
            list.filter { it.lastName.contains(query) || it.firstName.contains(query) }
        }

    override fun getChildren(parentId: String): Flow<List<FamilyMember>> =
        flow.map { list ->
            list.filter { it.fatherId == parentId || it.motherId == parentId }
        }

    override suspend fun getChildrenList(parentId: String): List<FamilyMember> =
        members.filter { it.fatherId == parentId || it.motherId == parentId }

    override fun getRootMembers(): Flow<List<FamilyMember>> =
        flow.map { list ->
            list.filter { it.fatherId == null && it.motherId == null }
        }

    override fun getMemberCount(): Flow<Int> = flow.map { it.size }

    override suspend fun insertMember(member: FamilyMember) {
        members.removeAll { it.id == member.id }
        members.add(member)
        updateFlow()
    }

    override suspend fun updateMember(member: FamilyMember) {
        val index = members.indexOfFirst { it.id == member.id }
        if (index >= 0) {
            members[index] = member
            updateFlow()
        }
    }

    override suspend fun deleteMember(member: FamilyMember) {
        members.removeAll { it.id == member.id }
        updateFlow()
    }

    override suspend fun deleteMemberById(id: String) {
        members.removeAll { it.id == id }
        updateFlow()
    }

    override suspend fun getAllMembersList(): List<FamilyMember> = members.toList()
}
