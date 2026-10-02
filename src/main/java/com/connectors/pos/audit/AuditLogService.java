package com.connectors.pos.audit;

import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.userdtos.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.apache.commons.logging.Log;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

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
    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);
    /**
     * Persists an audit entry in its own transaction so a later rollback of the
     * calling business operation cannot silently erase the record of what was
     * attempted; kept best-effort (never throws) so audit logging can never break
     * the primary business action.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void log(AuditEvent event) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String userName = "system";
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
                userId = principal.getId();
                userName = principal.getUsername();
            }

            AuditLog entry = AuditLog.builder()
                    .action(event.action())
                    .targetType(event.targetType())
                    .targetId(event.targetId())
                    .performedByUserId(userId)
                    .performedByName(userName)
                    .details(event.details())
                    .build();
            auditLogRepo.saveAndFlush(entry);


    }

    @Transactional(readOnly = true)
    public Page<AuditLog> findRecent(Pageable pageable) {
        return auditLogRepo.findAllByOrderByCreatedAtDesc(pageable);
    }
}
