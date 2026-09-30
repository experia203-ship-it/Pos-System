package com.connectors.pos.audit;

import com.connectors.pos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records an immutable trail of money-affecting or security-sensitive actions
 * (price/discount overrides, product deletes, settings changes, user role
 * changes, sale returns/voids, shift closes) so an owner can later answer
 * "who changed this and when".
 */
@RequiredArgsConstructor
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepo;

    /**
     * Persists an audit entry in its own transaction so a later rollback of the
     * calling business operation cannot silently erase the record of what was
     * attempted; kept best-effort (never throws) so audit logging can never break
     * the primary business action.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String targetType, Long targetId, String details) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String userName = "system";
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
                userId = principal.getId();
                userName = principal.getUsername();
            }

            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .performedByUserId(userId)
                    .performedByName(userName)
                    .details(details)
                    .build();
            auditLogRepo.save(entry);
        } catch (Exception ignored) {
            // Audit logging must never break the business operation it is describing.
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findRecent(Pageable pageable) {
        return auditLogRepo.findAllByOrderByCreatedAtDesc(pageable);
    }
}
