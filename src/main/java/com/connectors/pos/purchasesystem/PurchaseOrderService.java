package com.connectors.pos.purchasesystem;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderDto;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderItemDto;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseMapper;
import com.connectors.pos.purchasesystem.purchasedtos.PurchaseOrderResponseDto;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
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

        List<Long> prodIds = create.itemsList() .stream()
                .map(CreatePurchaseOrderItemDto::productId)
                .filter(Objects::nonNull).toList();

        List<Products> allProducts = productRepo.findAllByIds(prodIds);

        Map<Long, Products> allProdsWithIds = allProducts.stream()
                .collect(Collectors.toMap(Products::getId, p -> p));

        PurchaseOrder order = PurchaseOrder.builder()
                .discount(create.discount())
                .paid(create.paid())
                .build();

        order.setUser(user);
        order.setUsername(principal.getUsername());

        if (create.supplierId() != null) {
            Vendor vendor = vendorRepo.findById(create.supplierId())
                    .orElseThrow(() -> new EntityNotFoundException("no vendor was found"));
            order.setVendor(vendor);
        }

        BigDecimal total = BigDecimal.ZERO;

        for (CreatePurchaseOrderItemDto item : create. itemsList()) {
            Products prod = allProdsWithIds.get(item.productId());
            if (prod == null) {
                throw new ProductNotFoundException("no prod exist");
            }

            // Increase stock based on purchase
            prod.setStock(prod.getStock() + item.quantity());
            prod.setPurchasePrice(item.customPurchasePrice());

            PurchaseOrderItem pt = PurchaseOrderItem.builder()
                    .quantity(item.quantity())
                    .product(prod)
                    .productName(prod.getName())
                    .subDiscount(item.subDiscount())
                    .productPurchasePrice(item.customPurchasePrice())
                    .build();

            // FIXED MATH: (Price * Quantity) - Discount
            BigDecimal subDiscount = item.subDiscount() != null ? item.subDiscount() : BigDecimal.ZERO;
            BigDecimal lineTotalWithoutDiscount = item.customPurchasePrice().multiply(BigDecimal.valueOf(item.quantity()));
            BigDecimal subTotal = lineTotalWithoutDiscount.subtract(subDiscount);

            total = total.add(subTotal);
            order.addItem(pt);
        }

        order.setTotal(total);
        PurchaseOrder saved = orderRepo.save(order);

        return mapper.toResponse(saved);
    }


    @Transactional
    public void updatePurchaseOrder(Long ordId, CreatePurchaseOrderDto updateDto) {
        PurchaseOrder existingOrder = orderRepo.findById(ordId)
                .orElseThrow(() -> new BusinessRuleException("Purchase order not found"));

        Vendor vendor = vendorRepo.findById(updateDto.supplierId())
                .orElseThrow(() -> new BusinessRuleException("Vendor not found"));

        // 1. Reverse stock changes from the old order items before clearing them
        for (PurchaseOrderItem oldItem : existingOrder.getItems()) {
            if (oldItem.getProduct() != null) {
                Products product = oldItem.getProduct();
                product.setStock(product.getStock() - oldItem.getQuantity());
            }
        }

        // 2. Clear old items safely (requires orphanRemoval = true on the entity mapping)
        existingOrder.getItems().clear();

        existingOrder.setVendor(vendor);
        existingOrder.setDiscount(updateDto.discount() != null ? updateDto.discount() : BigDecimal.ZERO);
        existingOrder.setPaid(updateDto.paid() != null ? updateDto.paid() : BigDecimal.ZERO);

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<PurchaseOrderItem> newOrderItems = new ArrayList<>();

        for (CreatePurchaseOrderItemDto itemDto : updateDto.itemsList()) {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setPurchaseOrder(existingOrder);
            item.setQuantity(itemDto.quantity());
            item.setSubDiscount(itemDto.subDiscount() != null ? itemDto.subDiscount() : BigDecimal.ZERO);

            BigDecimal purchasePrice;

            if (itemDto.productId() != null) {
                Products product = productRepo.findById(itemDto.productId())
                        .orElseThrow(() -> new ProductNotFoundException("Product ID " + itemDto.productId() + " not found"));

                // Add new stock count
                product.setStock(product.getStock() + itemDto.quantity());

                purchasePrice = itemDto.customPurchasePrice() != null ? itemDto.customPurchasePrice() : product.getPurchasePrice();
                product.setPurchasePrice(purchasePrice);

                if (itemDto.customSellingPrice() != null) {
                    product.setSellingPrice(itemDto.customSellingPrice());
                }

                item.setProduct(product);
                item.setProductName(product.getName());
            } else {
                if (itemDto.customName() == null || itemDto.customPurchasePrice() == null) {
                    throw new BusinessRuleException("Custom items must have a name and purchase price");
                }
                item.setProductName(itemDto.customName());
                purchasePrice = itemDto.customPurchasePrice();
            }

            item.setProductPurchasePrice(purchasePrice);

            BigDecimal itemTotal = purchasePrice.multiply(BigDecimal.valueOf(itemDto.quantity()))
                    .subtract(item.getSubDiscount());
            item.setSubTotal(itemTotal);

            newOrderItems.add(item);
            grandTotal = grandTotal.add(itemTotal);
        }

        grandTotal = grandTotal.subtract(existingOrder.getDiscount());
        existingOrder.setTotal(grandTotal);
        existingOrder.setRemaining(grandTotal.subtract(existingOrder.getPaid()));

        existingOrder.getItems().addAll(newOrderItems);

        orderRepo.save(existingOrder);
    }
}