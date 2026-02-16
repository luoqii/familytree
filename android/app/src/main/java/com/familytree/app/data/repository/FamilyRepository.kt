package com.familytree.app.data.repository

import com.familytree.app.data.FamilyMemberDao
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

/**
 * 家族成员数据仓库
 * 作为数据层的单一来源
 */
class FamilyRepository(private val familyMemberDao: FamilyMemberDao) {

    /** 获取所有成员 */
    val allMembers: Flow<List<FamilyMember>> = familyMemberDao.getAllMembers()

    /** 获取成员总数 */
    val memberCount: Flow<Int> = familyMemberDao.getMemberCount()

    /** 根据 ID 获取成员 */
    suspend fun getMemberById(id: Long): FamilyMember? {
        return familyMemberDao.getMemberById(id)
    }

    /** 搜索成员 */
    fun searchMembers(query: String): Flow<List<FamilyMember>> {
        return familyMemberDao.searchMembers(query)
    }

    /** 获取子女 */
    fun getChildren(parentId: Long): Flow<List<FamilyMember>> {
        return familyMemberDao.getChildren(parentId)
    }

    /** 获取某代成员 */
    fun getMembersByGeneration(generation: Int): Flow<List<FamilyMember>> {
        return familyMemberDao.getMembersByGeneration(generation)
    }

    /** 添加成员 */
    suspend fun insertMember(member: FamilyMember): Long {
        return familyMemberDao.insertMember(member)
    }

    /** 更新成员 */
    suspend fun updateMember(member: FamilyMember) {
        familyMemberDao.updateMember(member)
    }

    /** 删除成员 */
    suspend fun deleteMember(member: FamilyMember) {
        familyMemberDao.deleteMember(member)
    }

    /** 根据 ID 删除成员 */
    suspend fun deleteMemberById(id: Long) {
        familyMemberDao.deleteMemberById(id)
    }
}
