package com.familytree.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

/**
 * 家族成员数据访问对象 (DAO)
 */
@Dao
interface FamilyMemberDao {

    /** 获取所有家族成员，按世代排序 */
    @Query("SELECT * FROM family_members ORDER BY generation ASC, name ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    /** 根据 ID 获取家族成员 */
    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMember?

    /** 搜索家族成员 */
    @Query("SELECT * FROM family_members WHERE name LIKE '%' || :query || '%'")
    fun searchMembers(query: String): Flow<List<FamilyMember>>

    /** 获取某人的子女 */
    @Query("SELECT * FROM family_members WHERE fatherId = :parentId OR motherId = :parentId")
    fun getChildren(parentId: Long): Flow<List<FamilyMember>>

    /** 获取某一代的所有成员 */
    @Query("SELECT * FROM family_members WHERE generation = :generation")
    fun getMembersByGeneration(generation: Int): Flow<List<FamilyMember>>

    /** 获取成员总数 */
    @Query("SELECT COUNT(*) FROM family_members")
    fun getMemberCount(): Flow<Int>

    /** 插入新成员 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    /** 更新成员信息 */
    @Update
    suspend fun updateMember(member: FamilyMember)

    /** 删除成员 */
    @Delete
    suspend fun deleteMember(member: FamilyMember)

    /** 根据 ID 删除成员 */
    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)
}
