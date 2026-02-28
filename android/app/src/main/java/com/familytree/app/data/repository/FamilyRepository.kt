package com.familytree.app.data.repository

import com.familytree.app.data.FamilyMemberDao
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

class FamilyRepository(private val dao: FamilyMemberDao) {

    val allMembers: Flow<List<FamilyMember>> = dao.getAllMembers()

    val memberCount: Flow<Int> = dao.getMemberCount()

    val rootMembers: Flow<List<FamilyMember>> = dao.getRootMembers()

    suspend fun getMemberById(id: String): FamilyMember? = dao.getMemberById(id)

    fun getMemberByIdFlow(id: String): Flow<FamilyMember?> = dao.getMemberByIdFlow(id)

    fun searchMembers(query: String): Flow<List<FamilyMember>> = dao.searchMembers(query)

    fun getChildren(parentId: String): Flow<List<FamilyMember>> = dao.getChildren(parentId)

    suspend fun getChildrenList(parentId: String): List<FamilyMember> = dao.getChildrenList(parentId)

    suspend fun getAllMembersList(): List<FamilyMember> = dao.getAllMembersList()

    suspend fun insertMember(member: FamilyMember) = dao.insertMember(member)

    suspend fun updateMember(member: FamilyMember) = dao.updateMember(member)

    suspend fun deleteMember(member: FamilyMember) = dao.deleteMember(member)

    suspend fun deleteMemberById(id: String) = dao.deleteMemberById(id)
}
