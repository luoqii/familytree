package com.familytree.app.data.model

/**
 * 人物唯一 ID 生成器
 *
 * 参考 FamilySearch 的 ID 格式，生成 "XXXX-XXX" 格式的不可推导唯一标识。
 * 使用大写字母和数字的随机组合，排除容易混淆的字符（O/0, I/1, L）。
 */
object PersonIdGenerator {

    private val CHARS = "ABCDEFGHJKMNPQRSTVWXYZ23456789"

    fun generate(): String {
        val part1 = (1..4).map { CHARS.random() }.joinToString("")
        val part2 = (1..3).map { CHARS.random() }.joinToString("")
        return "$part1-$part2"
    }
}
