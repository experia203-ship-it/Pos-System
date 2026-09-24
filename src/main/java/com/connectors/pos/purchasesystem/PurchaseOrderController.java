package com.connectors.pos.purchasesystem;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.ordersystem.orderdtos.CartItemView;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderDto;
import com.connectors.pos.purchasesystem.purchasedtos.CreatePurchaseOrderItemDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/purchase")
public class PurchaseOrderController {

    private final ProductRepository prodRepo;
    private final VendorRepository vendorRepo;
    private final PurchaseOrderService orderServo;

    @ModelAttribute("suppliers")
    public List<Vendor> populateVendors() {
        return vendorRepo.findAll();
    }

    private void recalculateAndPopulateModel(
            List<CreatePurchaseOrderItemDto> listItems,
            BigDecimal discountParam,
            BigDecimal paidParam,
            Long supplierId,
            Long ordId,
            String orderNumber,
            Model model) {


        listItems.removeIf(it -> it.quantity() <= 0);

        List<Long> prodIds = listItems.stream()
                .map(CreatePurchaseOrderItemDto::productId)
                .filter(Objects::nonNull)
                .toList();

        List<Products> allProducts = prodRepo.findAllByIds(prodIds);
        Map<Long, Products> productsWithIds = allProducts.stream()
                .collect(Collectors.toMap(Products::getId, p -> p));

        List<CartItemView> cartItems = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CreatePurchaseOrderItemDto item : listItems) {
            BigDecimal bigQuantity = BigDecimal.valueOf(item.quantity());
            BigDecimal subDisc = item.subDiscount() != null ? item.subDiscount() : BigDecimal.ZERO;
            String name;
            BigDecimal purchasePrice;

            if (item.productId() != null) {
                Products prod = productsWithIds.get(item.productId());
                name = (item.customName() != null && !item.customName().isBlank())
                        ? item.customName()
                        : (prod != null ? prod.getName() : "Unknown");
                BigDecimal basePrice = (prod != null && prod.getPurchasePrice() != null) ? prod.getPurchasePrice() : BigDecimal.ZERO;
                purchasePrice = (item.customPurchasePrice() != null) ? item.customPurchasePrice() : basePrice;
            } else {
                name = (item.customName() != null && !item.customName().isBlank()) ? item.customName() : "Custom Item";
                purchasePrice = (item.customPurchasePrice() != null) ? item.customPurchasePrice() : BigDecimal.ZERO;
            }

            BigDecimal subTotal = (purchasePrice.multiply(bigQuantity)).subtract(subDisc);
            grandTotal = grandTotal.add(subTotal);

            CartItemView cartItem = new CartItemView(
                    item.productId(),
                    name,
                    item.quantity(),
                    subDisc,
                    purchasePrice,
                    subTotal,
                    item.barcode(),
                    item.customName(),
                    item.customSellingPrice(),
                    item.customPurchasePrice()
            );
            cartItems.add(cartItem);
        }

        BigDecimal globalDiscount = discountParam != null ? discountParam : BigDecimal.ZERO;
        grandTotal = grandTotal.subtract(globalDiscount);

        BigDecimal paid = paidParam != null ? paidParam : BigDecimal.ZERO;
        BigDecimal remaining = grandTotal.subtract(paid);

        model.addAttribute("discount", globalDiscount);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("paid", paid);
        model.addAttribute("remaining", remaining);
        model.addAttribute("currentSupplierId", supplierId);
        model.addAttribute("ordId", ordId);
        model.addAttribute("orderNumber", orderNumber);
    }

    @PostMapping("/update")
    public String updateCart(
            @ModelAttribute CreatePurchaseOrderDto createDto,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ordId,
            @RequestParam(required = false) String orderNumber,
            Model model) {

        List<CreatePurchaseOrderItemDto> listItems = new ArrayList<>();
        if (createDto.itemsList() != null) {
            listItems.addAll(createDto.itemsList());
        }

        recalculateAndPopulateModel(listItems, createDto.discount(), createDto.paid(), supplierId, ordId, orderNumber, model);
        return "purchase-cart :: cart-zone";
    }

    @PostMapping("/add/{prodId}")
    public String addNewItemToCart(
            @PathVariable Long prodId,
            @ModelAttribute CreatePurchaseOrderDto createDto,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ordId,
            @RequestParam(required = false) String orderNumber,
            Model model) {

        List<CreatePurchaseOrderItemDto> listItems = new ArrayList<>();
        if (createDto.itemsList() != null) {
            listItems.addAll(createDto.itemsList());
        }

        Optional<CreatePurchaseOrderItemDto> opt = listItems.stream()
                .filter(it -> Objects.equals(it.productId(), prodId))
                .findFirst();

        if (opt.isPresent()) {
            CreatePurchaseOrderItemDto existing = opt.get();
            int index = listItems.indexOf(existing);
            CreatePurchaseOrderItemDto updated = new CreatePurchaseOrderItemDto(
                    existing.productId(),
                    existing.quantity() + 1,
                    existing.subDiscount(),
                    existing.barcode(),
                    existing.customName(),
                    existing.customSellingPrice(),
                    existing.customPurchasePrice()
            );
            listItems.set(index, updated);
        } else {
            CreatePurchaseOrderItemDto newItem = new CreatePurchaseOrderItemDto(
                    prodId,
                    1,
                    BigDecimal.ZERO,
                    null,
                    null,
                    null,
                    null
            );
            listItems.add(newItem);
        }

        recalculateAndPopulateModel(listItems, createDto.discount(), createDto.paid(), supplierId, ordId, orderNumber, model);
        return "purchase-cart :: cart-zone";
    }

    @PostMapping("/custom")
    public String addCustomItemToPurchase(
            @ModelAttribute CreatePurchaseOrderDto createDto,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ordId,
            @RequestParam(required = false) String orderNumber,
            Model model) {

        List<CreatePurchaseOrderItemDto> listItems = new ArrayList<>();
        if (createDto.itemsList() != null) {
            listItems.addAll(createDto.itemsList());
        }

        CreatePurchaseOrderItemDto customItem = new CreatePurchaseOrderItemDto(
                null,
                1,
                BigDecimal.ZERO,
                null,
                "Custom Item",
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        listItems.add(customItem);

        recalculateAndPopulateModel(listItems, createDto.discount(), createDto.paid(), supplierId, ordId, orderNumber, model);
        return "purchase-cart :: cart-zone";
    }

    @PostMapping("/scan")
    public String scanItem(
            @RequestParam("barcode") String barcode,
            @ModelAttribute CreatePurchaseOrderDto createDto,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ordId,
            @RequestParam(required = false) String orderNumber,
            Model model) {

        List<CreatePurchaseOrderItemDto> listItems = new ArrayList<>();
        if (createDto.itemsList() != null) {
            listItems.addAll(createDto.itemsList());
        }

        Products product = prodRepo.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("Product doesn't exist with that barcode: " + barcode));

        Optional<CreatePurchaseOrderItemDto> opt = listItems.stream()
                .filter(it -> Objects.equals(it.productId(), product.getId()))
                .findFirst();

        if (opt.isPresent()) {
            CreatePurchaseOrderItemDto existing = opt.get();
            int index = listItems.indexOf(existing);
            CreatePurchaseOrderItemDto updated = new CreatePurchaseOrderItemDto(
                    existing.productId(),
                    existing.quantity() + 1,
                    existing.subDiscount(),
                    existing.barcode(),
                    existing.customName(),
                    existing.customSellingPrice(),
                    existing.customPurchasePrice()
            );
            listItems.set(index, updated);
        } else {
            CreatePurchaseOrderItemDto newItem = new CreatePurchaseOrderItemDto(
                    product.getId(),
                    1,
                    BigDecimal.ZERO,
                    barcode,
                    null,
                    null,
                    null
            );
            listItems.add(newItem);
        }

        recalculateAndPopulateModel(listItems, createDto.discount(), createDto.paid(), supplierId, ordId, orderNumber, model);
        return "purchase-cart :: cart-zone";
    }

    @DeleteMapping("/remove/{index}")
    public String deleteItem(
            @PathVariable int index,
            @ModelAttribute CreatePurchaseOrderDto createDto,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long ordId,
            @RequestParam(required = false) String orderNumber,
            Model model) {

        List<CreatePurchaseOrderItemDto> listItems = new ArrayList<>();
        if (createDto.itemsList() != null) {
            listItems.addAll(createDto.itemsList());
        }

        if (index >= 0 && index < listItems.size()) {
            listItems.remove(index);
        }

        recalculateAndPopulateModel(listItems, createDto.discount(), createDto.paid(), supplierId, ordId, orderNumber, model);
        return "purchase-cart :: cart-zone";
    }

    @PostMapping("/check-out")
    public String checkOrderOut(
            @Valid @ModelAttribute CreatePurchaseOrderDto currentCart,
            BindingResult bindResult,
            HttpServletResponse response,
            Model model) {

        if (bindResult.hasErrors()) {
            response.setHeader("HX-Retarget", "#list-error");
            response.setHeader("HX-Reswap", "innerHTML");
            model.addAttribute("errorMessage", "Invalid purchase order data provided.");
            return "fragments/auth-messages :: exceptions-response";
        }

        orderServo.createPurchaseOrder(currentCart);

        recalculateAndPopulateModel(new ArrayList<>(), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, model);
        return "purchase-cart :: cart-zone";
    }

    @PatchMapping("/update/{id}")
    public String updateExistingOrder(
            @PathVariable("id") Long ordId,
            @Valid @ModelAttribute CreatePurchaseOrderDto updateDto,
            BindingResult bindResult,
            HttpServletResponse response,
            Model model) {

        if (bindResult.hasErrors()) {
            response.setHeader("HX-Retarget", "#cart-zone");
            response.setHeader("HX-Reswap", "innerHTML");
            return "purchase-cart :: cart-zone";
        }

        orderServo.updatePurchaseOrder(ordId, updateDto);

        recalculateAndPopulateModel(new ArrayList<>(), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, model);
        return "purchase-cart :: cart-zone";
    }

    @DeleteMapping("/clear")
    public String clearCart(Model model) {
        recalculateAndPopulateModel(new ArrayList<>(), BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, model);
        return "purchase-cart :: cart-zone";
    }
}