package com.mattstine.dddworkshop.pizzashop.ordering;

interface OrderingService {
    OnlineOrderRef createOrder(OnlineOrder.Type type);

    void addPizza(OnlineOrderRef ref, Pizza pizza);

    void requestPayment(OnlineOrderRef ref);

    OnlineOrder findByRef(OnlineOrderRef ref);
}
