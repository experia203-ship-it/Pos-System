package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.exceptions.*;
import com.connectors.pos.ordersystem.orderdtos.*;
import com.connectors.pos.ordersystem.orderdtos.SaleReturnLineOption;
import com.connectors.pos.ordersystem.orderdtos.SaleReturnRequest;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.settings.SettingsService;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.shift.ShiftService;
import com.connectors.pos.shift.ShiftSession;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
public class OrderService {


    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;
    private final OrderRepository orderRepo;
    private final OrderNumberGenerator orderNumberGenerator;
    private final SettingsService settingsService;
    private final ShiftService shiftService;
    private final SaleReturnRepository saleReturnRepo;


    @Transactional
    public OrderResponseDto createOrder(OrderCreateDto create) {

        Order order = orderMapper.toEntity(create);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Unauthorized: No active cashier session found.");

        }
        if (!(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Unauthorized: Invalid user token.");

        }
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        Users user = userRepo.getReferenceById(principal.getId());
        order.setUser(user);
        order.setUserName(principal.getUsername());
        SettingsResponseDto currentSettings = settingsService.getSettings();
        if (currentSettings.shiftManagement()) {
            ShiftSession activeShift;
            try {
                activeShift = shiftService.getOpenShiftForSale(principal.getId());
            } catch (ShiftOperationException exception) {
                throw new ShiftRequiredException("Open a shift before completing a sale.");
            }
            order.setShiftSession(activeShift);
        }

        Long customerId = create.customerId();
        if(customerId !=null) {

            Customer customer = customerRepo.getReferenceById(customerId);

            order.setCustomer(customer);
        }
        List<OrderItemCreateDto> itemsDto = create.itemsList();
if(create.itemsList() == null || create.itemsList().isEmpty()){
    throw new YouMustProvideAtLeastOneItem("please provide at least on item");

}
        List<Long> allProductIds = itemsDto.stream().map(OrderItemCreateDto::productId)
                .toList();

        List<Products> allProducts = productRepo.findAllByIds(allProductIds);

        Map<Long, Products> productsWithIds = allProducts.stream().
                collect(Collectors.toMap(Products::getId, p -> p));


        BigDecimal total = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal orderDiscount = nonNegative(create.discount(), "Sale discount cannot be negative.");

        for (OrderItemCreateDto item : itemsDto) {
            String name;
            BigDecimal purchasePrice;
            BigDecimal sellingPrice;
            Products prod = null;
            Long stock;
            BigDecimal discount = nonNegative(item.subDiscount(), "Line discount cannot be negative.");
            Long prodId = item.productId();
            int quantity = item.quantity();
            requirePositiveQuantity(quantity);
            if (prodId == null) {
                name = requireCustomName(item.customName());
                sellingPrice = nonNegative(item.customSellingPrice(), "Custom item selling price is required.");
                purchasePrice = item.customPurchasePrice() != null
                        ? nonNegative(item.customPurchasePrice(), "Purchase cost cannot be negative.")
                        : BigDecimal.ZERO;
                stock=0L;
            } else {

                prod = productsWithIds.get(prodId);
                if (prod == null) {
                    throw new ProductNotFoundException("no product was found with this id " + prodId);
                }
                name = prod.getName();
                sellingPrice = prod.getSellingPrice();
                purchasePrice = prod.getPurchasePrice();
                if(quantity>prod.getStock()){

throw new InsuffecientStockException("insuffesient stock the availabele quantity is :"+prod.getName()+"  " +prod.getStock());
                }
                stock = prod.getStock()-quantity;
                prod.setStock(stock);

            }
            OrderItem createItem = OrderItem.builder().
                    quantity(quantity)
                    .product(prod)
                    .productName(name)
                    .productPurchasePrice(purchasePrice)
                    .productSellingPrice(sellingPrice)
                    .subDiscount(discount).build();


            order.addOrderItem(createItem);
            BigDecimal extendedPrice = sellingPrice.multiply(BigDecimal.valueOf(quantity));
            validateDiscount(discount, extendedPrice, "Line discount cannot exceed the line total.");
            BigDecimal subTotal = extendedPrice.subtract(discount);
            total = total.add(subTotal);

            BigDecimal margin = (sellingPrice.subtract(purchasePrice)).multiply(BigDecimal.valueOf(quantity));
            BigDecimal subRevenue = margin.subtract(discount);

            revenue = revenue.add(subRevenue);


        }

        validateDiscount(orderDiscount, total, "Sale discount cannot exceed the subtotal.");
        total = total.subtract(orderDiscount);
        revenue = revenue.subtract(orderDiscount);
        BigDecimal taxRate = nonNegative(
                currentSettings.taxRate(), "Configured tax rate cannot be negative.");
        if (taxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new SaleValidationException("Configured tax rate cannot exceed 100%.");
        }
        BigDecimal taxAmount = SalesTaxCalculator.calculateTax(total, taxRate);
        total = total.add(taxAmount);

        PaymentSettlement payment = settlePayment(
                create.paymentMethod(), create.paymentReference(), create.paid(), total);

        order.setPaid(payment.amountApplied());
        order.setPaymentMethod(payment.method());
        order.setPaymentReference(payment.reference());
        order.setCashReceived(payment.cashReceived());
        order.setCashChange(payment.cashChange());
        order.setDiscount(orderDiscount);
        order.setTaxRate(taxRate);
        order.setTaxAmount(taxAmount);
        order.setTotal(total);
        order.setRevenue(revenue);

String orderNumber = generateOrderNumber();

        order.setOrderNumber(orderNumber);

        Order savedOrder = orderRepo.save(order);

        return orderMapper.toResponse(savedOrder);
    }



    public String generateOrderNumber() {
        return (orderNumberGenerator.nextValue() + 1000) + "";
    }



    @Transactional(readOnly = true)
    public OrderResponseDto findOrderById(Long id){

        Order found = orderRepo.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order was not found."));


        return orderMapper.toResponse(found);

    }



   @Transactional
    public OrderResponseDto updateOrder(Long orderId , OrderUpdateDto update){

        Order order = orderRepo.findById(orderId)
                .orElseThrow(()->new OrderNotFoundException("order wasn't found"));
        if (order.isVoided() || zeroIfNull(order.getReturnCredit()).signum() > 0
                || saleReturnRepo.existsByOrder_Id(orderId)) {
            throw new SaleValidationException("A sale with returns or a void cannot be edited.");
        }

        Customer customer = update.customerId() == null
                ? null
                : customerRepo.findById(update.customerId())
                        .orElseThrow(() -> new EntityNotFoundException("customer wasn't found"));
        order.setCustomer(customer);

           List<OrderItemCreateDto> items = update.itemsList();
           if (items == null || items.isEmpty()) {
               throw new YouMustProvideAtLeastOneItem("Please provide at least one sale item.");
           }
           List<Long> allProdIds =items.stream().map(OrderItemCreateDto::productId)
                   .filter(Objects::nonNull)
                   .toList();

           Map<Long,Products> prodsWithIds = productRepo.findAllByIds(allProdIds).stream()
                   .collect(Collectors.toMap(Products::getId,p->p));

           for(OrderItem oldItem : order.getOrderItems()){

               if(oldItem.getProduct()!=null && productRepo.checkActiveStatus(oldItem.getProduct().getId(),true) == 1){
        Products prod = oldItem.getProduct();

        prod.setStock(prod.getStock()+oldItem.getQuantity());

               }
               }

        return completeOrderUpdate(order, update, items, prodsWithIds);
    }

               @Transactional(readOnly = true)
               public Page<OrderResponseDto> findOrdersForReturns(Pageable pageable) {
                   return orderRepo.findAll(pageable).map(orderMapper::toResponse);
               }

               @Transactional(readOnly = true)
               public List<SaleReturnLineOption> getReturnableItems(Long orderId) {
                   Order order = orderRepo.findById(orderId)
                           .orElseThrow(() -> new OrderNotFoundException("Sale was not found."));
                   if (order.isVoided()) {
                       return List.of();
                   }
                   return order.getOrderItems().stream()
                           .map(item -> {
                               long returned = saleReturnRepo.sumReturnedQuantity(item.getId());
                               long remaining = item.getQuantity() - returned;
                               BigDecimal lineCredit = refundableLineTotal(order, item);
                               BigDecimal unitCredit = lineCredit.divide(
                                       BigDecimal.valueOf(item.getQuantity()), 2, java.math.RoundingMode.HALF_UP);
                               return new SaleReturnLineOption(item.getId(), item.getProductName(),
                                       item.getQuantity(), returned, remaining, unitCredit, item.getProduct() != null);
                           })
                           .filter(item -> item.quantityReturnable() > 0)
                           .toList();
               }

               @Transactional(readOnly = true)
               public List<SaleReturnHistoryRow> getSaleReturnHistory(Long orderId) {
                   if (!orderRepo.existsById(orderId)) {
                       throw new OrderNotFoundException("Sale was not found.");
                   }
                   return saleReturnRepo.findByOrder_IdOrderByCreatedAtDesc(orderId).stream()
                           .map(saleReturn -> new SaleReturnHistoryRow(
                                   saleReturn.getCreatedAt(),
                                   saleReturn.getReturnType(),
                                   saleReturn.getReason(),
                                   saleReturn.getUser().getName(),
                                   saleReturn.getItems().stream()
                                           .map(item -> item.getOrderItem().getProductName() + " x" + item.getQuantity())
                                           .collect(Collectors.joining(", ")),
                                   saleReturn.getItems().stream().mapToInt(SaleReturnItem::getQuantity).sum(),
                                   saleReturn.getItems().stream().anyMatch(SaleReturnItem::isRestocked),
                                   saleReturn.getCreditTotal(),
                                   saleReturn.getRefundTotal(),
                                   saleReturn.getPaymentMethod(),
                                   saleReturn.getPaymentReference()))
                           .toList();
               }

               @Transactional
               public SaleReturn recordSaleReturn(Long orderId, SaleReturnRequest request) {
                   Users user = authenticatedUser();
                   ShiftSession activeShift = resolveActiveShiftForReturn(user.getId(),
                           "Open a shift before processing a sale return.");
                   Order order = lockedSale(orderId);
                   validateReturnOrder(order);
                   if (request == null || request.orderItemId() == null || request.quantity() == null
                           || request.quantity() <= 0 || request.reason() == null || request.reason().isBlank()) {
                       throw new SaleValidationException("Select an item, a positive quantity, and a return reason.");
                   }

                   OrderItem item = order.getOrderItems().stream()
                           .filter(candidate -> candidate.getId().equals(request.orderItemId()))
                           .findFirst()
                           .orElseThrow(() -> new SaleValidationException("The selected item is not on this sale."));
                   long returnedQuantity = saleReturnRepo.sumReturnedQuantity(item.getId());
                   long remainingQuantity = item.getQuantity() - returnedQuantity;
                   if (request.quantity() > remainingQuantity) {
                       throw new SaleValidationException("Return quantity exceeds the unreturned quantity on this sale.");
                   }
                   if (request.restock() && item.getProduct() == null) {
                       throw new SaleValidationException("Custom sale items cannot be restocked.");
                   }
                   if (request.restock()) {
                       Products product = productRepo.findAllByIds(List.of(item.getProduct().getId())).stream()
                               .filter(candidate -> candidate.getId().equals(item.getProduct().getId()))
                               .findFirst()
                               .orElseThrow(() -> new SaleValidationException("The returned product is no longer available."));
                       product.setStock(incrementStock(product.getStock(), request.quantity()));
                   }

                   BigDecimal oldOrderCredit = zeroIfNull(order.getReturnCredit());
                   BigDecimal itemCredit = calculateReturnCredit(order, item, request.quantity(),
                           returnedQuantity, oldOrderCredit);
                   BigDecimal refund = refundDue(order, oldOrderCredit.add(itemCredit));
                   SaleReturn saleReturn = buildReturn(order, user, SaleReturnType.RETURN,
                           request.reason(), itemCredit, refund);
                   saleReturn.setShiftSession(activeShift);
                   saleReturn.addItem(SaleReturnItem.builder()
                           .orderItem(item)
                           .quantity(request.quantity())
                           .restocked(request.restock())
                           .creditTotal(itemCredit)
                           .unitCredit(itemCredit.divide(BigDecimal.valueOf(request.quantity()), 2,
                                   java.math.RoundingMode.HALF_UP))
                           .build());
                   applyReturnAccounting(order, itemCredit, refund);
                   return saleReturnRepo.save(saleReturn);
               }

               @Transactional
               public SaleReturn voidSale(Long orderId, String reason) {
                   Users user = authenticatedUser();
                   ShiftSession activeShift = resolveActiveShiftForReturn(user.getId(),
                           "Open a shift before voiding a sale.");
                   Order order = lockedSale(orderId);
                   validateReturnOrder(order);
                   if (reason == null || reason.isBlank() || reason.length() > 255) {
                       throw new SaleValidationException("A void reason of 1 to 255 characters is required.");
                   }

                   BigDecimal oldOrderCredit = zeroIfNull(order.getReturnCredit());
                   BigDecimal returnCredit = BigDecimal.ZERO;
                   SaleReturn saleReturn = buildReturn(order, user, SaleReturnType.VOID,
                           reason.trim(), BigDecimal.ZERO, BigDecimal.ZERO);
                   saleReturn.setShiftSession(activeShift);
                   boolean returnedAnyUnits = false;
                   for (OrderItem item : order.getOrderItems()) {
                       long returned = saleReturnRepo.sumReturnedQuantity(item.getId());
                       int remaining = item.getQuantity() - Math.toIntExact(returned);
                       if (remaining <= 0) {
                           continue;
                       }
                       returnedAnyUnits = true;
                       BigDecimal credit = calculateReturnCredit(order, item, remaining, returned,
                               oldOrderCredit.add(returnCredit));
                       returnCredit = returnCredit.add(credit);
                       if (item.getProduct() != null) {
                           Products product = productRepo.findAllByIds(List.of(item.getProduct().getId())).stream()
                                   .filter(candidate -> candidate.getId().equals(item.getProduct().getId()))
                                   .findFirst()
                                   .orElseThrow(() -> new SaleValidationException(
                                           "A product on this sale is no longer available for restocking."));
                           product.setStock(incrementStock(product.getStock(), remaining));
                       }
                       saleReturn.addItem(SaleReturnItem.builder()
                               .orderItem(item)
                               .quantity(remaining)
                               .restocked(item.getProduct() != null)
                               .creditTotal(credit)
                               .unitCredit(credit.divide(BigDecimal.valueOf(remaining), 2,
                                       java.math.RoundingMode.HALF_UP))
                               .build());
                   }
                   if (!returnedAnyUnits && order.getTotal().signum() > 0) {
                       throw new SaleValidationException("All sale items have already been returned.");
                   }
                   BigDecimal refund = refundDue(order, oldOrderCredit.add(returnCredit));
                   saleReturn.setCreditTotal(returnCredit);
                   saleReturn.setRefundTotal(refund);
                   applyReturnAccounting(order, returnCredit, refund);
                   order.setVoided(true);
                   return saleReturnRepo.save(saleReturn);
               }

               private Users authenticatedUser() {
                   Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                   if (auth == null || !auth.isAuthenticated()
                           || !(auth.getPrincipal() instanceof UserPrincipal principal)
                           || principal.getAuthorities().stream()
                                   .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
                       throw new AccessDeniedException("An administrator session is required to process sale returns.");
                   }
                   return userRepo.getReferenceById(principal.getId());
               }

               private ShiftSession resolveActiveShiftForReturn(Long userId, String requiredMessage) {
                   if (!settingsService.getSettings().shiftManagement()) {
                       return null;
                   }
                   try {
                       return shiftService.getOpenShiftForSale(userId);
                   } catch (ShiftOperationException exception) {
                       throw new ShiftRequiredException(requiredMessage);
                   }
               }

               private Order lockedSale(Long orderId) {
                   return orderRepo.findForUpdateById(orderId)
                           .orElseThrow(() -> new OrderNotFoundException("Sale was not found."));
               }

               private static void validateReturnOrder(Order order) {
                   if (order.isVoided()) {
                       throw new SaleValidationException("This sale has already been voided.");
                   }
               }

               private SaleReturn buildReturn(Order order, Users user, SaleReturnType type,
                                              String reason, BigDecimal credit, BigDecimal refund) {
                   return SaleReturn.builder()
                           .order(order)
                           .user(user)
                           .returnType(type)
                           .reason(reason.trim())
                           .creditTotal(credit)
                           .refundTotal(refund)
                           .paymentMethod(order.getPaymentMethod())
                           .paymentReference(order.getPaymentReference())
                           .build();
               }

               private static void applyReturnAccounting(Order order, BigDecimal credit, BigDecimal refund) {
                   order.setReturnCredit(zeroIfNull(order.getReturnCredit()).add(credit));
                   order.setRefundedTotal(zeroIfNull(order.getRefundedTotal()).add(refund));
               }

               private BigDecimal calculateReturnCredit(Order order, OrderItem item, int quantity,
                                                        long alreadyReturned, BigDecimal previousOrderCredit) {
                   BigDecimal lineTotal = refundableLineTotal(order, item);
                   BigDecimal previouslyCredited = zeroIfNull(saleReturnRepo.sumReturnedCredit(item.getId()));
                   long stillReturnable = item.getQuantity() - alreadyReturned;
                   BigDecimal credit;
                   if (quantity == stillReturnable) {
                       credit = lineTotal.subtract(previouslyCredited).max(BigDecimal.ZERO);
                   } else {
                       credit = lineTotal.divide(BigDecimal.valueOf(item.getQuantity()), 2,
                               java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(quantity));
                   }

                   boolean completesSale = true;
                   for (OrderItem orderItem : order.getOrderItems()) {
                       long already = orderItem.getId().equals(item.getId())
                               ? alreadyReturned : saleReturnRepo.sumReturnedQuantity(orderItem.getId());
                       if (orderItem.getQuantity() - already - (orderItem.getId().equals(item.getId()) ? quantity : 0) > 0) {
                           completesSale = false;
                           break;
                       }
                   }
                   if (completesSale) {
                       credit = order.getTotal().subtract(previousOrderCredit).max(BigDecimal.ZERO);
                   }
                   return credit.min(order.getTotal().subtract(previousOrderCredit).max(BigDecimal.ZERO));
               }

               private BigDecimal refundDue(Order order, BigDecimal updatedCredit) {
                   BigDecimal originalBalance = order.getTotal().subtract(order.getPaid()).max(BigDecimal.ZERO);
                   BigDecimal targetRefund = updatedCredit.subtract(originalBalance).max(BigDecimal.ZERO)
                           .min(order.getPaid());
                   return targetRefund.subtract(zeroIfNull(order.getRefundedTotal())).max(BigDecimal.ZERO);
               }

               private BigDecimal refundableLineTotal(Order order, OrderItem item) {
                   BigDecimal lineGross = item.getProductSellingPrice()
                           .multiply(BigDecimal.valueOf(item.getQuantity()))
                           .subtract(zeroIfNull(item.getSubDiscount())).max(BigDecimal.ZERO);
                   BigDecimal subtotalBeforeOrderDiscount = order.getOrderItems().stream()
                           .map(orderItem -> orderItem.getProductSellingPrice()
                                   .multiply(BigDecimal.valueOf(orderItem.getQuantity()))
                                   .subtract(zeroIfNull(orderItem.getSubDiscount())).max(BigDecimal.ZERO))
                           .reduce(BigDecimal.ZERO, BigDecimal::add);
                   if (subtotalBeforeOrderDiscount.signum() == 0) {
                       return BigDecimal.ZERO;
                   }
                   BigDecimal allocatedDiscount = zeroIfNull(order.getDiscount()).multiply(lineGross)
                           .divide(subtotalBeforeOrderDiscount, 2, java.math.RoundingMode.HALF_UP);
                   BigDecimal taxableLine = lineGross.subtract(allocatedDiscount).max(BigDecimal.ZERO);
                   BigDecimal taxableTotal = zeroIfNull(order.getTotal()).subtract(zeroIfNull(order.getTaxAmount()));
                   if (taxableTotal.signum() <= 0 || order.getTaxAmount() == null
                           || order.getTaxAmount().signum() == 0) {
                       return taxableLine;
                   }
                   BigDecimal allocatedTax = order.getTaxAmount().multiply(taxableLine)
                           .divide(taxableTotal, 2, java.math.RoundingMode.HALF_UP);
                   return taxableLine.add(allocatedTax);
               }

               private static BigDecimal zeroIfNull(BigDecimal amount) {
                   return amount == null ? BigDecimal.ZERO : amount;
               }

               private static long incrementStock(Long currentStock, int returnedQuantity) {
                   if (currentStock == null || currentStock < 0) {
                       throw new SaleValidationException("Current product stock is invalid.");
                   }
                   try {
                       return Math.addExact(currentStock, returnedQuantity);
                   } catch (ArithmeticException exception) {
                       throw new SaleValidationException("Returned quantity exceeds the available stock range.");
                   }
               }

    private OrderResponseDto completeOrderUpdate(Order order, OrderUpdateDto update,
                                                 List<OrderItemCreateDto> items,
                                                 Map<Long, Products> prodsWithIds) {
           order.getOrderItems().clear();



  BigDecimal total = BigDecimal.ZERO;
BigDecimal revenue = BigDecimal.ZERO;
BigDecimal orderDiscount = nonNegative(update.discount(), "Sale discount cannot be negative.");

   for(OrderItemCreateDto item : items){
       requirePositiveQuantity(item.quantity());
       BigDecimal bigQuantity = BigDecimal.valueOf(item.quantity());
       BigDecimal subDiscount = nonNegative(item.subDiscount(), "Line discount cannot be negative.");
        String name;
        BigDecimal price;
         BigDecimal purchase;
         Products prod = null;
        if(item.productId()==null){
            name = requireCustomName(item.customName());
            price = nonNegative(item.customSellingPrice(), "Custom item selling price is required.");
            purchase = item.customPurchasePrice()!=null
                    ? nonNegative(item.customPurchasePrice(), "Purchase cost cannot be negative")
                    : BigDecimal.ZERO;
        }

        else{
            prod = prodsWithIds.get(item.productId());
            if(productRepo.checkActiveStatus(item.productId(),false) == 1){

prod = productRepo.findProductByIdAndIsActiveFalse(item.productId());
            }

            if(prod==null){

                throw new ProductNotFoundException("prod doesn't exist");
            }
            name = prod.getName();
            price = prod.getSellingPrice();
            purchase=prod.getPurchasePrice();

            if(item.quantity()>prod.getStock()){
                throw new InsuffecientStockException("insufficient stock available");
            }

            prod.setStock(prod.getStock()-item.quantity());
        }

        BigDecimal extendedPrice = price.multiply(bigQuantity);
        validateDiscount(subDiscount, extendedPrice, "Line discount cannot exceed the line total.");
        BigDecimal subTotal = extendedPrice.subtract(subDiscount);

        total = total.add(subTotal);

        BigDecimal margin = (price.subtract(purchase)).multiply(bigQuantity);
        revenue = revenue.add(margin.subtract(subDiscount));

        OrderItem createItem =  OrderItem.builder()
                .subTotal(subTotal)
                .productName(name)
                .productSellingPrice(price)
                .productPurchasePrice(purchase)
                .quantity(item.quantity())
                .subDiscount(subDiscount)
                .product(prod)
                .build();
        order.addOrderItem(createItem);
   }


   productRepo.saveAll(prodsWithIds.values());

validateDiscount(orderDiscount, total, "Sale discount cannot exceed the subtotal.");
total = total.subtract(orderDiscount);
revenue = revenue.subtract(orderDiscount);
BigDecimal taxRate = nonNegative(
        settingsService.getSettings().taxRate(), "Configured tax rate cannot be negative.");
if (taxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
    throw new SaleValidationException("Configured tax rate cannot exceed 100%.");
}
BigDecimal taxAmount = SalesTaxCalculator.calculateTax(total, taxRate);
total = total.add(taxAmount);

PaymentSettlement payment = settlePayment(
        update.paymentMethod(), update.paymentReference(), update.paid(), total);
order.setTotal(total);
order.setRevenue(revenue);
order.setDiscount(orderDiscount);
order.setTaxRate(taxRate);
order.setTaxAmount(taxAmount);
order.setPaid(payment.amountApplied());
order.setPaymentMethod(payment.method());
order.setPaymentReference(payment.reference());
order.setCashReceived(payment.cashReceived());
order.setCashChange(payment.cashChange());

Order savedOrder = orderRepo.save(order);
if (order.getShiftSession() != null) {
    orderRepo.flush();
    shiftService.refreshReconciliationAfterOrderUpdate(order.getShiftSession().getId());
}

return orderMapper.toResponse(savedOrder);
   }

    private static BigDecimal nonNegative(BigDecimal amount, String message) {
        if (amount == null || amount.signum() < 0) {
            throw new SaleValidationException(message);
        }
        return amount;
    }

    private static PaymentSettlement settlePayment(PaymentMethod requestedMethod, String reference,
                                                   BigDecimal tendered, BigDecimal total) {
        BigDecimal amountTendered = nonNegative(tendered, "Payment amount cannot be negative.");
        PaymentMethod method = requestedMethod == null ? PaymentMethod.CASH : requestedMethod;
        String normalizedReference = reference == null || reference.isBlank() ? null : reference.trim();
        if (method == PaymentMethod.E_WALLET && amountTendered.signum() > 0 && normalizedReference == null) {
            throw new SaleValidationException("Provide the e-wallet provider or transaction reference.");
        }
        if (method != PaymentMethod.CASH && amountTendered.compareTo(total) > 0) {
            throw new SaleValidationException("Payment cannot exceed the sale total.");
        }

        BigDecimal amountApplied = method == PaymentMethod.CASH
                ? amountTendered.min(total)
                : amountTendered;
        BigDecimal cashReceived = method == PaymentMethod.CASH ? amountTendered : BigDecimal.ZERO;
        BigDecimal cashChange = method == PaymentMethod.CASH
                ? amountTendered.subtract(amountApplied)
                : BigDecimal.ZERO;
        return new PaymentSettlement(method, normalizedReference, amountApplied, cashReceived, cashChange);
    }

    private record PaymentSettlement(PaymentMethod method, String reference, BigDecimal amountApplied,
                                     BigDecimal cashReceived, BigDecimal cashChange) {
    }

    private static void validateDiscount(BigDecimal amount, BigDecimal limit, String message) {
        if (amount.compareTo(limit) > 0) {
            throw new SaleValidationException(message);
        }
    }

    private static void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new SaleValidationException("Sale item quantity must be at least one.");
        }
    }

    private static String requireCustomName(String name) {
        if (name == null || name.isBlank()) {
            throw new SaleValidationException("Custom sale items require a name.");
        }
        return name.trim();
    }


   @Transactional(readOnly = true)

    public List<OrderResponseDto> findByOrderNumber(String OrderNumber){

        List<Order> results = orderRepo.findByOrderNumber(OrderNumber);
System.out.println(results);
        return orderMapper.toListResponse(results);

   }


   @Transactional(readOnly = true)

    public Page<OrderResponseDto> findByCustomerName(String name, Pageable pageable){

     Page<Order> result  =  orderRepo.findOrdersByCustomerNameContainingKeyword(name,pageable);


       return result.map(orderMapper::toResponse);
   }


   @Transactional(readOnly = true)
    public CustomerSummary getCustomerSummaryInPeriodById(Long id , LocalDateTime start,LocalDateTime end,Pageable pageable){

        Page<Order> res = orderRepo.findOrdersByCustomerIdBetweenDates(id,start,end,pageable);
        Object[] totals = orderRepo.sumAllOrdersSummaryBetweenDatesById(id,start,end);
        BigDecimal totalSelling = (BigDecimal) totals[0];
        BigDecimal totalPaid = (BigDecimal) totals[1];
        BigDecimal totalRemaining = (BigDecimal) totals[2];

 Page<OrderResponseDto> response = res.map(orderMapper::toResponse);
  Customer found = customerRepo.findById(id)
          .orElseThrow(() -> new CustomerNotFoundException("Customer was not found."));
  String name = found.getName();
 CustomerSummary summary  = new CustomerSummary(name,start,end,totalSelling,totalPaid,totalRemaining,response);
       return summary;
   }

}
