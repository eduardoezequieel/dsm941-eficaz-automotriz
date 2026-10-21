package com.eficazautomotriz.data.memory

import com.eficazautomotriz.data.UserRepository
import com.eficazautomotriz.domain.model.User

class InMemoryUserRepository : InMemoryRepository<User>("USR"), UserRepository
