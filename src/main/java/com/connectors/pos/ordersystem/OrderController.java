package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.customersystem.CustomerService;
import com.connectors.pos.customersystem.customerdtos.CustomerViewDto;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.YouMustProvideAtLeastOneItem;
import com.connectors.pos.ordersystem.orderdtos.*;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.settings.*;
import com.connectors.pos.settings.settingsdtos.SettingsResponseDto;
import com.connectors.pos.shift.ShiftService;
import com.connectors.pos.shift.ShiftSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/pos")
public class OrderController { private final CustomerRepository customerRepo;
 private final OrderService orderServo;
private final ProductRepository productRepo;
private final SettingsService settingsServo;
private final CustomerService customerServo;
private final SettingsGlobalInjector settings;
private final ShiftService shiftService;
private final OrderRepository orderRepo;

    private long nextOrderNumber() {
        Long maximum = orderRepo.findMaxOrderNumber();
        return (maximum == null ? 0L : maximum) + 1L;
    }

    private BigDecimal addCartTaxTotals(Model model, BigDecimal taxableSubtotal) {
        BigDecimal taxRate = settings.getSettings().taxRate();
        if (taxRate == null) {
            taxRate = BigDecimal.ZERO;
        }
        BigDecimal taxAmount = SalesTaxCalculator.calculateTax(taxableSubtotal, taxRate);
        model.addAttribute("taxableSubtotal", taxableSubtotal);
        model.addAttribute("taxRate", taxRate);
        model.addAttribute("taxAmount", taxAmount);
        return taxableSubtotal.add(taxAmount);
    }

    private BigDecimal addPaymentPreview(Model model, PaymentMethod requestedMethod,
                                         String reference, BigDecimal tendered, BigDecimal total) {
        PaymentMethod method = requestedMethod == null ? PaymentMethod.CASH : requestedMethod;
        BigDecimal amountTendered = tendered == null ? BigDecimal.ZERO : tendered.max(BigDecimal.ZERO);
        BigDecimal payable = total.max(BigDecimal.ZERO);
        BigDecimal amountApplied = method == PaymentMethod.CASH ? amountTendered.min(payable) : amountTendered;
        BigDecimal cashChange = method == PaymentMethod.CASH
                ? amountTendered.subtract(amountApplied) : BigDecimal.ZERO;
        model.addAttribute("paymentMethod", method);
        model.addAttribute("paymentReference", reference);
        model.addAttribute("amountApplied", amountApplied);
        model.addAttribute("cashChange", cashChange);
        return amountApplied;
    }

@ModelAttribute("customers")
public List<Customer> populateCustomers(){

    return customerRepo.findAll();
}

@ModelAttribute("checkoutDisabled")
public boolean isCheckoutDisabled(@AuthenticationPrincipal UserPrincipal principal) {
    if (!settings.getSettings().shiftManagement()) {
        return false;
    }
    return principal == null || shiftService.findActiveShift(principal.getId()).isEmpty();
}

@ModelAttribute("returnsDisabled")
public boolean isReturnsDisabled(@AuthenticationPrincipal UserPrincipal principal) {
    if (!settings.getSettings().shiftManagement()) {
        return false;
    }
    return principal == null || shiftService.findActiveShift(principal.getId()).isEmpty();
}



    private String resolvePosView() {
        SettingsResponseDto res = settings.getSettings();
        PosStyle style = (res != null && res.posStyle() != null) ? res.posStyle() : PosStyle.HORIZONTAL;
        return style == PosStyle.HORIZONTAL ? "pos" : "fragments/pos-custom";
    }

    @GetMapping("/sales")
    public String viewPosPage(@AuthenticationPrincipal UserPrincipal principal, Model model){
        SettingsResponseDto res = settings.getSettings();
        model.addAttribute("allow_shift", res.shiftManagement());

        ShiftSession activeShift = principal == null ? null
                : shiftService.findActiveShift(principal.getId()).orElse(null);
        model.addAttribute("activeShift", activeShift);
        model.addAttribute("mode", "sales");

        return resolvePosView();
    }

    @GetMapping("/purchase")
    @PreAuthorize("hasRole('ADMIN')")
    public String viewPurchasePage(Model model){
        model.addAttribute("mode", "purchase");
        return resolvePosView();
    }

@PostMapping("/cart/add/{prodId}")


    public String addNewItemToCart(@PathVariable Long prodId  , @RequestParam(required = false) Long ordId,@ModelAttribute OrderCreateDto currentCart,Model model,HttpServletResponse response){
System.out.println("adding new item to cart with prodId: " + prodId);

    if(ordId!=null){
        model.addAttribute("ordId",ordId);
    }

    List<OrderItemCreateDto> listItems= new ArrayList<>();
        if(currentCart.itemsList()!=null && !(currentCart.itemsList().isEmpty())){
            listItems.addAll(currentCart.itemsList());

         }

            Optional<OrderItemCreateDto> opt = listItems.stream().filter(it->Objects.equals(it.productId(),prodId))
                    .findFirst();


            if(opt.isPresent()){

                OrderItemCreateDto alreadyExistsItem = opt.get();

                int newQ =alreadyExistsItem.quantity()+1;
                OrderItemCreateDto modified = new OrderItemCreateDto(alreadyExistsItem.productId(),newQ,alreadyExistsItem.subDiscount(), alreadyExistsItem.barcode(), alreadyExistsItem.customName()
                        ,alreadyExistsItem.customSellingPrice(),alreadyExistsItem.customPurchasePrice());

                int index = listItems.indexOf(alreadyExistsItem);
              listItems.set(index,modified);
        }

else {
                Products newProd = productRepo.findById(prodId)
                        .orElseThrow(() -> new ProductNotFoundException("product doesn't exist with that id: " + prodId));

                OrderItemCreateDto newItem = new OrderItemCreateDto(newProd.getId(), 1, BigDecimal.ZERO,null, null, null, null);

                listItems.add(newItem);
                response.setHeader("HX-Trigger-After-Settle","newItemAdded");

            }

List<Long> allProdIds = listItems.stream().map(OrderItemCreateDto ::productId).filter(Objects::nonNull)
        .toList();

List<Products> allProducts =  productRepo.findAllById(allProdIds);

Map<Long,Products> prodsAndIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));

List<CartItemView> cartItems = new ArrayList<>();


BigDecimal grandTotal = BigDecimal.ZERO;

for(OrderItemCreateDto item : listItems){
      String name;
      BigDecimal price;
      BigDecimal quantity = BigDecimal.valueOf(item.quantity());
      BigDecimal subDiscount = item.subDiscount()!=null ?item.subDiscount() :BigDecimal.ZERO;

      if(item.productId()!=null){
          Products prod = prodsAndIds.get(item.productId());
          name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() :prod.getName();
          price= (item.customSellingPrice()!=null) ? item.customSellingPrice() : prod.getSellingPrice();
      }
      else{
          name=item.customName();
          price=item.customSellingPrice()!=null ? item.customSellingPrice() : BigDecimal.ZERO;
      }

BigDecimal subTotal = (price.multiply(quantity)).subtract(subDiscount);
grandTotal=grandTotal.add(subTotal);

cartItems.add(new CartItemView(item.productId(),name,item.quantity()
                                ,subDiscount,price,subTotal,item.barcode(),item.customName(),item.customSellingPrice(),item.customPurchasePrice()));

}
BigDecimal discount = currentCart.discount()!=null ? currentCart.discount() :BigDecimal.ZERO;
grandTotal =grandTotal.subtract(discount);

BigDecimal paid = currentCart.paid()!=null ? currentCart.paid() : BigDecimal.ZERO;
grandTotal = addCartTaxTotals(model, grandTotal);

BigDecimal applied = addPaymentPreview(model, currentCart.paymentMethod(),
        currentCart.paymentReference(), paid, grandTotal);
BigDecimal remaining = grandTotal.subtract(applied);


Long currentLastOrderNum = nextOrderNumber();
String stordNum = currentLastOrderNum+"";

    model.addAttribute("orderNumber",stordNum);


model.addAttribute("globalDiscount",discount);
model.addAttribute("grandTotal",grandTotal);
model.addAttribute("cartItems",cartItems);
model.addAttribute("paid",paid);
model.addAttribute("remaining",remaining);
model.addAttribute("currentCustomerId", currentCart.customerId());


return "fragments/cart :: cart";
}


@PostMapping("/scan")
public String addNewItemUsingBarcodeSystem(@RequestParam(required = false) Long ordId,@RequestParam("barcode") String barcode,@ModelAttribute OrderCreateDto currentCart ,Model model){

    if(ordId!=null){
        model.addAttribute("ordId",ordId);
    }

    List<OrderItemCreateDto> listItems= new ArrayList<>();
    if(currentCart.itemsList()!=null && !(currentCart.itemsList().isEmpty())){
        listItems.addAll(currentCart.itemsList());

    }


    Optional<OrderItemCreateDto> opt = listItems.stream().filter(it->Objects.equals(it.barcode(),barcode))
            .findFirst();


    if(opt.isPresent()){

        OrderItemCreateDto alreadyExistsItem = opt.get();

        int newQ =alreadyExistsItem.quantity()+1;
        OrderItemCreateDto modified = new OrderItemCreateDto(alreadyExistsItem.productId(),newQ,alreadyExistsItem.subDiscount(),alreadyExistsItem.barcode(),alreadyExistsItem.customName()
                ,alreadyExistsItem.customSellingPrice(),alreadyExistsItem.customPurchasePrice());

        int index = listItems.indexOf(alreadyExistsItem);
        listItems.set(index,modified);
    }

    else {
        Products newProd = productRepo.findByBarcode(barcode)
                .orElseThrow(() -> new ProductNotFoundException("product doesn't exist with that barcode: " + barcode));

        OrderItemCreateDto newItem = new OrderItemCreateDto(newProd.getId(), 1, BigDecimal.ZERO, barcode,null, null, null);

        listItems.add(newItem);
    }

    List<Long> allProdIds = listItems.stream().map(OrderItemCreateDto ::productId).filter(Objects::nonNull)
            .toList();

    List<Products> allProducts =  productRepo.findAllById(allProdIds);

    Map<Long,Products> prodsAndIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));

    List<CartItemView> cartItems = new ArrayList<>();


    BigDecimal grandTotal = BigDecimal.ZERO;

    for(OrderItemCreateDto item : listItems){
        String name;
        BigDecimal price;
        BigDecimal quantity = BigDecimal.valueOf(item.quantity());
        BigDecimal subDiscount = item.subDiscount()!=null ?item.subDiscount() :BigDecimal.ZERO;

        if(item.productId()!=null){
            Products prod = prodsAndIds.get(item.productId());
            name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() :prod.getName();
            price= (item.customSellingPrice()!=null) ? item.customSellingPrice() : prod.getSellingPrice();
        }
        else{
            name=item.customName();
            price=item.customSellingPrice()!=null ? item.customSellingPrice() : BigDecimal.ZERO;
        }

        BigDecimal subTotal = (price.multiply(quantity)).subtract(subDiscount);
        grandTotal=grandTotal.add(subTotal);

        cartItems.add(new CartItemView(item.productId(),name,item.quantity()
                ,subDiscount,price,subTotal,item.barcode(),item.customName(),item.customSellingPrice(),item.customPurchasePrice()));

    }
    BigDecimal discount = currentCart.discount()!=null ? currentCart.discount() :BigDecimal.ZERO;
    grandTotal =grandTotal.subtract(discount);

    BigDecimal paid = currentCart.paid()!=null ? currentCart.paid() : BigDecimal.ZERO;
    grandTotal = addCartTaxTotals(model, grandTotal);

    BigDecimal applied = addPaymentPreview(model, currentCart.paymentMethod(),
            currentCart.paymentReference(), paid, grandTotal);
    BigDecimal remaining = grandTotal.subtract(applied);


    Long currentLastOrderNum = nextOrderNumber();
    String stordNum = currentLastOrderNum+"";

    model.addAttribute("orderNumber",stordNum);



    model.addAttribute("globalDiscount",discount);
    model.addAttribute("grandTotal",grandTotal);
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid",paid);
    model.addAttribute("remaining",remaining);
    model.addAttribute("currentCustomerId", currentCart.customerId());





    return "fragments/cart :: cart";





}





@PostMapping("/cart/update")
    public String updateCartInfo(@ModelAttribute OrderCreateDto createDto, Model model,@RequestParam(required = false) Long ordId){

if(ordId!=null){
    model.addAttribute("ordId",ordId);
}

    List<OrderItemCreateDto> listItems = new ArrayList<>();
    if(createDto.itemsList()!=null){

        listItems.addAll(createDto.itemsList());
    }

    listItems.removeIf(it->it.quantity()<=0);

    List<Long> prodIds = listItems.stream().map(OrderItemCreateDto::productId).filter(Objects::nonNull)
            .toList();
    List<Products> allProducts = productRepo.findAllById(prodIds);

    Map<Long,Products> productsWithIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));


    List<CartItemView> cartItems = new ArrayList<>();

BigDecimal grandTotal = BigDecimal.ZERO;
    for(OrderItemCreateDto item:listItems){

listItems.removeIf(it->it.quantity()<=0);

            BigDecimal Bigquantity=BigDecimal.valueOf(item.quantity());
            BigDecimal subDisc= item.subDiscount()!=null ? item.subDiscount() :BigDecimal.ZERO;
            String name;
            BigDecimal sellingPrice;
            BigDecimal purchasePrice;

            if(item.productId()!=null){
                Products prod = productsWithIds.get(item.productId());
                name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() : prod.getName();
                sellingPrice= (item.customSellingPrice()!=null) ? item.customSellingPrice() : prod.getSellingPrice();

            }
            else{
                name=item.customName();
                sellingPrice=item.customSellingPrice()!=null ? item.customSellingPrice() :BigDecimal.ZERO;
            }
            BigDecimal subTotal = (sellingPrice.multiply(Bigquantity)) .subtract(subDisc);

            grandTotal=grandTotal.add(subTotal);

        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.barcode(),item.customName()
        ,item.customSellingPrice(),item.customPurchasePrice());

             cartItems.add(cartItem);
        }
BigDecimal globalDiscount = createDto.discount()!=null ? createDto.discount() : BigDecimal.ZERO;
grandTotal=grandTotal.subtract(globalDiscount);

    BigDecimal paid = createDto.paid()!=null ? createDto.paid() : BigDecimal.ZERO;
    grandTotal = addCartTaxTotals(model, grandTotal);

    BigDecimal applied = addPaymentPreview(model, createDto.paymentMethod(),
            createDto.paymentReference(), paid, grandTotal);
    BigDecimal remaining = grandTotal.subtract(applied);

    Long currentLastOrderNum = nextOrderNumber();
    String stordNum = currentLastOrderNum+"";

    model.addAttribute("orderNumber",stordNum);


    model.addAttribute("globalDiscount",globalDiscount);
    model.addAttribute("grandTotal",grandTotal);
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid",paid);
    model.addAttribute("remaining",remaining);
    model.addAttribute("currentCustomerId", createDto.customerId());


return "fragments/cart ::cart";
}


@PostMapping("/cart/remove/{index}")

    public String removeAnItem(@ModelAttribute OrderCreateDto createDto ,@PathVariable("index") int index ,Model model,@RequestParam(required = false) Long ordId){

    if(ordId!=null){
        model.addAttribute("ordId",ordId);
    }

        List<OrderItemCreateDto> listItems = new ArrayList<>();

        if(createDto.itemsList()!=null&&!createDto.itemsList().isEmpty()){

            listItems.addAll(createDto.itemsList());

        }
        if(index>=0 && index<listItems.size()) {
            listItems.remove(index);

        }

    List<Long> prodIds = listItems.stream().map(OrderItemCreateDto::productId).filter(Objects::nonNull)
            .toList();
    List<Products> allProducts = productRepo.findAllById(prodIds);

    Map<Long,Products> productsWithIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));


    List<CartItemView> cartItems = new ArrayList<>();

    BigDecimal grandTotal = BigDecimal.ZERO;
    for(OrderItemCreateDto item:listItems){
        BigDecimal Bigquantity=BigDecimal.valueOf(item.quantity());
        BigDecimal subDisc= item.subDiscount()!=null ? item.subDiscount() :BigDecimal.ZERO;
        String name;
        BigDecimal sellingPrice;
        BigDecimal purchasePrice;

        if(item.productId()!=null){
            Products prod = productsWithIds.get(item.productId());
            name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() :prod.getName();
            sellingPrice= (item.customSellingPrice()!=null) ? item.customSellingPrice() :prod.getSellingPrice();
        }
        else{
            name=item.customName();
            sellingPrice=item.customSellingPrice()!=null ? item.customSellingPrice() :BigDecimal.ZERO;
        }

        BigDecimal subTotal = (sellingPrice.multiply(Bigquantity)) .subtract(subDisc);

        grandTotal=grandTotal.add(subTotal);

        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.barcode(),item.customName()
                ,item.customSellingPrice(),item.customPurchasePrice());
        cartItems.add(cartItem);

    }
    BigDecimal globalDiscount = createDto.discount()!=null ? createDto.discount() : BigDecimal.ZERO;
    grandTotal=grandTotal.subtract(globalDiscount);
    BigDecimal paid = createDto.paid()!=null ? createDto.paid() : BigDecimal.ZERO;
    grandTotal = addCartTaxTotals(model, grandTotal);

    BigDecimal applied = addPaymentPreview(model, createDto.paymentMethod(),
            createDto.paymentReference(), paid, grandTotal);
    BigDecimal remaining = grandTotal.subtract(applied);


    Long currentLastOrderNum = nextOrderNumber();
    String stordNum = currentLastOrderNum+"";

    model.addAttribute("orderNumber",stordNum);


    model.addAttribute("globalDiscount",globalDiscount);
    model.addAttribute("grandTotal",grandTotal);
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid",paid);
    model.addAttribute("remaining",remaining);
    model.addAttribute("currentCustomerId", createDto.customerId());

    return "fragments/cart ::cart";



}

@PostMapping("/check-out")

    public String checkOrderOut( @Valid @ModelAttribute OrderCreateDto currentCart,
                                BindingResult bindResult, HttpServletResponse response,Model model){



    if(bindResult.hasFieldErrors("itemsList")){

        throw new YouMustProvideAtLeastOneItem("please provide items");
    }

        if(bindResult.hasErrors()){

            response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");
            model.addAttribute("errorMessage", "Invalid order data provided.");
            return "fragments/auth-messages :: exceptions-response";
        }



    OrderResponseDto result = orderServo.createOrder(currentCart);

    response.setHeader("HX-Trigger", "{\"orderCompleted\": {\"orderId\": " + result.id() + "}}");
    model.addAttribute("globalDiscount", BigDecimal.ZERO);
    model.addAttribute("grandTotal", BigDecimal.ZERO);
    model.addAttribute("cartItems", List.of());
    model.addAttribute("paid", BigDecimal.ZERO);
    model.addAttribute("remaining", BigDecimal.ZERO);
    model.addAttribute("currentCustomerId", null);
    model.addAttribute("orderNumber", null);
    addCartTaxTotals(model, BigDecimal.ZERO);
    addPaymentPreview(model, PaymentMethod.CASH, null, BigDecimal.ZERO, BigDecimal.ZERO);

    return "fragments/cart ::cart";

}

@GetMapping("/print/{orderId}")

        public String printOrderById(@PathVariable Long orderId ,Model model){

OrderResponseDto found = orderServo.findOrderById(orderId);
model.addAttribute("order",found);

if(found.customerId()!=null){
CustomerViewDto cust = customerServo.findByCustomerId(found.customerId());
model.addAttribute("customerName",cust!=null ? cust.name() : "walk in customer");
}
else{
model.addAttribute("customerName","walk in customer");
}
    SettingsResponseDto settings = settingsServo.getSettings();
String printPage = switch(settings.printSize()){
    case A5-> "invoice-print-A5";
    case A4-> "invoice-print-A4";
    case THERMAL -> "invoice-print-thermal";


    };

return printPage;
}

@GetMapping("/returns")
@PreAuthorize("hasRole('ADMIN')")
public String viewSaleReturns(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                              Pageable pageable, Model model) {
    model.addAttribute("sales", orderServo.findOrdersForReturns(pageable));
    return "sale-returns :: sale-returns";
}

@GetMapping("/returns/{orderId}")
@PreAuthorize("hasRole('ADMIN')")
public String openSaleReturn(@PathVariable Long orderId, Model model) {
    OrderResponseDto sale = orderServo.findOrderById(orderId);
    model.addAttribute("sale", sale);
    model.addAttribute("returnItems", orderServo.getReturnableItems(orderId));
    model.addAttribute("returnHistory", orderServo.getSaleReturnHistory(orderId));
    model.addAttribute("returnRequest", new SaleReturnRequest(null, null, false, ""));
    return "fragments/sale-return-form :: sale-return-form";
}

@PostMapping("/returns/{orderId}")
@PreAuthorize("hasRole('ADMIN')")
public String processSaleReturn(@PathVariable Long orderId,
                                @Valid @ModelAttribute("returnRequest") SaleReturnRequest request,
                                BindingResult bindingResult,
                                HttpServletResponse response,
                                Model model) {
    if (bindingResult.hasErrors()) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#sale-return-error");
        response.setHeader("HX-Reswap", "innerHTML");
        model.addAttribute("errorMessage", bindingResult.getFieldError().getDefaultMessage());
        return "fragments/auth-messages :: exceptions-response";
    }

    try {
        orderServo.recordSaleReturn(orderId, request);
        model.addAttribute("sales", orderServo.findOrdersForReturns(PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        response.setHeader("HX-Trigger", "close-modal");
        return "sale-returns :: sale-returns";
    } catch (BusinessRuleException ex) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#sale-return-error");
        response.setHeader("HX-Reswap", "innerHTML");
        model.addAttribute("errorMessage", ex.getMessage());
        return "fragments/auth-messages :: exceptions-response";
    }
}

@PostMapping("/returns/{orderId}/void")
@PreAuthorize("hasRole('ADMIN')")
public String voidSale(@PathVariable Long orderId,
                       @RequestParam String reason,
                       HttpServletResponse response,
                       Model model) {
    try {
        orderServo.voidSale(orderId, reason);
        model.addAttribute("sales", orderServo.findOrdersForReturns(PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
        response.setHeader("HX-Trigger", "close-modal");
        return "sale-returns :: sale-returns";
    } catch (BusinessRuleException ex) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#sale-return-error");
        response.setHeader("HX-Reswap", "innerHTML");
        model.addAttribute("errorMessage", ex.getMessage());
        return "fragments/auth-messages :: exceptions-response";
    }
}

@GetMapping("order/{orderId}")
    public String viewExistingOrderPage(@PathVariable Long orderId, Model model){

   OrderResponseDto response=  orderServo.findOrderById(orderId);

   List<OrderItemCreateDto> items = response.items().stream().map(it->{
      return new OrderItemCreateDto(it.productId(),it.quantity(),it.subDiscount(),it.barcode(),it.productName(),it.productSellingPrice(),it.productPurchasePrice());

   }).toList();

   BigDecimal tenderedAmount = response.paymentMethod() == PaymentMethod.CASH
           ? response.cashReceived() : response.paid();
   OrderUpdateDto update = new OrderUpdateDto(response.discount(), response.customerId(), items, tenderedAmount,
           response.remaining(), response.orderNumber(), response.paymentMethod(), response.paymentReference());

   Long ordId = response.id();


















    //start

    List<Long> prodIds = items.stream().map(OrderItemCreateDto::productId).filter(Objects::nonNull)
            .toList();
    List<Products> allProducts = productRepo.findAllById(prodIds);

    Map<Long,Products> productsWithIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));


    List<CartItemView> cartItems = new ArrayList<>();

    for(OrderItemCreateDto item:items){
        BigDecimal Bigquantity=BigDecimal.valueOf(item.quantity());
        BigDecimal subDisc= item.subDiscount()!=null ? item.subDiscount() :BigDecimal.ZERO;
        String name;
        BigDecimal sellingPrice;
        BigDecimal purchasePrice;

        if(item.productId()!=null){
            Products prod = productsWithIds.get(item.productId());
            name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() :prod.getName();
            sellingPrice= (item.customSellingPrice()!=null) ? item.customSellingPrice() :prod.getSellingPrice();
        }
        else{
            name=item.customName();
            sellingPrice=item.customSellingPrice()!=null ? item.customSellingPrice() :BigDecimal.ZERO;
        }

        BigDecimal subTotal = (sellingPrice.multiply(Bigquantity)) .subtract(subDisc);


        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.barcode(),item.customName()
                ,item.customSellingPrice(),item.customPurchasePrice());
        cartItems.add(cartItem);

    }



    model.addAttribute("globalDiscount",response.discount());
    model.addAttribute("grandTotal",response.total());
    model.addAttribute("taxableSubtotal",response.total().subtract(response.taxAmount()));
    model.addAttribute("taxRate",response.taxRate());
    model.addAttribute("taxAmount",response.taxAmount());
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid", tenderedAmount);
    model.addAttribute("paymentMethod", response.paymentMethod());
    model.addAttribute("paymentReference", response.paymentReference());
    model.addAttribute("amountApplied", response.paid());
    model.addAttribute("cashChange", response.cashChange());
    model.addAttribute("remaining",response.remaining());
    model.addAttribute("currentCustomerId", response.customerId());
    model.addAttribute("ordId",ordId);
    model.addAttribute("update",update);
    model.addAttribute("orderNumber",response.orderNumber());












    //end













    return "fragments/cart :: cart";

}

@PatchMapping("/update/{ordId}")

        public String updateOrderById(@PathVariable Long ordId, @Valid @ModelAttribute OrderUpdateDto update,
                                      BindingResult bindResult, HttpServletResponse response, Model model){



    if(bindResult.hasErrors()){

        response.setHeader("Hx-Retarget" ,"#cart-zone");
        response.setHeader("Hx-Reswap","innerHTML");
        return "fragments/cart ::cart";

    }

    OrderResponseDto result =  orderServo.updateOrder(ordId,update);


    response.setHeader("HX-Trigger", "{\"orderCompleted\": {\"orderId\": " + result.id() + "}}");

model.addAttribute("ordId",null);

model.addAttribute("grandTotal",BigDecimal.ZERO);
    model.addAttribute("paid",BigDecimal.ZERO);

    model.addAttribute("remaining",BigDecimal.ZERO);

    model.addAttribute("globalDiscount",BigDecimal.ZERO);
    addCartTaxTotals(model, BigDecimal.ZERO);
    addPaymentPreview(model, PaymentMethod.CASH, null, BigDecimal.ZERO, BigDecimal.ZERO);
    List<CartItemView> items = new ArrayList<>();
model.addAttribute("cartItems",items);

List<Customer> customers = customerRepo.findAll();
model.addAttribute("customers",customers);
    return "fragments/cart ::cart";

}


@GetMapping("/search")

    public String findOrderByNumber(@RequestParam("orderNum") String orderNumber , Model model){


    List<OrderResponseDto> results = orderServo.findByOrderNumber(orderNumber);
    model.addAttribute("orderResults",results);


    return "fragments/order-search-results :: order-results-fragment";



    }

@DeleteMapping("/clear")
    public String clearCart( Model model ){


List<CartItemView> emptyList = new ArrayList<>();


    model.addAttribute("globalDiscount",BigDecimal.ZERO);
    model.addAttribute("grandTotal",BigDecimal.ZERO);
    model.addAttribute("cartItems",emptyList);
    model.addAttribute("paid",BigDecimal.ZERO);
    model.addAttribute("remaining",BigDecimal.ZERO);
    addCartTaxTotals(model, BigDecimal.ZERO);
    addPaymentPreview(model, PaymentMethod.CASH, null, BigDecimal.ZERO, BigDecimal.ZERO);
    model.addAttribute("currentCustomerId", null);
    model.addAttribute("ordId", null);
    model.addAttribute("orderNumber", null);
    return "fragments/cart ::cart";


    }

    @PostMapping("/custom")

    public String addCustomRowToCart(@ModelAttribute OrderCreateDto createDto, Model model,@RequestParam(required = false) Long ordId){

    System.out.println("adding custom row to cart");

        if(ordId!=null){
            model.addAttribute("ordId",ordId);
        }


        List<OrderItemCreateDto> listItems = new ArrayList<>();
        if(createDto.itemsList()!=null){

            listItems.addAll(createDto.itemsList());
        }

        OrderItemCreateDto cutomItemAdded = new OrderItemCreateDto(null,1,BigDecimal.ZERO
        ,null,"customItem",BigDecimal.ZERO,BigDecimal.ZERO);

        listItems.add(cutomItemAdded);



        listItems.removeIf(it->it.quantity()<=0);

        List<Long> prodIds = listItems.stream().map(OrderItemCreateDto::productId).filter(Objects::nonNull)
                .toList();
        List<Products> allProducts = productRepo.findAllById(prodIds);

        Map<Long,Products> productsWithIds = allProducts.stream().collect(Collectors.toMap(Products::getId,p->p));


        List<CartItemView> cartItems = new ArrayList<>();

        BigDecimal grandTotal = BigDecimal.ZERO;
        for(OrderItemCreateDto item:listItems){

            BigDecimal Bigquantity=BigDecimal.valueOf(item.quantity());
            BigDecimal subDisc= item.subDiscount()!=null ? item.subDiscount() :BigDecimal.ZERO;
            String name;
            BigDecimal sellingPrice;
            BigDecimal purchasePrice;

            if(item.productId()!=null){
                Products prod = productsWithIds.get(item.productId());
                name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() : prod.getName();
                sellingPrice= (item.customSellingPrice()!=null) ? item.customSellingPrice() : prod.getSellingPrice();

            }
            else{
                name=item.customName();
                sellingPrice=item.customSellingPrice()!=null ? item.customSellingPrice() :BigDecimal.ZERO;
            }
            BigDecimal subTotal = (sellingPrice.multiply(Bigquantity)) .subtract(subDisc);

            grandTotal=grandTotal.add(subTotal);

            CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.barcode(),item.customName()
                    ,item.customSellingPrice(),item.customPurchasePrice());

            cartItems.add(cartItem);
        }
        BigDecimal globalDiscount = createDto.discount()!=null ? createDto.discount() : BigDecimal.ZERO;
        grandTotal=grandTotal.subtract(globalDiscount);

        BigDecimal paid = createDto.paid()!=null ? createDto.paid() : BigDecimal.ZERO;
        grandTotal = addCartTaxTotals(model, grandTotal);

        BigDecimal applied = addPaymentPreview(model, createDto.paymentMethod(),
                createDto.paymentReference(), paid, grandTotal);
        BigDecimal remaining = grandTotal.subtract(applied);

        Long currentLastOrderNum = nextOrderNumber();
        String stordNum = currentLastOrderNum+"";

        model.addAttribute("orderNumber",stordNum);



        model.addAttribute("globalDiscount",globalDiscount);
        model.addAttribute("grandTotal",grandTotal);
        model.addAttribute("cartItems",cartItems);
        model.addAttribute("paid",paid);
        model.addAttribute("remaining",remaining);
        model.addAttribute("currentCustomerId", createDto.customerId());


        return "fragments/cart ::cart";    }

    @GetMapping("/customer")
public String findOrdersByCustomerName(@RequestParam(name ="custName") String name, @PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.ASC) Pageable pageable,Model model){

        Page<OrderResponseDto> results  = orderServo.findByCustomerName(name,pageable);

    model.addAttribute("orderResults",results);

    return "fragments/order-search-results :: order-results-fragment";
    }

    @GetMapping("/summary")
    public String getSummaryPage(){

    return "summary";
    }

    @GetMapping({"/CustomerOrderSummary", "/CustomerOrderSummary/{id}"})
    public String getCustomerOrderSummary(@PageableDefault Pageable pageable,
                                          @PathVariable(name = "id", required = false) Long pathId,
                                          @RequestParam(name = "customerId", required = false) Long customerId,
                                          @RequestParam(name="dateRange", required =false ) String dateRange,
                                          Model model){

        Long id = pathId != null ? pathId : customerId;
        if (id == null) {
            return "fragments/order-sum-res-fragment :: select-customer";
        }
        LocalDate[] dates = parseCustomerSummaryDateRange(dateRange);
        if (dates == null) {
            return "fragments/order-sum-res-fragment :: summary-error";
        }

        LocalDateTime start = dates[0].atStartOfDay();
        LocalDateTime end = dates[1].atTime(LocalTime.MAX);

      CustomerSummary summary = orderServo.getCustomerSummaryInPeriodById(id,start,end,pageable);
    model.addAttribute("summary",summary);
     model.addAttribute("customerId",id);
     model.addAttribute("orders",summary.orders());
    return "fragments/order-sum-res-fragment :: summary-result";
}

    private static LocalDate[] parseCustomerSummaryDateRange(String dateRange) {
        if (dateRange == null || dateRange.isBlank()) {
            LocalDate today = LocalDate.now();
            return new LocalDate[]{today.minusMonths(1), today};
        }

        String[] parts = dateRange.trim().split("\\s+to\\s+", -1);
        if (parts.length == 1) {
            parts = new String[]{parts[0], parts[0]};
        }
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        try {
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            return start.isAfter(end) ? null : new LocalDate[]{start, end};
        } catch (DateTimeParseException exception) {
            return null;
        }
    }



}
