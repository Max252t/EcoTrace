package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.UsersRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetAuthorNameUseCaseTest {

    private val usersRepository: UsersRepository = mockk()

    @Test
    fun invoke_returnsDisplayNameFromRepository() = runBlocking {
        coEvery { usersRepository.displayName("author-1") } returns "Мария"
        val useCase = GetAuthorNameUseCase(usersRepository)

        val result = useCase("author-1")

        assertEquals("Мария", result)
    }

    @Test
    fun invoke_returnsNullWhenRepositoryHasNoName() = runBlocking {
        coEvery { usersRepository.displayName("author-1") } returns null
        val useCase = GetAuthorNameUseCase(usersRepository)

        val result = useCase("author-1")

        assertNull(result)
    }
}
