package com.connectors.pos.shift;

import com.connectors.pos.ordersystem.OrderRepository;
import com.connectors.pos.ordersystem.SaleReturnRepository;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import com.connectors.pos.exceptions.ShiftOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftServiceBusinessTest {

    @Mock private ShiftRepository shiftRepo;
    @Mock private CashDrawerRepository cashRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private SaleReturnRepository saleReturnRepo;
    @Mock private UserRepository userRepo;

    private ShiftService service;

    @BeforeEach
    void setUp() {
        service = new ShiftService(shiftRepo, cashRepo, orderRepo, saleReturnRepo, userRepo);
    }

    @Test
    void calculatesExpectedCashFromStartingFloatPaidOrdersAndCashEvents() {
        ShiftSession shift = ShiftSession.builder().id(12L)
                .startingFloat(money("100.00")).status(ShiftStatus.OPEN).build();
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));
        when(orderRepo.sumShiftCashPayments(12L)).thenReturn(money("220.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_IN))
                .thenReturn(money("30.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_OUT))
                .thenReturn(money("50.00"));

        assertThat(service.calculateExpectedCash(12L)).isEqualByComparingTo("300.00");
    }

    @Test
    void treatsMissingTotalsAndCashEventsAsZero() {
        ShiftSession shift = ShiftSession.builder().id(13L)
                .startingFloat(null).status(ShiftStatus.OPEN).build();
        when(shiftRepo.findById(13L)).thenReturn(Optional.of(shift));

        assertThat(service.calculateExpectedCash(13L)).isEqualByComparingTo("0.00");
    }

    @Test
    void closesShiftWithExpectedAndCountedCashAndEndTime() {
        ShiftSession shift = ShiftSession.builder().id(12L)
                .startingFloat(money("100.00")).status(ShiftStatus.OPEN).build();
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));
        when(shiftRepo.lockShiftForMutation(12L)).thenReturn(1);
        when(orderRepo.sumShiftCashPayments(12L)).thenReturn(money("220.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_IN))
                .thenReturn(money("30.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_OUT))
                .thenReturn(money("50.00"));
        when(shiftRepo.save(any(ShiftSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftSession closed = service.closeShift(12L, money("295.00"));

        assertThat(closed.getStatus()).isEqualTo(ShiftStatus.CLOSED);
        assertThat(closed.getExpectedCash()).isEqualByComparingTo("300.00");
        assertThat(closed.getCountedCash()).isEqualByComparingTo("295.00");
        assertThat(closed.getEndTime()).isNotNull();
        verify(shiftRepo).save(shift);
    }

    @Test
    void refreshesExpectedCashWhenAnOrderOnAClosedShiftChanges() {
        ShiftSession shift = ShiftSession.builder().id(12L)
                .startingFloat(money("20.00")).status(ShiftStatus.CLOSED).build();
        when(shiftRepo.lockShiftForMutation(12L)).thenReturn(1);
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));
        when(orderRepo.sumShiftCashPayments(12L)).thenReturn(money("30.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_IN))
                .thenReturn(money("5.00"));
        when(cashRepo.sumAllCashEventsDuringShift(12L, EventType.PAY_OUT))
                .thenReturn(money("2.00"));
        when(shiftRepo.save(shift)).thenReturn(shift);

        service.refreshReconciliationAfterOrderUpdate(12L);

        assertThat(shift.getExpectedCash()).isEqualByComparingTo("53.00");
        verify(shiftRepo).save(shift);
    }

    @Test
    void opensShiftForUserWhenNoShiftIsAlreadyOpen() {
        Users user = new Users();
        when(shiftRepo.findOpenShift(3L)).thenReturn(Optional.empty());
        when(userRepo.lockUserForShiftOpening(3L)).thenReturn(1);
        when(userRepo.findById(3L)).thenReturn(Optional.of(user));
        when(shiftRepo.save(any(ShiftSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShiftSession opened = service.openShift(3L, money("25.00"));

        assertThat(opened.getUser()).isSameAs(user);
        assertThat(opened.getStartingFloat()).isEqualByComparingTo("25.00");
        assertThat(opened.getStatus()).isEqualTo(ShiftStatus.OPEN);
        verify(shiftRepo).save(opened);
    }

    @Test
    void reusesExistingOpenShiftWithoutCreatingAnother() {
        ShiftSession existing = ShiftSession.builder().id(4L).status(ShiftStatus.OPEN).build();
        when(userRepo.lockUserForShiftOpening(3L)).thenReturn(1);
        when(shiftRepo.findOpenShift(3L)).thenReturn(Optional.of(existing));

        assertThat(service.openShift(3L, money("99.00"))).isSameAs(existing);

        verify(shiftRepo, never()).save(any());
        verify(userRepo, never()).findById(any());
    }

    @Test
    void failsWhenExpectedCashRequestedForUnknownShift() {
        when(shiftRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculateExpectedCash(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Shift not found");
        verifyNoInteractions(orderRepo, cashRepo);
    }

    @Test
    void rejectsNegativeStartingFloat() {
        assertThatThrownBy(() -> service.openShift(3L, money("-0.01")))
                .isInstanceOf(ShiftOperationException.class)
                .hasMessageContaining("cannot be negative");
        verifyNoInteractions(shiftRepo, userRepo);
    }

    @Test
    void linksSalesToTheSameOpenShiftAfterLockingIt() {
        ShiftSession shift = ShiftSession.builder().id(12L).status(ShiftStatus.OPEN).build();
        when(shiftRepo.findOpenShift(3L)).thenReturn(Optional.of(shift));
        when(shiftRepo.lockShiftForMutation(12L)).thenReturn(1);
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));

        assertThat(service.getOpenShiftForSale(3L)).isSameAs(shift);

        verify(shiftRepo, times(2)).findOpenShift(3L);
    }

    @Test
    void rejectsCashEventsOnClosedShift() {
        ShiftSession shift = ShiftSession.builder().id(12L).status(ShiftStatus.CLOSED).build();
        when(shiftRepo.lockShiftForMutation(12L)).thenReturn(1);
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> service.addCashEvent(12L, EventType.PAY_IN,
                money("5.00"), "Cash float correction"))
                .isInstanceOf(ShiftOperationException.class)
                .hasMessageContaining("already closed");
        verifyNoInteractions(cashRepo);
    }

    @Test
    void rejectsClosingShiftMoreThanOnce() {
        ShiftSession shift = ShiftSession.builder().id(12L).status(ShiftStatus.CLOSED).build();
        when(shiftRepo.lockShiftForMutation(12L)).thenReturn(1);
        when(shiftRepo.findById(12L)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> service.closeShift(12L, money("10.00")))
                .isInstanceOf(ShiftOperationException.class)
                .hasMessageContaining("already closed");
        verify(shiftRepo, never()).save(any());
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
