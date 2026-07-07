package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.customersystem.CustomerService;
import com.connectors.pos.exceptions.CustomerMustBeProvidedException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.exceptions.YouMustProvideAtLeastOneItem;
import com.connectors.pos.ordersystem.orderdtos.*;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/pos")
public class OrderController {
   private final CustomerRepository customerRepo;
    private final OrderService orderServo;
private final ProductRepository productRepo;

private final CustomerService customerServo;


@ModelAttribute("customers")
public List<Customer> populateCustomers(){

    return customerRepo.findAll();
}



    @GetMapping
    public String viewPosPage(){


        return "pos";

    }


@PostMapping("/cart/add/{prodId}")


    public String addNewItemToCart(@PathVariable Long prodId ,@ModelAttribute OrderCreateDto currentCart,Model model){
 List<OrderItemCreateDto> listItems= new ArrayList<>();
        if(currentCart.itemsList()!=null && !(currentCart.itemsList().isEmpty())){
            listItems.addAll(currentCart.itemsList());

         }


            Optional<OrderItemCreateDto> opt = listItems.stream().filter(it->Objects.equals(it.productId(),prodId))
                    .findFirst();


            if(opt.isPresent()){

                OrderItemCreateDto alreadyExistsItem = opt.get();

                int newQ =alreadyExistsItem.quantity()+1;
                OrderItemCreateDto modified = new OrderItemCreateDto(alreadyExistsItem.productId(),newQ,alreadyExistsItem.subDiscount(),alreadyExistsItem.customName()
                        ,alreadyExistsItem.customSellingPrice(),alreadyExistsItem.customPurchasePrice());

                int index = listItems.indexOf(alreadyExistsItem);
              listItems.set(index,modified);
        }

else {
                Products newProd = productRepo.findById(prodId)
                        .orElseThrow(() -> new ProductNotFoundException("product doesn't exist with that id: " + prodId));

                OrderItemCreateDto newItem = new OrderItemCreateDto(newProd.getId(), 1, BigDecimal.ZERO, null, null, null);

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
                                ,subDiscount,price,subTotal,item.customName(),item.customSellingPrice(),item.customPurchasePrice()));

}
BigDecimal discount = currentCart.discount()!=null ? currentCart.discount() :BigDecimal.ZERO;
grandTotal =grandTotal.subtract(discount);

BigDecimal paid = currentCart.paid()!=null ? currentCart.paid() : BigDecimal.ZERO;

BigDecimal remaining = currentCart.paid()!=null  ? (grandTotal.subtract(paid)) : BigDecimal.ZERO;

model.addAttribute("globalDiscount",discount);
model.addAttribute("grandTotal",grandTotal);
model.addAttribute("cartItems",cartItems);
model.addAttribute("paid",paid);
model.addAttribute("remaining",remaining);
model.addAttribute("currentCustomerId", currentCart.customerId());





return "fragments/cart :: cart";
}



@PostMapping("/cart/update")
    public String updateCartInfo(@ModelAttribute OrderCreateDto createDto, Model model){



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
                name= (item.customName()!=null&&!item.customName().isBlank()) ? item.customName() :prod.getName();
                sellingPrice= (item.customSellingPrice()!=null) ? item.customSellingPrice() :prod.getSellingPrice();
            }
            else{
                name=item.customName();
                sellingPrice=item.customSellingPrice()!=null ? item.customSellingPrice() :BigDecimal.ZERO;
            }

            BigDecimal subTotal = (sellingPrice.multiply(Bigquantity)) .subtract(subDisc);

            grandTotal=grandTotal.add(subTotal);

        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.customName()
        ,item.customSellingPrice(),item.customPurchasePrice());

             cartItems.add(cartItem);
        }
BigDecimal globalDiscount = createDto.discount()!=null ? createDto.discount() : BigDecimal.ZERO;
grandTotal=grandTotal.subtract(globalDiscount);

    BigDecimal paid = createDto.paid()!=null ? createDto.paid() : BigDecimal.ZERO;

    BigDecimal remaining = createDto.paid()!=null  ? (grandTotal.subtract(paid)) : BigDecimal.ZERO;


    model.addAttribute("globalDiscount",globalDiscount);
    model.addAttribute("grandTotal",grandTotal);
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid",paid);
    model.addAttribute("remaining",remaining);
    model.addAttribute("currentCustomerId", createDto.customerId());


return "fragments/cart ::cart";
}


@PostMapping("/cart/remove/{index}")

    public String removeAnItem(@ModelAttribute OrderCreateDto createDto ,@PathVariable("index") int index ,Model model){

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

        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.customName()
                ,item.customSellingPrice(),item.customPurchasePrice());
        cartItems.add(cartItem);

    }
    BigDecimal globalDiscount = createDto.discount()!=null ? createDto.discount() : BigDecimal.ZERO;
    grandTotal=grandTotal.subtract(globalDiscount);
    BigDecimal paid = createDto.paid()!=null ? createDto.paid() : BigDecimal.ZERO;

    BigDecimal remaining = createDto.paid()!=null  ? (grandTotal.subtract(paid)) : BigDecimal.ZERO;


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

    if(bindResult.hasFieldErrors("customerId")){
        throw new CustomerMustBeProvidedException("customer must be provided");
    }

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


    return "fragments/cart ::cart";

}

@GetMapping("/print/{orderId}")

        public String printOrderById(@PathVariable Long orderId ,Model model){

OrderResponseDto found = orderServo.findOrderById(orderId);
model.addAttribute("order",found);

if(found.customerId()!=null){
Customer cust = customerServo.findByCustomerId(found.customerId());
model.addAttribute("customerName",cust!=null ? cust.getName() : "walk in customer");
}
else{
model.addAttribute("customerName","walk in customer");
}
return "invoice-print";
}

@GetMapping("order/{orderId}")
    public String viewExistingOrderPage(@PathVariable Long orderId, Model model){

   OrderResponseDto response=  orderServo.findOrderById(orderId);

   List<OrderItemCreateDto> items = response.items().stream().map(it->{
      return new OrderItemCreateDto(it.productId(),it.quantity(),it.subDiscount(),it.productName(),it.productSellingPrice(),it.productPurchasePrice());

   }).toList();

   OrderUpdateDto update = new OrderUpdateDto(response.discount(),response.customerId(),items,response.paid()
   ,response.remaining(),response.orderNumber());

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


        CartItemView cartItem = new CartItemView(item.productId(),name,item.quantity(),subDisc,sellingPrice, subTotal,item.customName()
                ,item.customSellingPrice(),item.customPurchasePrice());
        cartItems.add(cartItem);

    }



    model.addAttribute("globalDiscount",response.discount());
    model.addAttribute("grandTotal",response.total());
    model.addAttribute("cartItems",cartItems);
    model.addAttribute("paid",response.paid());
    model.addAttribute("remaining",response.remaining());
    model.addAttribute("currentCustomerId", response.customerId());
    model.addAttribute("ordId",ordId);
    model.addAttribute("update",update);












    //end













    return "fragments/cart :: cart";

}

@PatchMapping("/update/{ordId}")

        public String updateOrderById( @PathVariable Long ordId ,@ModelAttribute OrderUpdateDto update , BindingResult bindResult,HttpServletResponse response , Model model){



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
    model.addAttribute("currentCustomerId", null);
    model.addAttribute("ordId", null);
    model.addAttribute("orderNumber", null);
    return "fragments/cart ::cart";


    }
}
