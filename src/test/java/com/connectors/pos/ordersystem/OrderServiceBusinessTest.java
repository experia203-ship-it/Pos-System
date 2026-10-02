package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.exceptions.InsuffecientStockException;
import com.connectors.pos.exceptions.YouMustProvideAtLeastOneItem;
import com.connectors.pos.ordersystem.orderdtos.*;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.settings.PosStyle;
import com.connectors.pos.settings.PrintSize;
import com.connectors.pos.settings.Theme;
import com.connectors.pos.settings.SettingsService;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.shift.ShiftService;
import com.connectors.pos.shift.ShiftSession;
import com.connectors.pos.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceBusinessTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserRepository userRepo;
    @Mock private com.connectors.pos.customersystem.CustomerRepository customerRepo;
    @Mock private ProductRepository productRepo;
    @Mock private OrderItemRepository orderItemRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private SettingsService settingsService;
    @Mock private ShiftService shiftService;
    @Mock private SaleReturnRepository saleReturnRepo;
    @Mock private Authentication authentication;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderMapper, orderItemMapper, userRepo, customerRepo,
                productRepo, orderItemRepo, orderRepo, orderNumberGenerator, settingsService, shiftService,
                saleReturnRepo);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsOrderWithDiscountedTotalsStockReductionAndGeneratedNumber() {
        authenticateAsCashier();
        Products stockedProduct = product(11L, "Rotor", "20.00", "12.00", 8L);
        when(productRepo.findAllByIds(anyList())).thenReturn(List.of(stockedProduct));

        OrderCreateDto request = new OrderCreateDto(
                money("5.00"), null,
                List.of(
                        new OrderItemCreateDto(11L, 2, money("1.00"), null, null, null, null),
                        new OrderItemCreateDto(null, 3, money("2.00"), null,
                                "Shop rag", money("10.00"), money("4.00"))),
                money("40.00"), null, null);
        Order mappedOrder = Order.builder().orderItems(new HashSet<>()).build();
        OrderResponseDto response = mock(OrderResponseDto.class);
        when(orderMapper.toEntity(request)).thenReturn(mappedOrder);
        enableOpenShift();
        when(orderNumberGenerator.nextValue()).thenReturn(7L);
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(response);

        assertThat(service.createOrder(request)).isSameAs(response);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(savedOrder.capture());
        Order order = savedOrder.getValue();
        assertThat(order.getTotal()).isEqualByComparingTo("62.00");
        assertThat(order.getRevenue()).isEqualByComparingTo("26.00");
        assertThat(order.getPaid()).isEqualByComparingTo("40.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("22.00");
        assertThat(order.getOrderNumber()).isEqualTo("1007");
        assertThat(order.getShiftSession()).isNotNull();
        assertThat(order.getOrderItems()).hasSize(2);
        assertThat(order.getOrderItems()).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        assertThat(stockedProduct.getStock()).isEqualTo(6L);
        verify(userRepo).getReferenceById(1L);
    }

    @Test
    void updateRestoresOldStockBeforeApplyingReplacementQuantities() {
        Products product = product(11L, "Rotor", "10.00", "6.00", 3L);
        Order existing = Order.builder().orderItems(new HashSet<>()).build();
        existing.addOrderItem(OrderItem.builder().product(product).quantity(2)
                .productName("Rotor").productSellingPrice(money("10.00"))
                .productPurchasePrice(money("6.00")).subDiscount(money("0.00")).build());
        when(orderRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        when(productRepo.checkActiveStatus(11L, true)).thenReturn(1);
        when(productRepo.checkActiveStatus(11L, false)).thenReturn(0);
        when(productRepo.findAllByIds(List.of(11L))).thenReturn(List.of(product));
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));

        OrderUpdateDto update = new OrderUpdateDto(money("2.00"), null,
                List.of(new OrderItemCreateDto(11L, 4, money("1.00"), null,
                        null, null, null)), money("10.00"), null, null);

        service.updateOrder(5L, update);

        assertThat(product.getStock()).isEqualTo(1L);
        assertThat(existing.getTotal()).isEqualByComparingTo("37.00");
        assertThat(existing.getRevenue()).isEqualByComparingTo("13.00");
        assertThat(existing.getRemaining()).isEqualByComparingTo("27.00");
        assertThat(existing.getOrderItems()).hasSize(1);
        verify(productRepo).saveAll(any());
        verify(orderRepo).save(existing);
    }

    @Test
    void updateRejectsSaleWithRecordedZeroValueReturn() {
        Order existing = Order.builder().id(5L).orderItems(new HashSet<>())
                .returnCredit(money("0.00")).build();
        when(orderRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(saleReturnRepo.existsByOrder_Id(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateOrder(5L, null))
                .isInstanceOf(com.connectors.pos.exceptions.SaleValidationException.class)
                .hasMessageContaining("cannot be edited");

        verify(orderRepo, never()).save(any());
    }

    @Test
    void updateReassignsCustomerAndClearsItWhenCustomerIdIsNull() {
        Order existing = Order.builder().orderItems(new HashSet<>())
                .customer(Customer.builder().id(3L).name("Old customer").build()).build();
        Customer replacement = Customer.builder().id(8L).name("New customer").build();
        when(orderRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        when(customerRepo.findById(8L)).thenReturn(Optional.of(replacement));
        when(productRepo.findAllByIds(List.of())).thenReturn(List.of());
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));

        OrderUpdateDto withCustomer = new OrderUpdateDto(money("0.00"), 8L,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);
        OrderUpdateDto withoutCustomer = new OrderUpdateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);

        service.updateOrder(5L, withCustomer);
        assertThat(existing.getCustomer()).isSameAs(replacement);

        service.updateOrder(5L, withoutCustomer);
        assertThat(existing.getCustomer()).isNull();
        verify(customerRepo).findById(8L);
        verify(orderRepo, times(2)).save(existing);
    }

    @Test
    void refreshesClosedShiftReconciliationWhenUpdatingItsSale() {
        ShiftSession closedShift = ShiftSession.builder().id(12L)
                .status(com.connectors.pos.shift.ShiftStatus.CLOSED).build();
        Order existing = Order.builder().orderItems(new HashSet<>())
                .shiftSession(closedShift).build();
        when(orderRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        when(productRepo.findAllByIds(List.of())).thenReturn(List.of());
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));
        OrderUpdateDto update = new OrderUpdateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("10.00"), null, null);

        service.updateOrder(5L, update);

        verify(orderRepo).flush();
        verify(shiftService).refreshReconciliationAfterOrderUpdate(12L);
    }

    @Test
    void rejectsOrderCreationWithoutAuthenticatedCashier() {
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null, List.of(),
                money("0.00"), null, null);

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No active cashier");

        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void rejectsAuthenticatedPrincipalThatIsNotAUserPrincipal() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-a-cashier");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null, List.of(),
                money("0.00"), null, null);

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid user token");
        verifyNoInteractions(userRepo, orderRepo);
    }

    @Test
    void rejectsOrderWithNoItemsAfterVerifyingCashier() {
        authenticateAsCashier();
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null, List.of(),
                money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(new Order());
        enableOpenShift();

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(YouMustProvideAtLeastOneItem.class);
        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void rejectsProductQuantityAboveAvailableStock() {
        authenticateAsCashier();
        Products product = product(11L, "Rotor", "10.00", "6.00", 2L);
        when(productRepo.findAllByIds(List.of(11L))).thenReturn(List.of(product));
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(11L, 3, money("0.00"), null,
                        null, null, null)), money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(new Order());
        enableOpenShift();

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(InsuffecientStockException.class);
        assertThat(product.getStock()).isEqualTo(2L);
        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void rejectsSaleWhenShiftManagementIsEnabledAndNoShiftIsOpen() {
        authenticateAsCashier();
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(true));
        when(shiftService.getOpenShiftForSale(1L))
                .thenThrow(new com.connectors.pos.exceptions.ShiftOperationException("No active shift."));
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(new Order());

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(com.connectors.pos.exceptions.ShiftRequiredException.class)
                .hasMessageContaining("Open a shift");
        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void allowsSalesWithoutAnOpenShiftWhenShiftManagementIsDisabled() {
        authenticateAsCashier();
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(new Order());
        when(orderNumberGenerator.nextValue()).thenReturn(1L);
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));

        service.createOrder(request);

        verifyNoInteractions(shiftService);
        verify(orderRepo).save(any(Order.class));
    }

    @Test
    void appliesConfiguredTaxAfterDiscountsAndExcludesTaxFromRevenue() {
        authenticateAsCashier();
        Products product = product(11L, "Rotor", "10.05", "6.00", 4L);
        when(productRepo.findAllByIds(List.of(11L))).thenReturn(List.of(product));
        when(settingsService.getSettings()).thenReturn(settingsWithTaxRate(false, "10.00"));
        OrderCreateDto request = new OrderCreateDto(money("0.95"), null,
                List.of(new OrderItemCreateDto(11L, 1, money("0.05"), null,
                        null, null, null)),
                money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(Order.builder().orderItems(new HashSet<>()).build());
        when(orderNumberGenerator.nextValue()).thenReturn(1L);
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));

        service.createOrder(request);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getTaxRate()).isEqualByComparingTo("10.00");
        assertThat(savedOrder.getValue().getTaxAmount()).isEqualByComparingTo("0.91");
        assertThat(savedOrder.getValue().getTotal()).isEqualByComparingTo("9.96");
        assertThat(savedOrder.getValue().getRevenue()).isEqualByComparingTo("3.05");
    }

    @Test
    void rejectsOverpaymentBeforePersistingSale() {
        authenticateAsCashier();
        enableOpenShift();
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("10.01"), null, null, PaymentMethod.CARD, null);
        when(orderMapper.toEntity(request)).thenReturn(new Order());

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(com.connectors.pos.exceptions.SaleValidationException.class)
                .hasMessageContaining("Payment cannot exceed");
        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void cashOverTenderIsRecordedAsChangeAndOnlySaleTotalIsApplied() {
        authenticateAsCashier();
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("12.00"), null, null, PaymentMethod.CASH, null);
        when(orderMapper.toEntity(request)).thenReturn(Order.builder().orderItems(new HashSet<>()).build());
        when(orderNumberGenerator.nextValue()).thenReturn(1L);
        when(orderRepo.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(mock(OrderResponseDto.class));

        service.createOrder(request);

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        verify(orderRepo).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getPaid()).isEqualByComparingTo("10.00");
        assertThat(savedOrder.getValue().getCashReceived()).isEqualByComparingTo("12.00");
        assertThat(savedOrder.getValue().getCashChange()).isEqualByComparingTo("2.00");
        assertThat(savedOrder.getValue().getPaymentMethod()).isEqualTo(PaymentMethod.CASH);
    }

    @Test
    void requiresAnEWalletProviderWhenRecordingAnEWalletPayment() {
        authenticateAsCashier();
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(false));
        OrderCreateDto request = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("5.00"), null, null, PaymentMethod.E_WALLET, null);
        when(orderMapper.toEntity(request)).thenReturn(Order.builder().orderItems(new HashSet<>()).build());

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(com.connectors.pos.exceptions.SaleValidationException.class)
                .hasMessageContaining("e-wallet provider");

        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void rejectsDiscountsThatExceedTheirSaleLineOrSubtotal() {
        authenticateAsCashier();
        enableOpenShift();
        OrderCreateDto lineDiscount = new OrderCreateDto(money("0.00"), null,
                List.of(new OrderItemCreateDto(null, 1, money("10.01"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);
        when(orderMapper.toEntity(lineDiscount)).thenReturn(new Order());

        assertThatThrownBy(() -> service.createOrder(lineDiscount))
                .isInstanceOf(com.connectors.pos.exceptions.SaleValidationException.class)
                .hasMessageContaining("Line discount");

        Order totalDiscount = new Order();
        OrderCreateDto request = new OrderCreateDto(money("10.01"), null,
                List.of(new OrderItemCreateDto(null, 1, money("0.00"), null,
                        "Custom item", money("10.00"), money("0.00"))),
                money("0.00"), null, null);
        when(orderMapper.toEntity(request)).thenReturn(totalDiscount);

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(com.connectors.pos.exceptions.SaleValidationException.class)
                .hasMessageContaining("Sale discount");
        verifyNoInteractions(orderRepo, orderNumberGenerator);
    }

    @Test
    void readsCustomerSummaryTotalsFromAFlatAggregateResult() {
        LocalDateTime start = LocalDateTime.parse("2026-01-01T00:00:00");
        LocalDateTime end = LocalDateTime.parse("2026-01-31T23:59:59");
        PageRequest pageable = PageRequest.of(0, 10);
        when(orderRepo.findOrdersByCustomerIdBetweenDates(9L, start, end, pageable))
                .thenReturn(Page.empty(pageable));
        when(orderRepo.sumAllOrdersSummaryBetweenDatesById(9L, start, end))
                .thenReturn(new OrderTotals(money("25.00"), money("15.00"), money("10.00")));
        when(customerRepo.findById(9L))
                .thenReturn(Optional.of(Customer.builder().id(9L).name("Sam").build()));

        CustomerSummary summary = service.getCustomerSummaryInPeriodById(9L, start, end, pageable);

        assertThat(summary.name()).isEqualTo("Sam");
        assertThat(summary.total()).isEqualByComparingTo("25.00");
        assertThat(summary.paid()).isEqualByComparingTo("15.00");
        assertThat(summary.remaining()).isEqualByComparingTo("10.00");
        assertThat(summary.orders()).isEmpty();
    }

    private void enableOpenShift() {
        when(settingsService.getSettings()).thenReturn(settingsWithShiftManagement(true));
        when(shiftService.getOpenShiftForSale(1L)).thenReturn(new ShiftSession());
    }

    private static SettingsResponseDto settingsWithShiftManagement(boolean enabled) {
        return new SettingsResponseDto("Demo", null, null, null, Theme.SYSTEM_DEFAULT,
                PrintSize.A4, "EGP", PosStyle.HORIZONTAL, null, enabled);
    }

    private static SettingsResponseDto settingsWithTaxRate(boolean shiftManagement, String taxRate) {
        return new SettingsResponseDto("Demo", null, null, null, Theme.SYSTEM_DEFAULT,
                PrintSize.A4, "EGP", PosStyle.HORIZONTAL, null, shiftManagement, money(taxRate));
    }

    private void authenticateAsCashier() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(1L);
        when(principal.getUsername()).thenReturn("cashier@example.test");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static Products product(Long id, String name, String sellingPrice,
                                    String purchasePrice, Long stock) {
        return Products.builder().id(id).name(name)
                .sellingPrice(money(sellingPrice)).purchasePrice(money(purchasePrice))
                .stock(stock).build();
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
