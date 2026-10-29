package com.benito.sales.resource;

import com.benito.sales.dto.OrderRequestDTO;
import com.benito.sales.dto.OrderResponseDTO;
import com.benito.sales.dto.OrdersPaginatedDTO;
import com.benito.sales.dto.TemporalOrderResponseDTO;
import com.benito.sales.exception.InvalidIdException;
import com.benito.sales.service.OrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;



@Path("api/v1/order")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    //DEPENDENCY INJECTION -> Service & jwt
    private final OrderService _orderService;
    private final JsonWebToken jwt;
    public OrderResource(OrderService orderService, JsonWebToken jwt)
    {
        this._orderService = orderService;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed("admin")
    public OrdersPaginatedDTO getAllOrders(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                           @QueryParam("pageSize") @Positive @DefaultValue("10") @Max(30) int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _orderService.getAllOrders(page, pageSize);
    }

    @GET
    @Path("/me")
    @RolesAllowed("client")
    public OrdersPaginatedDTO getMyOrders(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                          @QueryParam("pageSize") @Positive @DefaultValue("10") @Max(30) int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _orderService.getMyOrders(jwt.getClaim("userId"), page, pageSize);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"admin", "client"})
    public OrderResponseDTO getOrderById(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        boolean isAdmin = jwt.getGroups().contains("admin");

        //Quarkus return this with a 200 status code
        return _orderService.getOrderById(validatedId, jwt.getClaim("userId"), isAdmin);
    }


    @POST
    @RolesAllowed({"admin", "client"})
    public RestResponse<TemporalOrderResponseDTO> createPurchaseOrder(@Valid OrderRequestDTO requestOrder)
    {
        validateId(requestOrder.clientId);

        TemporalOrderResponseDTO processedOrder = _orderService.createPurchaseOrder(requestOrder,
                                                                                    jwt.getRawToken(),
                                                                                    jwt.getSubject());

        return RestResponse.accepted(processedOrder);
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public RestResponse<Void> deleteOrder(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        _orderService.deleteOrder(validatedId, jwt.getSubject());
        return RestResponse.noContent();
    }




    //HELPERS
    private ObjectId validateId(String id)
    {
        if (!ObjectId.isValid(id))
            throw new InvalidIdException(id);

        return new ObjectId(id);
    }
}