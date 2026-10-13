package com.eficazautomotriz.data

import com.eficazautomotriz.domain.model.User

/** Authentication resolves by identifier only: it needs no extra queries. */
interface UserRepository : Repository<User>