package io.github.xhuohai.sudo.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentSessionResponse(
    @SerialName("current_user") val currentUser: CurrentSessionUser? = null
)

@Serializable
data class CurrentSessionUser(
    val id: Int,
    val username: String,
    @SerialName("avatar_template") val avatarTemplate: String? = null,
    val name: String? = null
)
