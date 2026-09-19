package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.User

/** La autenticacion de la Etapa 2 solo resuelve por identificador. */
class UserRepository : InMemoryRepository<User>("USR")
