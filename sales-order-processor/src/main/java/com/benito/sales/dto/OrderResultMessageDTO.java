package com.benito.sales.dto;

public class OrderResultMessageDTO {

    public String orderId;
    public String status;
    public String rejectionReason;

    public OrderResultMessageDTO() { }

    public OrderResultMessageDTO(String orderId, String status, String rejectionReason)
    {
        this.orderId = orderId;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }
}
