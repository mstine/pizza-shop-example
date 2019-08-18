package com.mattstine.dddworkshop.pizzashop.infrastructure.repository.ports;

public interface Repository<K extends Ref, T extends Aggregate, S extends AggregateState, U extends AggregateEvent, V extends RepositoryAddEvent> {
    K nextIdentity();

    void add(T aggregateInstance);

    T findByRef(K ref);
}
