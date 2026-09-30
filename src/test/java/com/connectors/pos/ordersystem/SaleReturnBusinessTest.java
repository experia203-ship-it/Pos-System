package com.connectors.pos.ordersystem;

import com.connectors.pos.exceptions.SaleValidationException;
import com.connectors.pos.ordersystem.orderdtos.OrderItemMapper;
import com.connectors.pos.ordersystem.orderdtos.OrderMapper;
import com.connectors.pos.ordersystem.orderdtos.SaleReturnRequest;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Roles;
import com.connectors.pos.users.Users;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaleReturnBusinessTest {

    @Mock private OrderMapper orderMapper;
    @Mock private OrderItemMapper orderItemMapper;
    @Mock private UserRepository userRepo;
    @Mock private com.connectors.pos.customersystem.CustomerRepository customerRepo;
    @Mock private ProductRepository productRepo;
    @Mock private OrderItemRepository orderItemRepo;
    @Mock private OrderRepository orderRepo;
    @Mock private OrderNumberGenerator orderNumberGenerator;
    @Mock private com.connectors.pos.settings.SettingsService settingsService;
    @Mock private com.connectors.pos.shift.ShiftService shiftService;
    @Mock private SaleReturnRepository saleReturnRepo;

    private OrderService service;
    private Users user;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderMapper, orderItemMapper, userRepo, customerRepo,
                productRepo, orderItemRepo, orderRepo, orderNumberGenerator, settingsService,
                shiftService, saleReturnRepo);
        user = Users.builder().id(9L).name("Admin").email("admin@example.test")
                .roles(Set.of(Roles.builder().name("ROLE_ADMIN").build())).build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(new UserPrincipal(user), null, "ROLE_ADMIN"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void appliesReturnCreditToBalanceBeforeRefundingOriginalTender() {
        stubAdministratorReference();
        Products product = productWithStock(3L);
        Order order = saleWithProduct(product, 2, "20.00", "15.00");
        when(orderRepo.findForUpdateById(1L)).thenReturn(Optional.of(order));
        when(saleReturnRepo.sumReturnedQuantity(31L)).thenReturn(0L);
        when(saleReturnRepo.sumReturnedCredit(31L)).thenReturn(money("0.00"));
        when(productRepo.findAllByIds(List.of(10L))).thenReturn(List.of(product));
        when(saleReturnRepo.save(any(SaleReturn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleReturn result = service.recordSaleReturn(1L,
                new SaleReturnRequest(31L, 1, true, "Damaged"));

        assertThat(result.getCreditTotal()).isEqualByComparingTo("10.00");
        assertThat(result.getRefundTotal()).isEqualByComparingTo("5.00");
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.CARD);
        assertThat(result.getPaymentReference()).isEqualTo("terminal-42");
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getQuantity()).isEqualTo(1);
            assertThat(item.isRestocked()).isTrue();
        });
        assertThat(order.getReturnCredit()).isEqualByComparingTo("10.00");
        assertThat(order.getRefundedTotal()).isEqualByComparingTo("5.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("0.00");
        assertThat(product.getStock()).isEqualTo(4L);
    }

    @Test
    void doesNotRefundWhenReturnCreditDoesNotExceedUnpaidBalance() {
        stubAdministratorReference();
        Products product = productWithStock(3L);
        Order order = saleWithProduct(product, 2, "20.00", "5.00");
        when(orderRepo.findForUpdateById(1L)).thenReturn(Optional.of(order));
        when(saleReturnRepo.sumReturnedQuantity(31L)).thenReturn(0L);
        when(saleReturnRepo.sumReturnedCredit(31L)).thenReturn(money("0.00"));
        when(saleReturnRepo.save(any(SaleReturn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleReturn result = service.recordSaleReturn(1L,
                new SaleReturnRequest(31L, 1, false, "Customer changed mind"));

        assertThat(result.getCreditTotal()).isEqualByComparingTo("10.00");
        assertThat(result.getRefundTotal()).isEqualByComparingTo("0.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("5.00");
        assertThat(order.getRefundedTotal()).isEqualByComparingTo("0.00");
        assertThat(product.getStock()).isEqualTo(3L);
        verify(productRepo, never()).findAllByIds(any());
    }

    @Test
    void voidsSaleAndRestocksOnlyUnitsNotPreviouslyReturned() {
        stubAdministratorReference();
        Products product = productWithStock(3L);
        Order order = saleWithProduct(product, 2, "20.00", "15.00");
        when(orderRepo.findForUpdateById(1L)).thenReturn(Optional.of(order));
        when(saleReturnRepo.sumReturnedQuantity(31L)).thenReturn(0L);
        when(saleReturnRepo.sumReturnedCredit(31L)).thenReturn(money("0.00"));
        when(productRepo.findAllByIds(List.of(10L))).thenReturn(List.of(product));
        when(saleReturnRepo.save(any(SaleReturn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SaleReturn result = service.voidSale(1L, "Duplicate sale");

        assertThat(result.getReturnType()).isEqualTo(SaleReturnType.VOID);
        assertThat(result.getCreditTotal()).isEqualByComparingTo("20.00");
        assertThat(result.getRefundTotal()).isEqualByComparingTo("15.00");
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.isRestocked()).isTrue();
        });
        assertThat(order.isVoided()).isTrue();
        assertThat(order.getRemaining()).isEqualByComparingTo("0.00");
        assertThat(product.getStock()).isEqualTo(5L);
    }

    @Test
    void rejectsReturningMoreThanRemainingQuantity() {
        stubAdministratorReference();
        Products product = productWithStock(3L);
        Order order = saleWithProduct(product, 2, "20.00", "20.00");
        when(orderRepo.findForUpdateById(1L)).thenReturn(Optional.of(order));
        when(saleReturnRepo.sumReturnedQuantity(31L)).thenReturn(1L);

        assertThatThrownBy(() -> service.recordSaleReturn(1L,
                new SaleReturnRequest(31L, 2, false, "Over return")))
                .isInstanceOf(SaleValidationException.class)
                .hasMessageContaining("exceeds");

        verify(saleReturnRepo, never()).save(any());
    }

    @Test
    void rejectsSaleReturnWhenAuthenticatedUserIsNotAdministrator() {
        Users cashier = Users.builder().id(10L).email("cashier@example.test").build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(new UserPrincipal(cashier), null, "ROLE_USER"));

        assertThatThrownBy(() -> service.recordSaleReturn(1L,
                new SaleReturnRequest(31L, 1, false, "Reason")))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(orderRepo, never()).findForUpdateById(any());
    }

    @Test
    void exposesReturnAuditHistoryWithActorReasonAndItemDetails() {
        OrderItem orderItem = OrderItem.builder().productName("Widget").build();
        SaleReturn audit = SaleReturn.builder()
                .order(Order.builder().id(1L).build())
                .user(user)
                .returnType(SaleReturnType.RETURN)
                .reason("Damaged")
                .creditTotal(money("10.00"))
                .refundTotal(money("5.00"))
                .paymentMethod(PaymentMethod.CARD)
                .paymentReference("terminal-42")
                .createdAt(LocalDateTime.of(2026, 1, 2, 9, 30))
                .build();
        audit.addItem(SaleReturnItem.builder().orderItem(orderItem).quantity(1)
                .restocked(true).creditTotal(money("10.00")).unitCredit(money("10.00")).build());
        when(orderRepo.existsById(1L)).thenReturn(true);
        when(saleReturnRepo.findByOrder_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(audit));

        var history = service.getSaleReturnHistory(1L);

        assertThat(history).singleElement().satisfies(row -> {
            assertThat(row.reason()).isEqualTo("Damaged");
            assertThat(row.userName()).isEqualTo("Admin");
            assertThat(row.itemName()).isEqualTo("Widget x1");
            assertThat(row.restocked()).isTrue();
            assertThat(row.credit()).isEqualByComparingTo("10.00");
            assertThat(row.refund()).isEqualByComparingTo("5.00");
            assertThat(row.paymentMethod()).isEqualTo(PaymentMethod.CARD);
        });
    }

    private static Order saleWithProduct(Products product, int quantity, String total, String paid) {
        Order order = Order.builder()
                .id(1L)
                .total(money(total))
                .discount(money("0.00"))
                .taxAmount(money("0.00"))
                .paid(money(paid))
                .paymentMethod(PaymentMethod.CARD)
                .paymentReference("terminal-42")
                .returnCredit(money("0.00"))
                .refundedTotal(money("0.00"))
                .orderItems(new HashSet<>())
                .build();
        order.addOrderItem(OrderItem.builder()
                .id(31L)
                .product(product)
                .productName("Widget")
                .quantity(quantity)
                .productSellingPrice(money("10.00"))
                .productPurchasePrice(money("5.00"))
                .subDiscount(money("0.00"))
                .build());
        return order;
    }

    private static Products productWithStock(long stock) {
        return Products.builder().id(10L).name("Widget").stock(stock).build();
    }

    private void stubAdministratorReference() {
        when(userRepo.getReferenceById(9L)).thenReturn(user);
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
