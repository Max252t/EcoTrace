package com.ecotrace.backend.data.repository

import com.ecotrace.backend.data.db.UploadsTable
import com.ecotrace.backend.data.db.dbQuery
import com.ecotrace.backend.domain.repository.UploadsRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant

class UploadsRepositoryImpl : UploadsRepository {

    override suspend fun record(name: String, userId: String) = dbQuery {
        UploadsTable.insert {
            it[UploadsTable.name] = name
            it[UploadsTable.userId] = userId
            it[createdAt] = Instant.now()
        }
        Unit
    }

    override suspend fun ownerOf(name: String): String? = dbQuery {
        UploadsTable.selectAll()
            .where { UploadsTable.name eq name }
            .map { it[UploadsTable.userId] }
            .singleOrNull()
    }

    override suspend fun delete(name: String) = dbQuery {
        UploadsTable.deleteWhere { UploadsTable.name eq name }
        Unit
    }
}
