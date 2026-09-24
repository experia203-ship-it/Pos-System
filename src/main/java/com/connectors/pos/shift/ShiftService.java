package com.connectors.pos.shift;

import com.connectors.pos.ordersystem.OrderRepository;
import com.connectors.pos.users.Users;
import com.connectors.pos.users.UserRepository; // Add your user repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class ShiftService {

    private final ShiftRepository shiftRepo;
    private final CashDrawerRepository cashRepo;
    private final OrderRepository orderRepo;
    private final UserRepository userRepo;

    @Transactional
    public ShiftSession openShift(Long userId, BigDecimal startingFloat) {
        return shiftRepo.findOpenShift(userId).orElseGet(() -> {
            // Fetch the user using the ID provided by the controller
            Users user = userRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShiftSession newShift = ShiftSession.builder()
                    .user(user)
                    .startingFloat(startingFloat)
                    .status(ShiftStatus.OPEN)
                    .build();

            return shiftRepo.save(newShift);
        });
    }

    @Transactional(readOnly = true)
    public ShiftSession getActiveShift(Long userId) {
        return shiftRepo.findOpenShift(userId)
                .orElseThrow(() -> new RuntimeException("No active shift for that user"));
    }

    @Transactional
    public CashDrawerEvent addCashEvent(Long shiftId, EventType type, BigDecimal amount, String reason) {
        ShiftSession shift = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found"));

        CashDrawerEvent event = CashDrawerEvent.builder()
                .eventType(type)
                .amount(amount)
                .reason(reason)
                .shiftSession(shift)
                .build();

        return cashRepo.save(event);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateExpectedCash(Long shiftId) {
        ShiftSession session = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found"));

        BigDecimal totalShift =  orderRepo.sumShiftTotal(shiftId)!=null?orderRepo.sumShiftTotal(shiftId):BigDecimal.ZERO;
        BigDecimal eventOut = cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_OUT)!=null?cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_OUT):BigDecimal.ZERO;
        BigDecimal eventIn = cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_IN)!=null?cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_IN):BigDecimal.ZERO;

        return (session.getStartingFloat()!=null ?session.getStartingFloat():BigDecimal.ZERO)
                .add(totalShift)
                .add(eventIn)
                .subtract(eventOut);
    }

    @Transactional
    public ShiftSession closeShift(Long shiftId, BigDecimal actualCount) {
        ShiftSession shift = shiftRepo.findById(shiftId)
                .orElseThrow(() -> new RuntimeException("Shift not found"));

        // Calculate expected cash using the ID
        BigDecimal expected = calculateExpectedCash(shiftId);

        shift.setExpectedCash(expected);
        shift.setCountedCash(actualCount);
        shift.setEndTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
System.out.println(actualCount+"  f "+ expected);
      return  shiftRepo.save(shift);
    }
}