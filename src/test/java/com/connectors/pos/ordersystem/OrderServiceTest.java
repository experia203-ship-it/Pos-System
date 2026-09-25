package com.connectors.pos.ordersystem;

import com.connectors.pos.customersystem.Customer;
import com.connectors.pos.customersystem.CustomerRepository;
import com.connectors.pos.exceptions.InsuffecientStockException;
import com.connectors.pos.ordersystem.orderdtos.OrderCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderItemCreateDto;
import com.connectors.pos.ordersystem.orderdtos.OrderMapper;
import com.connectors.pos.ordersystem.orderdtos.OrderResponseDto;
import com.connectors.pos.products.ProductRepository;
import com.connectors.pos.products.Products;
import com.connectors.pos.security.UserPrincipal;
import com.connectors.pos.users.UserRepository;
import com.connectors.pos.users.Users;
import org.h2.command.dml.MergeUsing;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private CustomerRepository customerRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private ProductRepository prodRepo;
    @Mock
    private OrderRepository orderRepo;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;
    @InjectMocks
    private OrderService orderServo;
    @Spy
    private OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @AfterEach
    void clearHolder() {


         SecurityContextHolder.clearContext();
}

    @Test
    void createOrderWithSuccess() {


        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getId()).thenReturn(1L);
        when(mockPrincipal.getUsername()).thenReturn("cashier_01");
            Authentication mockAuth = mock(Authentication.class);
     when(mockAuth.isAuthenticated()).thenReturn(true);
            when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        SecurityContext mockContext= mock(SecurityContext.class);

        when(mockContext.getAuthentication()).thenReturn(mockAuth);
        SecurityContextHolder.setContext(mockContext);

        Order newOrder = Order.builder()
                .userName("cashier_01")
                .total(new BigDecimal("150.00"))
                .discount(new BigDecimal("10.00"))
                .revenue(new BigDecimal("140.00"))
                .paid(new BigDecimal("150.00"))
                .orderNumber("ORD-2026-001")
                .user(null)
                .customer(null)
                .build();
   when(orderRepo.save(any(Order.class))).thenReturn(newOrder);

   when(customerRepo.getReferenceById(any(Long.class))).thenReturn(new Customer());
   when(userRepo.getReferenceById(any(Long.class))).thenReturn(new Users());
when(prodRepo.findAllByIds(anyList())).thenReturn(new ArrayList<Products>());
   when(orderNumberGenerator.nextValue()).thenReturn(1L);


        OrderItemCreateDto dummyItem = new OrderItemCreateDto(
                null,
                1,
                BigDecimal.ZERO,
                   null,
                "Custom Part",
                new BigDecimal("160.00"),
                new BigDecimal("100.00")
        );
        List<OrderItemCreateDto> items = new ArrayList<>();
        items.add(dummyItem);

OrderCreateDto dto = new OrderCreateDto(new BigDecimal("10.00"),1L,items,
           new BigDecimal("150.00"),new BigDecimal("33.00"),1L);


        OrderResponseDto response =    orderServo.createOrder(dto);

        assertThat(response).isNotNull();
        assertThat(response.orderNumber()).isEqualTo("ORD-2026-001");
        assertThat(response.total()).isEqualTo(new BigDecimal("150.00"));

verify(orderRepo).save(any(Order.class));
    }


    @Test

    void createOrderWithFailure(){
        UserPrincipal mockPrincipal = mock(UserPrincipal.class);
        when(mockPrincipal.getId()).thenReturn(1L);
        when(mockPrincipal.getUsername()).thenReturn("cashier_01");
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.isAuthenticated()).thenReturn(true);
        when(mockAuth.getPrincipal()).thenReturn(mockPrincipal);
        SecurityContext mockContext= mock(SecurityContext.class);

        when(mockContext.getAuthentication()).thenReturn(mockAuth);
        SecurityContextHolder.setContext(mockContext);

        Products prod =  Products.builder()
                .id(1L).stock(20L).build();


        OrderItemCreateDto item = new OrderItemCreateDto(1L,30,new BigDecimal("2.00"),null,null,null,null);
        List<OrderItemCreateDto> items = new ArrayList<>();
        items.add(item);
        OrderCreateDto order = new OrderCreateDto(new BigDecimal("2.00"),1L,items,new BigDecimal("2.00"),new BigDecimal("2.00"),123L);
        List<Long> prodIds = items.stream().map(OrderItemCreateDto::productId).toList();
        when(prodRepo.findAllByIds(prodIds)).thenReturn(List.of(prod));

      InsuffecientStockException ex =  assertThrows(InsuffecientStockException.class,()->{orderServo.createOrder(order)
        ;

        });

assertThat(ex.getMessage()).isEqualTo("insuffesient stock the availabele quantity is :"+prod.getName()+"  " +prod.getStock());
    }


}