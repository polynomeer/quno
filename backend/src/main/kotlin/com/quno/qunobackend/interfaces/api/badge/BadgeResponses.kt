package com.quno.qunobackend.interfaces.api.badge

import com.quno.qunobackend.domain.badge.BadgeTier
import com.quno.qunobackend.domain.badge.BadgeType

/** Only the identifier and tier are sent — display copy (name/description) is the frontend's
 * responsibility, same split as `NotificationType`/`describeNotification`. */
data class BadgeResponse(
    val type: BadgeType,
    val tier: BadgeTier,
)
