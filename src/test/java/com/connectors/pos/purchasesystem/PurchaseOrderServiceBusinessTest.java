package com.connectors.pos.purchasesystem;

import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.exceptions.PurchaseOrderException;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderDto;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderItemDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseMapper;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseOrderResponseDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseReturnRequest;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderServiceBusinessTest {

    @Mock private PurchaseOrderRepository orderRepo;
    @Mock private ProductRepository productRepo;
    @Mock private VendorRepository vendorRepo;
    @Mock private PurchaseMapper mapper;
    @Mock private UserRepository userRepo;
    @Mock private PurchaseReturnRepository returnRepo;
    @Mock private Authentication authentication;

    private PurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new PurchaseOrderService(orderRepo, productRepo, vendorRepo, mapper, userRepo, returnRepo);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsPurchaseOrderWithLineDiscountTotalsAndIncreasedStock() {
        authenticateAsCashier();
        Products first = product(1L, "Bearing", "4.00", 2L);
        Products second = product(2L, "Belt", "5.00", 1L);
        when(productRepo.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(first, second));
        CreatePurchaseOrderDto request = new CreatePurchaseOrderDto(money("3.00"), null,
                List.of(
                        item(1L, 2, "1.00", "4.00"),
                        item(2L, 3, "2.00", "5.00")),
                money("10.00"), null, null);
        when(orderRepo.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PurchaseOrderResponseDto response = mock(PurchaseOrderResponseDto.class);
        when(mapper.toResponse(any(PurchaseOrder.class))).thenReturn(response);

        assertThat(service.createPurchaseOrder(request)).isSameAs(response);

        ArgumentCaptor<PurchaseOrder> saved = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(orderRepo).save(saved.capture());
        PurchaseOrder order = saved.getValue();
        assertThat(order.getTotal()).isEqualByComparingTo("20.00");
        assertThat(order.getDiscount()).isEqualByComparingTo("3.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("7.00");
        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems()).extracting(PurchaseOrderItem::getQuantity)
                .containsExactlyInAnyOrder(2, 3);
        assertThat(order.getItems()).extracting(PurchaseOrderItem::getProductPurchasePrice)
                .containsExactlyInAnyOrder(money("4.00"), money("5.00"));
        assertThat(order.getItems()).allSatisfy(item ->
                assertThat(item.getPurchaseOrder()).isSameAs(order));
        assertThat(first.getStock()).isEqualTo(4L);
        assertThat(second.getStock()).isEqualTo(4L);
        assertThat(first.getPurchasePrice()).isEqualByComparingTo("4.00");
        assertThat(second.getPurchasePrice()).isEqualByComparingTo("5.00");
    }

    @Test
    void rejectsEditingAPurchaseOrderAlreadyPostedToInventory() {
        Products product = product(1L, "Bearing", "2.00", 7L);
        when(orderRepo.existsById(5L)).thenReturn(true);
        CreatePurchaseOrderDto update = new CreatePurchaseOrderDto(money("2.00"), 9L,
                List.of(item(1L, 4, "1.00", "3.00")),
                money("5.00"), null, null);

        assertThatThrownBy(() -> service.updatePurchaseOrder(5L, update))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining("cannot be edited");

        assertThat(product.getStock()).isEqualTo(7L);
        assertThat(product.getPurchasePrice()).isEqualByComparingTo("2.00");
        verifyNoInteractions(productRepo, vendorRepo);
        verify(orderRepo, never()).save(any());
    }

    @Test
    void computesWeightedAverageCostFromExistingStockAndNewReceipt() {
        authenticateAsCashier();
        Products product = product(1L, "Bearing", "5.00", 10L);
        when(productRepo.findAllByIds(List.of(1L))).thenReturn(List.of(product));
        CreatePurchaseOrderDto receipt = new CreatePurchaseOrderDto(money("0.00"), null,
                List.of(item(1L, 5, "0.00", "8.00")),
                money("0.00"), null, null);
        when(orderRepo.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(PurchaseOrder.class)))
                .thenReturn(mock(PurchaseOrderResponseDto.class));

        service.createPurchaseOrder(receipt);

        assertThat(product.getStock()).isEqualTo(15L);
        assertThat(product.getPurchasePrice()).isEqualByComparingTo("6.00");
    }

    @Test
    void createsVendorlessPurchaseWithCustomItemWithoutChangingInventory() {
        authenticateAsCashier();
        when(productRepo.findAllByIds(List.of())).thenReturn(List.of());
        CreatePurchaseOrderDto request = new CreatePurchaseOrderDto(money("2.00"), null,
                List.of(new CreatePurchaseOrderItemDto(null, 2, money("1.00"), null,
                        "Freight", null, money("5.00"))),
                money("3.00"), null, null);
        when(orderRepo.save(any(PurchaseOrder.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(PurchaseOrder.class)))
                .thenReturn(mock(PurchaseOrderResponseDto.class));

        service.createPurchaseOrder(request);

        ArgumentCaptor<PurchaseOrder> saved = ArgumentCaptor.forClass(PurchaseOrder.class);
        verify(orderRepo).save(saved.capture());
        PurchaseOrder order = saved.getValue();
        assertThat(order.getVendor()).isNull();
        assertThat(order.getTotal()).isEqualByComparingTo("9.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("4.00");
        assertThat(order.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProduct()).isNull();
            assertThat(item.getProductName()).isEqualTo("Freight");
            assertThat(item.getSubTotal()).isEqualByComparingTo("9.00");
        });
    }

    @Test
    void rejectsLineDiscountGreaterThanItsExtendedPurchasePrice() {
        authenticateAsCashier();
        when(productRepo.findAllByIds(List.of())).thenReturn(List.of());
        CreatePurchaseOrderDto request = new CreatePurchaseOrderDto(money("0.00"), null,
                List.of(new CreatePurchaseOrderItemDto(null, 1, money("11.00"), null,
                        "Freight", null, money("10.00"))),
                money("0.00"), null, null);

        assertThatThrownBy(() -> service.createPurchaseOrder(request))
                .hasMessageContaining("Line discount cannot exceed");
        verify(orderRepo, never()).save(any());
    }

    @Test
    void rejectsPurchaseOrderCreationWithoutAuthenticatedCashier() {
        CreatePurchaseOrderDto request = new CreatePurchaseOrderDto(money("0.00"), null,
                List.of(), money("0.00"), null, null);

        assertThatThrownBy(() -> service.createPurchaseOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No active cashier");

        verifyNoInteractions(orderRepo, productRepo, userRepo);
    }

    @Test
    void rejectsAuthenticatedPrincipalThatIsNotAUserPrincipal() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymous-token");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        CreatePurchaseOrderDto request = new CreatePurchaseOrderDto(money("0.00"), null,
                List.of(), money("0.00"), null, null);

        assertThatThrownBy(() -> service.createPurchaseOrder(request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid user token");
        verifyNoInteractions(userRepo, orderRepo);
    }

    @Test
    void recordsVendorReturnWithProportionalCreditAndReducesStock() {
        authenticateAsCashier();
        Products product = product(1L, "Bearing", "10.00", 8L);
        PurchaseOrder order = purchaseOrderWithVendorAndLine(product, 4, "10.00", "4.00");
        Users user = mock(Users.class);
        when(userRepo.getReferenceById(2L)).thenReturn(user);
        when(orderRepo.findById(5L)).thenReturn(Optional.of(order));
        when(productRepo.findAllByIds(List.of(1L))).thenReturn(List.of(product));
        when(returnRepo.sumReturnedQuantity(11L)).thenReturn(0L);
        when(returnRepo.save(any(PurchaseReturn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseReturn result = service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 2, "Damaged packaging"));

        assertThat(result.getReason()).isEqualTo("Damaged packaging");
        assertThat(result.getCreditTotal()).isEqualByComparingTo("18.00");
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getPurchaseOrderItem().getId()).isEqualTo(11L);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitCredit()).isEqualByComparingTo("9.00");
            assertThat(item.getCreditTotal()).isEqualByComparingTo("18.00");
        });
        assertThat(result.getPurchaseOrder()).isSameAs(order);
        assertThat(result.getUser()).isSameAs(user);
        assertThat(product.getStock()).isEqualTo(6L);
        assertThat(order.getVendorCredit()).isEqualByComparingTo("18.00");
        assertThat(order.getRemaining()).isEqualByComparingTo("18.00");
        verify(returnRepo).save(any(PurchaseReturn.class));
    }

    @Test
    void finalPartialCentReturnUsesRemainderSoCreditMatchesDiscountedLineTotal() {
        authenticateAsCashier();
        Products product = product(1L, "Bearing", "10.00", 8L);
        PurchaseOrder order = purchaseOrderWithVendorAndLine(product, 3, "10.00", "1.00");
        when(userRepo.getReferenceById(2L)).thenReturn(mock(Users.class));
        when(orderRepo.findById(5L)).thenReturn(Optional.of(order));
        when(productRepo.findAllByIds(List.of(1L))).thenReturn(List.of(product));
        when(returnRepo.sumReturnedQuantity(11L)).thenReturn(1L);
        when(returnRepo.sumReturnedCredit(11L)).thenReturn(money("9.67"));
        when(returnRepo.save(any(PurchaseReturn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseReturn result = service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 2, "Return remaining units"));

        assertThat(result.getCreditTotal()).isEqualByComparingTo("19.33");
        assertThat(result.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getUnitCredit()).isEqualByComparingTo("9.67");
            assertThat(item.getCreditTotal()).isEqualByComparingTo("19.33");
        });
        assertThat(order.getVendorCredit()).isEqualByComparingTo("19.33");
        assertThat(product.getStock()).isEqualTo(6L);
    }

    @Test
    void vendorCreditIncludesAllocatedPurchaseOrderDiscount() {
        authenticateAsCashier();
        Products product = product(1L, "Bearing", "10.00", 8L);
        PurchaseOrder order = purchaseOrderWithVendorAndLine(product, 4, "10.00", "4.00");
        order.setDiscount(money("3.00"));
        when(orderRepo.findById(5L)).thenReturn(Optional.of(order));
        when(productRepo.findAllByIds(List.of(1L))).thenReturn(List.of(product));
        when(returnRepo.sumReturnedQuantity(11L)).thenReturn(0L);
        when(returnRepo.save(any(PurchaseReturn.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseReturn result = service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 2, "Damaged packaging"));

        assertThat(result.getCreditTotal()).isEqualByComparingTo("16.50");
        assertThat(order.getVendorCredit()).isEqualByComparingTo("16.50");
    }

    @Test
    void rejectsReturnsWithoutVendorOrBeyondPurchasedQuantityOrCurrentStock() {
        authenticateWithoutUserLookup();
        Products product = product(1L, "Bearing", "10.00", 1L);
        PurchaseOrder order = purchaseOrderWithVendorAndLine(product, 2, "10.00", "0.00");
        when(orderRepo.findById(5L)).thenReturn(Optional.of(order));
        when(productRepo.findAllByIds(List.of(1L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 2, "Not enough stock")))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining("current available stock");

        product.setStock(5L);
        when(returnRepo.sumReturnedQuantity(11L)).thenReturn(1L);
        assertThatThrownBy(() -> service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 2, "Exceeds receipt")))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining("exceeds the quantity");

        order.setVendor(null);
        assertThatThrownBy(() -> service.processVendorReturn(5L,
                new PurchaseReturnRequest(11L, 1, "Missing vendor")))
                .isInstanceOf(PurchaseOrderException.class)
                .hasMessageContaining("linked to a vendor");

        verify(returnRepo, never()).save(any());
    }

    private void authenticateAsCashier() {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getId()).thenReturn(2L);
        setAuthenticatedPrincipal(principal);
    }

    private void authenticateWithoutUserLookup() {
        setAuthenticatedPrincipal(mock(UserPrincipal.class));
    }

    private void setAuthenticatedPrincipal(UserPrincipal principal) {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private static CreatePurchaseOrderItemDto item(Long productId, int quantity,
                                                    String discount, String purchasePrice) {
        return new CreatePurchaseOrderItemDto(productId, quantity, money(discount),
                null, null, null, money(purchasePrice));
    }

    private static Products product(Long id, String name, String purchasePrice, Long stock) {
        return Products.builder().id(id).name(name).purchasePrice(money(purchasePrice))
                .stock(stock).build();
    }

    private static PurchaseOrder purchaseOrderWithVendorAndLine(Products product, int quantity,
                                                                 String purchasePrice, String discount) {
        PurchaseOrder order = PurchaseOrder.builder()
                .id(5L)
                .username("buyer@example.test")
                .total(money(purchasePrice).multiply(BigDecimal.valueOf(quantity)).subtract(money(discount)))
                .discount(BigDecimal.ZERO)
                .paid(BigDecimal.ZERO)
                .vendor(Vendor.builder().id(3L).name("Supplier").build())
                .vendorCredit(BigDecimal.ZERO)
                .build();
        order.addItem(PurchaseOrderItem.builder()
                .id(11L)
                .product(product)
                .productName(product.getName())
                .quantity(quantity)
                .productPurchasePrice(money(purchasePrice))
                .subDiscount(money(discount))
                .build());
        return order;
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
