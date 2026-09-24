package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.exceptions.*;
import com.connectors.pos.ordersystem.orderdtos.*;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.parser.Parser;
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
        BigDecimal orderDiscount = create.discount() != null ? create.discount() : BigDecimal.ZERO;

        for (OrderItemCreateDto item : itemsDto) {
            String name;
            BigDecimal purchasePrice;
            BigDecimal sellingPrice;
            Products prod = null;
            Long stock;
            BigDecimal discount = item.subDiscount() != null ? item.subDiscount() : BigDecimal.ZERO;
            Long prodId = item.productId();
            int quantity = item.quantity();
            if (prodId == null) {
                if (item.customName() == null || item.customSellingPrice() == null) {
                    throw new IllegalArgumentException("name and selling price are required for custom items");
                }
                name = item.customName();
                sellingPrice = item.customSellingPrice();
                purchasePrice = item.customPurchasePrice() != null ? item.customPurchasePrice() : BigDecimal.ZERO;
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
            BigDecimal subTotal = (sellingPrice.multiply(BigDecimal.valueOf(quantity))).subtract(discount);
            total = total.add(subTotal);

            BigDecimal margin = (sellingPrice.subtract(purchasePrice)).multiply(BigDecimal.valueOf(quantity));
            BigDecimal subRevenue = margin.subtract(discount);

            revenue = revenue.add(subRevenue);


        }


        total = total.subtract(orderDiscount);
        revenue = revenue.subtract(orderDiscount);

BigDecimal remaining = total.subtract(create.paid());

order.setRemaining(remaining);
        order.setPaid(create.paid());
        order.setDiscount(create.discount());
        order.setTotal(total);
        order.setRevenue(revenue);

String orderNumber = generateOrderNumber();

        order.setOrderNumber(orderNumber);

        Order savedOrder = orderRepo.save(order);

        return orderMapper.toResponse(savedOrder);
    }



    public String generateOrderNumber() {
Long nextOrderNumber = orderRepo.getNextOrderSequence();

        return nextOrderNumber+1000 + "";
    }



    @Transactional(readOnly = true)
    public OrderResponseDto findOrderById(Long id){

        Order found = orderRepo.findById(id)
                .orElseThrow(()-> new EntityNotFoundException(""));


        return orderMapper.toResponse(found);

    }



   @Transactional
    public OrderResponseDto updateOrder(Long orderId , OrderUpdateDto update){

        Order order = orderRepo.findById(orderId)
                .orElseThrow(()->new OrderNotFoundException("order wasn't found"));


           List<OrderItemCreateDto> items = update.itemsList();
           List<Long> allProdIds =items.stream().map(OrderItemCreateDto::productId)
                   .filter(Objects::nonNull)
                   .toList();

           Map<Long,Products> prodsWithIds = productRepo.findAllByIds(allProdIds).stream()
                   .collect(Collectors.toMap(Products::getId,p->p));

           for(OrderItem oldItem : order.getOrderItems()){

               if(oldItem.getProduct()!=null && productRepo.checkActiveStatus(oldItem.getProduct().getId(),true)){
        Products prod = oldItem.getProduct();

        prod.setStock(prod.getStock()+oldItem.getQuantity());

               }
               }

           order.getOrderItems().clear();



  BigDecimal total = BigDecimal.ZERO;
BigDecimal revenue = BigDecimal.ZERO;
BigDecimal orderDiscount = update.discount()!=null ? update.discount() : BigDecimal.ZERO;

   for(OrderItemCreateDto item : items){
       BigDecimal bigQuantity = BigDecimal.valueOf(item.quantity());
       BigDecimal subDiscount = item.subDiscount()!=null ? item.subDiscount() : BigDecimal.ZERO;
        String name;
        BigDecimal price;
         BigDecimal purchase;
         Products prod = null;
        if(item.productId()==null){
            name = item.customName();
            price = item.customSellingPrice();
            purchase = item.customPurchasePrice()!=null? item.customPurchasePrice() :BigDecimal.ZERO;
        }

        else{
            prod = prodsWithIds.get(item.productId());
            if(productRepo.checkActiveStatus(item.productId(),false)){

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

        BigDecimal subTotal = (price.multiply(bigQuantity)).subtract(subDiscount);

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

total = total.subtract(orderDiscount);
revenue = revenue.subtract(orderDiscount);

BigDecimal paid = update.paid()!=null ? update.paid() : BigDecimal.ZERO;
order.setTotal(total);
order.setRevenue(revenue);
order.setDiscount(orderDiscount);
order.setPaid(paid);
order.setRemaining(total.subtract(paid));

Order savedOrder = orderRepo.save(order);

return orderMapper.toResponse(savedOrder);
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
          System.out.println(res.getTotalElements()+ "contents"+ res.getContent().indexOf(1));
        Object[] result = orderRepo.sumAllOrdersSummaryBetweenDatesById(id,start,end);
       Object[] row = (Object[]) result[0];

 BigDecimal totalSelling = (BigDecimal) row[0];
 BigDecimal totalPaid = (BigDecimal) row[1];
 BigDecimal totaLRemaining = (BigDecimal) row[2];

 Page<OrderResponseDto> response = res.map(orderMapper::toResponse);
  Customer found = customerRepo.findById(id).orElseThrow(()-> new EntityNotFoundException("customer wasn't found"));
  String name = found.getName();
 CustomerSummary summary  = new CustomerSummary(name,start,end,totalSelling,totalPaid,totaLRemaining,response);
       return summary;
   }

}
