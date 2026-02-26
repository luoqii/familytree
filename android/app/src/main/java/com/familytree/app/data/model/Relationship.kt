package com.familytree.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 家族成员之间的关系模型
 */
@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["fromMemberId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["toMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fromMemberId"]),
        Index(value = ["toMemberId"])
    ]
)
data class Relationship(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 关系发起方 ID */
    val fromMemberId: Long,

    /** 关系接收方 ID */
    val toMemberId: Long,

    /** 关系类型: parent, child, spouse, sibling */
    val type: String,

    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis()
)
