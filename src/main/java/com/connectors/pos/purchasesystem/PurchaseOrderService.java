package com.connectors.pos.purchasesystem;

import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.exceptions.PurchaseOrderException;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderDto;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderItemDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseMapper;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseOrderResponseDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseReturnLineOption;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseReturnRequest;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final VendorRepository vendorRepo;
    private final PurchaseMapper mapper;
    private final UserRepository userRepo;
    private final PurchaseReturnRepository returnRepo;

    @Transactional
    public PurchaseOrderResponseDto createPurchaseOrder(CreatePurchaseOrderDto create) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized: No active cashier session found.");
        }
        if (!(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Unauthorized: Invalid user token.");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Users user = userRepo.getReferenceById(principal.getId());
        List<CreatePurchaseOrderItemDto> items = requireItems(create.itemsList());
        BigDecimal discount = nonNegative(create.discount(), "Purchase discount cannot be negative.");
        BigDecimal paid = nonNegative(create.paid(), "Paid amount cannot be negative.");

        List<Long> prodIds = items.stream()
                .map(CreatePurchaseOrderItemDto::productId)
                .filter(Objects::nonNull).toList();

        List<Products> allProducts = productRepo.findAllByIds(prodIds);

        Map<Long, Products> allProdsWithIds = allProducts.stream()
                .collect(Collectors.toMap(Products::getId, p -> p));
        Map<Long, BigDecimal> previousUnitCosts = new HashMap<>();
        for (Products product : allProducts) {
            previousUnitCosts.put(product.getId(), product.getPurchasePrice());
        }
        Map<Long, Long> receivedQuantities = new HashMap<>();
        Map<Long, BigDecimal> receivedValues = new HashMap<>();

        PurchaseOrder order = PurchaseOrder.builder()
                .discount(discount)
                .paid(paid)
                .build();

        order.setUser(user);
        order.setUsername(principal.getUsername());

        order.setVendor(findVendor(create.supplierId()));

        BigDecimal total = BigDecimal.ZERO;

        for (CreatePurchaseOrderItemDto item : items) {
            Products product = findProduct(item.productId(), allProdsWithIds);
            BigDecimal purchasePrice = purchasePrice(item, product,
                    product == null ? null : previousUnitCosts.get(product.getId()));
            BigDecimal subDiscount = nonNegative(item.subDiscount(), "Line discount cannot be negative.");
            BigDecimal subTotal = calculateLineTotal(purchasePrice, item.quantity(), subDiscount);
            String itemName = product == null ? requireCustomName(item.customName())
                    : (item.customName() == null || item.customName().isBlank()
                    ? product.getName() : item.customName().trim());

            if (product != null) {
                receivedQuantities.merge(product.getId(), (long) item.quantity(), Math::addExact);
                receivedValues.merge(product.getId(),
                        purchasePrice.multiply(BigDecimal.valueOf(item.quantity())), BigDecimal::add);
                if (item.customSellingPrice() != null) {
                    if (item.customSellingPrice().signum() <= 0) {
                        throw new PurchaseOrderException("Product selling price must be greater than zero.");
                    }
                    product.setSellingPrice(item.customSellingPrice());
                }
            }

            order.addItem(PurchaseOrderItem.builder()
                    .quantity(item.quantity())
                    .product(product)
                    .productName(itemName)
                    .subTotal(subTotal)
                    .subDiscount(subDiscount)
                    .productPurchasePrice(purchasePrice)
                    .build());
            total = total.add(subTotal);
        }

        applyWeightedAverageCost(allProdsWithIds, receivedQuantities, receivedValues);
        validateOrderDiscount(discount, total);
        order.setTotal(total);
        order.setRemaining(total.subtract(discount).subtract(paid));
        PurchaseOrder saved = orderRepo.save(order);

        return mapper.toResponse(saved);
    }


    @Transactional
    public void updatePurchaseOrder(Long ordId, CreatePurchaseOrderDto updateDto) {
        if (!orderRepo.existsById(ordId)) {
            throw new PurchaseOrderException("Purchase order not found.");
        }
        throw new PurchaseOrderException(
                "Posted purchase orders cannot be edited. Record a correcting transaction instead.");
    }

    @Transactional
    public PurchaseReturn processVendorReturn(Long orderId, PurchaseReturnRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("An administrator session is required to record a vendor return.");
        }

        PurchaseOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new PurchaseOrderException("Purchase order not found."));
        if (order.getVendor() == null) {
            throw new PurchaseOrderException("Vendor returns require a purchase order linked to a vendor.");
        }
        if (request == null || request.purchaseItemId() == null || request.quantity() == null
                || request.quantity() <= 0 || request.reason() == null || request.reason().isBlank()) {
            throw new PurchaseOrderException("A valid item, positive quantity, and return reason are required.");
        }

        PurchaseOrderItem purchaseItem = order.getItems().stream()
                .filter(item -> item.getId().equals(request.purchaseItemId()))
                .findFirst()
                .orElseThrow(() -> new PurchaseOrderException("The selected item is not on this purchase order."));
        if (purchaseItem.getProduct() == null) {
            throw new PurchaseOrderException("Custom purchase items cannot be returned to inventory.");
        }
        Products product = productRepo.findAllByIds(List.of(purchaseItem.getProduct().getId())).stream()
                .filter(candidate -> candidate.getId().equals(purchaseItem.getProduct().getId()))
                .findFirst()
                .orElseThrow(() -> new PurchaseOrderException("The returned product is no longer available."));

        long alreadyReturned = returnRepo.sumReturnedQuantity(purchaseItem.getId());
        long returnableQuantity = purchaseItem.getQuantity() - alreadyReturned;
        if (request.quantity() > returnableQuantity) {
            throw new PurchaseOrderException("Return quantity exceeds the quantity remaining from this purchase.");
        }
        if (product.getStock() == null || request.quantity() > product.getStock()) {
            throw new PurchaseOrderException("Returned products cannot exceed the current available stock.");
        }

        BigDecimal originalLineCredit = returnCreditForLine(order, purchaseItem);
        BigDecimal creditTotal;
        if (request.quantity() == returnableQuantity) {
            creditTotal = originalLineCredit.subtract(
                    zeroIfNull(returnRepo.sumReturnedCredit(purchaseItem.getId())));
        } else {
            BigDecimal unitCredit = originalLineCredit.divide(
                    BigDecimal.valueOf(purchaseItem.getQuantity()), 2, RoundingMode.HALF_UP);
            creditTotal = unitCredit.multiply(BigDecimal.valueOf(request.quantity()));
        }
        if (creditTotal.signum() <= 0) {
            throw new PurchaseOrderException("The selected purchase line has no remaining supplier credit value.");
        }

        PurchaseReturn vendorReturn = PurchaseReturn.builder()
                .purchaseOrder(order)
                .user(userRepo.getReferenceById(principal.getId()))
                .reason(request.reason().trim())
                .creditTotal(creditTotal)
                .build();
        BigDecimal unitCredit = creditTotal.divide(
                BigDecimal.valueOf(request.quantity()), 2, RoundingMode.HALF_UP);
        vendorReturn.addItem(PurchaseReturnItem.builder()
                .purchaseOrderItem(purchaseItem)
                .quantity(request.quantity())
                .unitCredit(unitCredit)
                .creditTotal(creditTotal)
                .build());

        product.setStock(product.getStock() - request.quantity());
        order.setVendorCredit(zeroIfNull(order.getVendorCredit()).add(creditTotal));
        return returnRepo.save(vendorReturn);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponseDto> viewPurchaseOrders(Pageable pageable) {
        return orderRepo.findAll(pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturnLineOption> getReturnableItems(Long orderId) {
        PurchaseOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new PurchaseOrderException("Purchase order not found."));
        if (order.getVendor() == null) {
            throw new PurchaseOrderException("Vendor returns require a purchase order linked to a vendor.");
        }
        return order.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getQuantity() > 0)
                .map(item -> {
                    long returned = returnRepo.sumReturnedQuantity(item.getId());
                    long remaining = item.getQuantity() - returned;
                    BigDecimal lineValue = returnCreditForLine(order, item);
                    BigDecimal unitCredit = lineValue.divide(
                            BigDecimal.valueOf(item.getQuantity()), 2, RoundingMode.HALF_UP);
                    return new PurchaseReturnLineOption(item.getId(), item.getProductName(),
                            item.getQuantity(), returned, remaining, unitCredit);
                })
                .filter(item -> item.quantityReturnable() > 0)
                .toList();
    }

    private Vendor findVendor(Long vendorId) {
        return vendorId == null ? null : vendorRepo.findById(vendorId)
                .orElseThrow(() -> new PurchaseOrderException("Vendor not found."));
    }

    private static List<CreatePurchaseOrderItemDto> requireItems(List<CreatePurchaseOrderItemDto> items) {
        if (items == null || items.isEmpty()) {
            throw new PurchaseOrderException("At least one purchase item is required.");
        }
        for (CreatePurchaseOrderItemDto item : items) {
            if (item == null || item.quantity() <= 0) {
                throw new PurchaseOrderException("Purchase item quantity must be at least one.");
            }
        }
        return items;
    }

    private static Products findProduct(Long productId, Map<Long, Products> products) {
        if (productId == null) {
            return null;
        }
        Products product = products.get(productId);
        if (product == null) {
            throw new ProductNotFoundException("Product ID " + productId + " not found.");
        }
        return product;
    }

    private static BigDecimal purchasePrice(CreatePurchaseOrderItemDto item, Products product,
                                            BigDecimal previousUnitCost) {
        BigDecimal price = item.customPurchasePrice() != null
                ? item.customPurchasePrice()
                : (product == null ? null : previousUnitCost);
        price = nonNegative(price, "Purchase price is required and cannot be negative.");
        if (product != null && price.signum() == 0) {
            throw new PurchaseOrderException("Product purchase price must be greater than zero.");
        }
        return price;
    }

    private static BigDecimal calculateLineTotal(BigDecimal unitPrice, int quantity, BigDecimal discount) {
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount.compareTo(lineTotal) > 0) {
            throw new PurchaseOrderException("Line discount cannot exceed the line total.");
        }
        return lineTotal.subtract(discount);
    }

    private static String requireCustomName(String name) {
        if (name == null || name.isBlank()) {
            throw new PurchaseOrderException("Custom purchase items require a name.");
        }
        return name.trim();
    }

    private static BigDecimal nonNegative(BigDecimal value, String message) {
        if (value == null || value.signum() < 0) {
            throw new PurchaseOrderException(message);
        }
        return value;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal returnCreditForLine(PurchaseOrder order, PurchaseOrderItem item) {
        BigDecimal lineValue = item.getProductPurchasePrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()))
                .subtract(zeroIfNull(item.getSubDiscount()));
        BigDecimal orderTotal = zeroIfNull(order.getTotal());
        BigDecimal orderDiscount = zeroIfNull(order.getDiscount());
        if (orderTotal.signum() <= 0 || orderDiscount.signum() == 0) {
            return lineValue;
        }
        BigDecimal allocatedOrderDiscount = orderDiscount.multiply(lineValue)
                .divide(orderTotal, 2, RoundingMode.HALF_UP);
        return lineValue.subtract(allocatedOrderDiscount).max(BigDecimal.ZERO);
    }

    private static void validateOrderDiscount(BigDecimal discount, BigDecimal total) {
        if (discount.compareTo(total) > 0) {
            throw new PurchaseOrderException("Order discount cannot exceed the purchase total.");
        }
    }

    private static void applyWeightedAverageCost(Map<Long, Products> products,
                                                 Map<Long, Long> receivedQuantities,
                                                 Map<Long, BigDecimal> receivedValues) {
        for (Map.Entry<Long, Long> received : receivedQuantities.entrySet()) {
            Products product = products.get(received.getKey());
            Long currentStock = product.getStock();
            if (currentStock == null || currentStock < 0) {
                throw new PurchaseOrderException("Product stock is invalid.");
            }
            long updatedStock;
            try {
                updatedStock = Math.addExact(currentStock, received.getValue());
            } catch (ArithmeticException exception) {
                throw new PurchaseOrderException("Received quantity exceeds the available stock range.");
            }

            BigDecimal currentValue = currentStock == 0
                    ? BigDecimal.ZERO
                    : nonNegative(product.getPurchasePrice(), "Current product cost is missing.")
                    .multiply(BigDecimal.valueOf(currentStock));
            BigDecimal updatedValue = currentValue.add(receivedValues.get(received.getKey()));
            BigDecimal averageCost = updatedValue.divide(
                    BigDecimal.valueOf(updatedStock), 2, RoundingMode.HALF_UP);
            product.setStock(updatedStock);
            product.setPurchasePrice(averageCost);
        }
    }
}