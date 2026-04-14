package com.ecotrace.backend.data.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val displayName = varchar("display_name", 128)
    val role = varchar("role", 32)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ReportsTable : Table("reports") {
    val id = varchar("id", 36)
    val title = varchar("title", 255)
    val description = text("description")
    val type = varchar("type", 64)
    val status = varchar("status", 64)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val imageUrl = varchar("image_url", 512).nullable()
    val authorId = varchar("author_id", 36).references(UsersTable.id)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
