package com.benito.sales.service;

import com.benito.sales.dto.OrderMessageDTO;
import com.benito.sales.dto.OrderResultMessageDTO;
import com.benito.sales.dto.restClientDTO.BalanceRequestDTO;
import com.benito.sales.entity.OrderStatus;
import com.benito.sales.entity.OrdersPayments;
import com.benito.sales.exception.*;
import com.benito.sales.repository.OrdersPaymentsRepository;
import com.benito.sales.restClient.ClientsApiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.time.Instant;

@ApplicationScoped
public class OrderProcessorService {

    //DEPENDENCY INJECTION -> Repository, Rest Client, Kafka emitter, service token & JSON mapper
    private final OrdersPaymentsRepository _paymentsRepository;
    private final ClientsApiClient _clientsApi;
    private final Emitter<String> _emitter;
    private final ServiceTokenProvider _serviceTokenProvider;
    private final ObjectMapper _objectMapper;

    public OrderProcessorService(OrdersPaymentsRepository paymentsRepository,
                                 @RestClient ClientsApiClient clientsApi,
                                 @Channel("order-results-out") Emitter<String> emitter,
                                 ServiceTokenProvider serviceTokenProvider,
                                 ObjectMapper objectMapper)
    {
        this._paymentsRepository = paymentsRepository;
        this._clientsApi = clientsApi;
        this._emitter = emitter;
        this._serviceTokenProvider = serviceTokenProvider;
        this._objectMapper = objectMapper;
    }


    //CHARGES THE CLIENT FOR AN ORDER AND PUBLISHES THE RESULT (CALLED BY THE KAFKA CONSUMER)
    public void processPayment(OrderMessageDTO order)
    {
        OrdersPayments payment = _paymentsRepository.findByOrderId(order.orderId);

        //IDEMPOTENCY
        if (payment != null && payment.status != OrderStatus.PROCESSING)
        {
            publishResult(payment);
            return;
        }

        //FIRST TIME WE SEE THIS ORDER: WE WRITE DOWN THAT ITS PAYMENT STARTED
        if (payment == null)
        {
            payment = new OrdersPayments();
            payment.orderId = order.orderId;
            payment.clientId = order.clientId;
            payment.amount = order.amount;
            payment.status = OrderStatus.PROCESSING;
            payment.createdAt = Instant.now();

            _paymentsRepository.persist(payment);
        }

        try
        {
            //SERVICE REQUEST HIS OWN TOKEN AND CHARGES THE CLIENT
            String serviceToken = _serviceTokenProvider.getAuthorizationToken();
            chargeClient(serviceToken, order.clientId, order.amount, order.orderId);

            payment.status = OrderStatus.COMPLETED;
            payment.rejectionReason = null;
        }
        catch (InsufficientBalanceException | ClientNotFoundException | ClientAccessDeniedException
               | ConflictUpdateException exception)
        {
            //BUSINESS ERRORS: THE CLIENT WAS NOT CHARGED, THE ORDER IS REJECTED
            payment.status = OrderStatus.REJECTED;
            payment.rejectionReason = exception.getMessage();
        }

        payment.updatedAt = Instant.now();
        _paymentsRepository.update(payment);

        publishResult(payment);
    }




    //HELPERS
    private void publishResult(OrdersPayments payment)
    {
        OrderResultMessageDTO result = new OrderResultMessageDTO(payment.orderId,
                                                                 payment.status.name(),
                                                                 payment.rejectionReason);
        try
        {
            _emitter.send(KafkaRecord.of(payment.orderId, _objectMapper.writeValueAsString(result)));
        }
        catch (JsonProcessingException exception)
        {
            throw new IllegalStateException("The result of order " + payment.orderId + " could not be serialized", exception);
        }
    }

    //CALLS THE CLIENTS SERVICE TO SUBTRACT THE ORDER TOTAL FROM THE CLIENT'S BALANCE
    private void chargeClient(String authorization, String clientId, BigDecimal amount, String orderId)
    {
        try
        {
            _clientsApi.debitBalance(authorization, clientId, new BalanceRequestDTO(amount, orderId));
        }
        catch (WebApplicationException exception)
        {
            switch (exception.getResponse().getStatus())
            {
                case 422: throw new InsufficientBalanceException();
                case 404: throw new ClientNotFoundException(clientId);
                case 403: throw new ClientAccessDeniedException();
                case 409: throw new ConflictUpdateException();
                default: throw new ExternalServiceException("clients");
            }
        }
        catch (ProcessingException exception)
        {
            throw new ServiceNotAvailableException("clients");
        }
    }
}
