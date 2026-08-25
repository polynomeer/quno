package com.quno.qunobackend.interfaces.api.notification

import com.quno.qunobackend.application.notification.usecase.ListMyNotificationsUseCase
import com.quno.qunobackend.application.notification.usecase.MarkAllNotificationsReadUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/me/notifications")
class NotificationController(
    private val listMyNotificationsUseCase: ListMyNotificationsUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal userId: Long): List<NotificationResponse> =
        listMyNotificationsUseCase.execute(userId).map { it.toResponse() }

    @PostMapping("/mark-read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markRead(@AuthenticationPrincipal userId: Long) {
        markAllNotificationsReadUseCase.execute(userId)
    }
}
