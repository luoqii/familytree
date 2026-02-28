package com.familytree.app.data.repository

import com.familytree.app.data.FamilyMemberDao
import com.familytree.app.data.gedcom.GedcomExporter
import com.familytree.app.data.gedcom.GedcomParser
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

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

    suspend fun importGedcom(inputStream: InputStream): GedcomParser.ParseResult {
        val parser = GedcomParser()
        val result = parser.parse(inputStream)
        for (member in result.members) {
            dao.insertMember(member)
        }
        return result
    }

    suspend fun importGedcom(gedcomText: String): GedcomParser.ParseResult {
        val parser = GedcomParser()
        val result = parser.parse(gedcomText)
        for (member in result.members) {
            dao.insertMember(member)
        }
        return result
    }

    suspend fun exportGedcom(): String {
        val members = dao.getAllMembersList()
        val exporter = GedcomExporter()
        return exporter.export(members)
    }
}
