package com.connectors.pos.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequiredArgsConstructor
@Controller
@RequestMapping("/audit")
public class AuditController {


    private final AuditLogService auditLogService;


    @GetMapping
    public String viewAuditLogsPage(@PageableDefault(size = 20) Pageable pageable, Model model) {
        model.addAttribute("auditLogs", auditLogService.findRecent(pageable));
        return "audit";
    }
}
