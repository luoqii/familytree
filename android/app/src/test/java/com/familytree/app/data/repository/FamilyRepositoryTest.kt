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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FamilyRepositoryTest {

    private lateinit var fakeDao: FakeFamilyMemberDao
    private lateinit var repository: FamilyRepository

    @Before
    fun setup() {
        fakeDao = FakeFamilyMemberDao()
        repository = FamilyRepository(fakeDao)
    }

    @Test
    fun insertMember_andGetById() = runBlocking {
        val member = FamilyMember(id = "TEST-001", lastName = "张", firstName = "三")
        repository.insertMember(member)

        val retrieved = repository.getMemberById("TEST-001")
        assertNotNull(retrieved)
        assertEquals("张三", retrieved!!.fullName)
    }

    @Test
    fun updateMember_changesData() = runBlocking {
        val member = FamilyMember(id = "TEST-002", lastName = "李", firstName = "四")
        repository.insertMember(member)

        val updated = member.copy(firstName = "五")
        repository.updateMember(updated)

        val retrieved = repository.getMemberById("TEST-002")
        assertEquals("李五", retrieved!!.fullName)
    }

    @Test
    fun deleteMemberById_removesMember() = runBlocking {
        val member = FamilyMember(id = "TEST-003", lastName = "王", firstName = "六")
        repository.insertMember(member)

        repository.deleteMemberById("TEST-003")

        val retrieved = repository.getMemberById("TEST-003")
        assertNull(retrieved)
    }

    @Test
    fun getAllMembersList_returnsAllMembers() = runBlocking {
        repository.insertMember(FamilyMember(id = "A001-AAA", lastName = "张", firstName = "一"))
        repository.insertMember(FamilyMember(id = "A002-BBB", lastName = "李", firstName = "二"))
        repository.insertMember(FamilyMember(id = "A003-CCC", lastName = "王", firstName = "三"))

        val all = repository.getAllMembersList()
        assertEquals(3, all.size)
    }

    @Test
    fun searchMembers_findsByLastName() = runBlocking {
        repository.insertMember(FamilyMember(id = "S001-AAA", lastName = "张", firstName = "大"))
        repository.insertMember(FamilyMember(id = "S002-BBB", lastName = "李", firstName = "小"))
        repository.insertMember(FamilyMember(id = "S003-CCC", lastName = "张", firstName = "明"))

        val results = repository.searchMembers("张").first()
        assertEquals(2, results.size)
    }

    @Test
    fun getChildrenList_returnsChildren() = runBlocking {
        val father = FamilyMember(id = "F001-AAA", lastName = "张", firstName = "大", gender = "male")
        repository.insertMember(father)
        repository.insertMember(
            FamilyMember(id = "C001-AAA", lastName = "张", firstName = "小一", fatherId = "F001-AAA")
        )
        repository.insertMember(
            FamilyMember(id = "C002-BBB", lastName = "张", firstName = "小二", fatherId = "F001-AAA")
        )
        repository.insertMember(
            FamilyMember(id = "C003-CCC", lastName = "李", firstName = "其他")
        )

        val children = repository.getChildrenList("F001-AAA")
        assertEquals(2, children.size)
    }

    @Test
    fun memberCount_reflectsInsertionsAndDeletions() = runBlocking {
        assertEquals(0, repository.memberCount.first())

        repository.insertMember(FamilyMember(id = "M001-AAA", lastName = "张", firstName = "一"))
        assertEquals(1, repository.memberCount.first())

        repository.insertMember(FamilyMember(id = "M002-BBB", lastName = "李", firstName = "二"))
        assertEquals(2, repository.memberCount.first())

        repository.deleteMemberById("M001-AAA")
        assertEquals(1, repository.memberCount.first())
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
