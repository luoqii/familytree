package com.familytree.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.familytree.app.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {

    @Query("SELECT * FROM persons ORDER BY lastName ASC, firstName ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getMemberById(id: String): FamilyMember?

    @Query("SELECT * FROM persons WHERE id = :id")
    fun getMemberByIdFlow(id: String): Flow<FamilyMember?>

    @Query(
        "SELECT * FROM persons WHERE lastName LIKE '%' || :query || '%' " +
                "OR firstName LIKE '%' || :query || '%'"
    )
    fun searchMembers(query: String): Flow<List<FamilyMember>>

    @Query("SELECT * FROM persons WHERE fatherId = :parentId OR motherId = :parentId")
    fun getChildren(parentId: String): Flow<List<FamilyMember>>

    @Query("SELECT * FROM persons WHERE fatherId = :parentId OR motherId = :parentId")
    suspend fun getChildrenList(parentId: String): List<FamilyMember>

    @Query("SELECT * FROM persons WHERE fatherId IS NULL AND motherId IS NULL")
    fun getRootMembers(): Flow<List<FamilyMember>>

    @Query("SELECT COUNT(*) FROM persons")
    fun getMemberCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember)

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("DELETE FROM persons WHERE id = :id")
    suspend fun deleteMemberById(id: String)

    @Query("SELECT * FROM persons")
    suspend fun getAllMembersList(): List<FamilyMember>
}
