package com.connectors.pos.shift;

import com.connectors.pos.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/shift")
public class ShiftController {
    private final ShiftService shiftService;
@GetMapping("/reset-widget")
public String returnOpenShift(Model model){

    model.addAttribute("activeShift", null);

    return "pos :: shift-widget";

}


    @PostMapping("/open")
    public String openShift(@AuthenticationPrincipal UserPrincipal principal, @RequestParam("startingFloat") BigDecimal startingFloat, Model model){

        Long userId = principal.getId();


     ShiftSession session = shiftService.openShift(userId,startingFloat);
        model.addAttribute("activeShift", session);
     model.addAttribute("sessionId",session.getId());
        return "pos :: shift-widget";
    }





    @PostMapping("/close")
    public String closeShift(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("actualCount") BigDecimal actualCount,
            Model model) {

        ShiftSession session = shiftService.getActiveShift(principal.getId());
        ShiftSession closed = shiftService.closeShift(session.getId(), actualCount);

        // Pass the closed session to display the report instead of null right away
        model.addAttribute("closedShift", closed);
        return "shift :: shift-summary";
    }
    @PostMapping("/event")
    public String addCashEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("eventType") EventType eventType,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("reason") String reason,
            Model model) {

        // 1. Get the current active shift for this user
        ShiftSession session = shiftService.getActiveShift(principal.getId());

        // 2. Add the cash event to the database
        shiftService.addCashEvent(session.getId(), eventType, amount, reason);

        // 3. Put the active shift back in the model so the widget renders STATE B again
        model.addAttribute("activeShift", session);

        // 4. Return the fragment to refresh the UI seamlessly
        return "pos :: shift-widget";
    }
}
