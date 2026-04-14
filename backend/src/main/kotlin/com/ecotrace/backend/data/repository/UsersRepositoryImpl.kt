package com.ecotrace.backend.data.repository

import com.ecotrace.backend.data.db.UsersTable
import com.ecotrace.backend.data.db.dbQuery
import com.ecotrace.backend.domain.model.User
import com.ecotrace.backend.domain.model.UserRole
import com.ecotrace.backend.domain.repository.UsersRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll

class UsersRepositoryImpl : UsersRepository {

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .map(::rowToUser)
            .singleOrNull()
    }

    override suspend fun findById(id: String): User? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map(::rowToUser)
            .singleOrNull()
    }

    override suspend fun create(user: User): User = dbQuery {
        UsersTable.insert {
            it[id] = user.id
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[displayName] = user.displayName
            it[role] = user.role.name
            it[createdAt] = user.createdAt
        }
        user
    }

    override suspend fun existsByEmail(email: String): Boolean = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .count() > 0
    }

    private fun rowToUser(row: ResultRow) = User(
        id = row[UsersTable.id],
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        displayName = row[UsersTable.displayName],
        role = UserRole.valueOf(row[UsersTable.role]),
        createdAt = row[UsersTable.createdAt],
    )
}
