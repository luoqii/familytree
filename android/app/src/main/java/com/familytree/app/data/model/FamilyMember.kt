package com.familytree.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家族成员数据模型
 * 使用 Room 数据库存储
 */
@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 姓名 */
    val name: String,

    /** 性别: "male" / "female" / "other" */
    val gender: String = "male",

    /** 出生日期 (时间戳) */
    val birthDate: Long? = null,

    /** 去世日期 (时间戳)，null 表示在世 */
    val deathDate: Long? = null,

    /** 父亲 ID */
    val fatherId: Long? = null,

    /** 母亲 ID */
    val motherId: Long? = null,

    /** 配偶 ID */
    val spouseId: Long? = null,

    /** 出生地 */
    val birthPlace: String? = null,

    /** 备注信息 */
    val notes: String? = null,

    /** 头像 URI */
    val avatarUri: String? = null,

    /** 世代编号 (第几代) */
    val generation: Int = 0,

    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),

    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
)
