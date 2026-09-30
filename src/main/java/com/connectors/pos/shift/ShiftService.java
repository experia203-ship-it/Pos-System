package com.connectors.pos.shift;

import com.connectors.pos.ordersystem.OrderRepository;
import com.connectors.pos.ordersystem.SaleReturnRepository;
import com.connectors.pos.exceptions.ShiftOperationException;
import com.connectors.pos.users.Users;
import com.connectors.pos.users.UserRepository; // Add your user repository
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ShiftService {

    private final ShiftRepository shiftRepo;
    private final CashDrawerRepository cashRepo;
    private final OrderRepository orderRepo;
    private final SaleReturnRepository saleReturnRepo;
    private final UserRepository userRepo;

    @Transactional
    public ShiftSession openShift(Long userId, BigDecimal startingFloat) {
        requireNonNegative(startingFloat, "Starting cash cannot be negative.");
        if (userRepo.lockUserForShiftOpening(userId) != 1) {
            throw new ShiftOperationException("User not found.");
        }
        Optional<ShiftSession> openShift = shiftRepo.findOpenShift(userId);
        if (openShift.isPresent()) {
            return openShift.get();
        }
            // Fetch the user using the ID provided by the controller
            Users user = userRepo.findById(userId)
                    .orElseThrow(() -> new ShiftOperationException("User not found."));

            ShiftSession newShift = ShiftSession.builder()
                    .user(user)
                    .startingFloat(startingFloat)
                    .status(ShiftStatus.OPEN)
                    .build();

            return shiftRepo.save(newShift);
    }

    @Transactional(readOnly = true)
    public ShiftSession getActiveShift(Long userId) {
        return shiftRepo.findOpenShift(userId)
                .orElseThrow(() -> new ShiftOperationException("No active shift for that user."));
    }

    @Transactional
    public ShiftSession getOpenShiftForSale(Long userId) {
        ShiftSession shift = shiftRepo.findOpenShift(userId)
                .orElseThrow(() -> new ShiftOperationException("No active shift for that user."));
        lockAndReloadShift(shift.getId());
        return shiftRepo.findOpenShift(userId)
                .orElseThrow(() -> new ShiftOperationException("No active shift for that user."));
    }

    @Transactional(readOnly = true)
    public Optional<ShiftSession> findActiveShift(Long userId) {
        return shiftRepo.findOpenShift(userId);
    }

    @Transactional
    public CashDrawerEvent addCashEvent(Long shiftId, EventType type, BigDecimal amount, String reason) {
        ShiftSession shift = lockAndReloadShift(shiftId);
        requireOpen(shift);
        if (type == null) {
            throw new ShiftOperationException("Cash event type is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new ShiftOperationException("Cash event amount must be greater than zero.");
        }
        if (reason == null || reason.isBlank()) {
            throw new ShiftOperationException("Cash event reason is required.");
        }

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

        BigDecimal cashSales = zeroIfNull(orderRepo.sumShiftCashPayments(shiftId));
        BigDecimal cashRefunds = zeroIfNull(saleReturnRepo.sumCashRefundsForShift(shiftId));
        BigDecimal eventOut = zeroIfNull(cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_OUT));
        BigDecimal eventIn = zeroIfNull(cashRepo.sumAllCashEventsDuringShift(shiftId, EventType.PAY_IN));

        return zeroIfNull(session.getStartingFloat())
                .add(cashSales)
                .subtract(cashRefunds)
                .add(eventIn)
                .subtract(eventOut);
    }

    @Transactional
    public void refreshReconciliationAfterOrderUpdate(Long shiftId) {
        ShiftSession shift = lockAndReloadShift(shiftId);
        if (shift.getStatus() == ShiftStatus.CLOSED) {
            shift.setExpectedCash(calculateExpectedCash(shiftId));
            shiftRepo.save(shift);
        }
    }

    @Transactional
    public ShiftSession closeShift(Long shiftId, BigDecimal actualCount) {
        ShiftSession shift = lockAndReloadShift(shiftId);
        requireOpen(shift);
        requireNonNegative(actualCount, "Counted cash cannot be negative.");

        // Calculate expected cash using the ID
        BigDecimal expected = calculateExpectedCash(shiftId);

        shift.setExpectedCash(expected);
        shift.setCountedCash(actualCount);
        shift.setEndTime(LocalDateTime.now());
        shift.setStatus(ShiftStatus.CLOSED);
      return  shiftRepo.save(shift);
    }

    private static BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private static void requireOpen(ShiftSession shift) {
        if (shift.getStatus() != ShiftStatus.OPEN) {
            throw new ShiftOperationException("This shift is already closed.");
        }
    }

    private static void requireNonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) {
            throw new ShiftOperationException(message);
        }
    }

    private ShiftSession lockAndReloadShift(Long shiftId) {
        if (shiftRepo.lockShiftForMutation(shiftId) != 1) {
            throw new ShiftOperationException("Shift not found.");
        }
        return shiftRepo.findById(shiftId)
                .orElseThrow(() -> new ShiftOperationException("Shift not found."));
    }
}