package com.ecotrace.backend.data.repository

import com.ecotrace.backend.data.db.ReportsTable
import com.ecotrace.backend.data.db.dbQuery
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.domain.repository.ReportsRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant

class ReportsRepositoryImpl : ReportsRepository {

    override suspend fun getAll(type: ProblemType?, status: ReportStatus?): List<Report> = dbQuery {
        var query = ReportsTable.selectAll()
        if (type != null) query = query.andWhere { ReportsTable.type eq type.name }
        if (status != null) query = query.andWhere { ReportsTable.status eq status.name }
        query.map(::rowToReport)
    }

    override suspend fun getById(id: String): Report? = dbQuery {
        ReportsTable.selectAll()
            .where { ReportsTable.id eq id }
            .map(::rowToReport)
            .singleOrNull()
    }

    override suspend fun getByAuthor(authorId: String): List<Report> = dbQuery {
        ReportsTable.selectAll()
            .where { ReportsTable.authorId eq authorId }
            .map(::rowToReport)
    }

    override suspend fun create(report: Report): Report = dbQuery {
        ReportsTable.insert {
            it[id] = report.id
            it[title] = report.title
            it[description] = report.description
            it[type] = report.type.name
            it[status] = report.status.name
            it[latitude] = report.latitude
            it[longitude] = report.longitude
            it[imageUrl] = report.imageUrl
            it[authorId] = report.authorId
            it[createdAt] = report.createdAt
            it[updatedAt] = report.updatedAt
        }
        report
    }

    override suspend fun updateStatus(id: String, status: ReportStatus): Report? = dbQuery {
        val now = Instant.now()
        ReportsTable.update({ ReportsTable.id eq id }) {
            it[ReportsTable.status] = status.name
            it[updatedAt] = now
        }
        ReportsTable.selectAll()
            .where { ReportsTable.id eq id }
            .map(::rowToReport)
            .singleOrNull()
    }

    override suspend fun update(report: Report): Report? = dbQuery {
        val now = Instant.now()
        ReportsTable.update({ ReportsTable.id eq report.id }) {
            it[title] = report.title
            it[description] = report.description
            it[type] = report.type.name
            it[status] = report.status.name
            it[latitude] = report.latitude
            it[longitude] = report.longitude
            it[imageUrl] = report.imageUrl
            it[updatedAt] = now
        }
        ReportsTable.selectAll()
            .where { ReportsTable.id eq report.id }
            .map(::rowToReport)
            .singleOrNull()
    }

    override suspend fun delete(id: String): Boolean = dbQuery {
        ReportsTable.deleteWhere { ReportsTable.id eq id } > 0
    }

    private fun rowToReport(row: ResultRow) = Report(
        id = row[ReportsTable.id],
        title = row[ReportsTable.title],
        description = row[ReportsTable.description],
        type = ProblemType.valueOf(row[ReportsTable.type]),
        status = ReportStatus.valueOf(row[ReportsTable.status]),
        latitude = row[ReportsTable.latitude],
        longitude = row[ReportsTable.longitude],
        imageUrl = row[ReportsTable.imageUrl],
        authorId = row[ReportsTable.authorId],
        createdAt = row[ReportsTable.createdAt],
        updatedAt = row[ReportsTable.updatedAt],
    )
}
