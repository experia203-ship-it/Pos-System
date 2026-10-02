package com.connectors.pos.users.userdtos;

public record AuditEvent(
        String action,
        String targetType,
        Long targetId,
        String details,
        Long userId,
        String userName

) {
}
