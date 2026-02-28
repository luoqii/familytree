package com.familytree.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家族成员数据模型
 *
 * ID 参考 FamilySearch 的生成规则，使用 "XXXX-XXX" 格式的不可推导唯一标识。
 * 一旦生成不会改变，即使数据被删除也不会复用。
 */
@Entity(tableName = "persons")
data class FamilyMember(
    @PrimaryKey
    val id: String,

    /** 姓 */
    val lastName: String,

    /** 名 */
    val firstName: String,

    /** 性别: "male" / "female" */
    val gender: String = "male",

    /** 出生日期 (格式: YYYY-MM-DD) */
    val birthDate: String? = null,

    /** 出生地点 */
    val birthPlace: String? = null,

    /** 死亡日期 (格式: YYYY-MM-DD)，null 表示在世 */
    val deathDate: String? = null,

    /** 死亡地点 */
    val deathPlace: String? = null,

    /** 父亲 ID */
    val fatherId: String? = null,

    /** 母亲 ID */
    val motherId: String? = null,

    /** 备注信息 */
    val notes: String? = null,

    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),

    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 返回完整姓名 */
    val fullName: String
        get() = "$lastName$firstName"
}
