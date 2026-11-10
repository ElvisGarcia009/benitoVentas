package com.benito.sales.dto.KafkaDTO;

//MESSAGE THAT SALES-ORDER-PROCESSOR PUBLISHES IN THE "order-results" TOPIC
public class OrderResultMessageDTO {

    public String orderId;
    public String status;           //COMPLETED OR REJECTED
    public String rejectionReason;  //NULL WHEN THE ORDER WAS COMPLETED

    public OrderResultMessageDTO() { }

    public OrderResultMessageDTO(String orderId, String status, String rejectionReason)
    {
        this.orderId = orderId;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
