package com.order.service;

import com.order.dto.InventoryResponse;
import com.order.dto.OrderLineItemsDto;
import com.order.dto.OrderRequest;
import com.order.model.Order;
import com.order.model.OrderLineItems;
import com.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {

        Order order=new Order();

        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems=orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        System.out.printf("order data "+order);

        if(orderRepository==null) System.out.println("OrderRepository is null");

            assert orderRepository != null;

        /*
        * Call Inventory Service and place order if product is in Stock.
        * */
            List<String> skuCodes=order.getOrderLineItemsList().stream()
                    .map(OrderLineItems::getSkuCode)
                    .toList();
        InventoryResponse [] inventoryResponsesArray=webClient.get()
                .uri("http://localhost:8082/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        assert inventoryResponsesArray != null;
        boolean allProductsInStock= Arrays.stream(inventoryResponsesArray)
                .allMatch(InventoryResponse::isInStock);

       if(allProductsInStock)
       {
           orderRepository.save(order);
       }
       else {
           throw  new IllegalArgumentException("Product is not in Stock, Please try again later....");
       }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {

        OrderLineItems orderLineItems=new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        return orderLineItems;
    }

}
