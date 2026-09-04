package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.UsersRepository
import javax.inject.Inject

class GetAuthorNameUseCase @Inject constructor(
    private val usersRepository: UsersRepository,
) {
    suspend operator fun invoke(userId: String): String? = usersRepository.displayName(userId)
}
