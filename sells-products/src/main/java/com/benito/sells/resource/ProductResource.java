package com.benito.sells.resource;

import com.benito.sells.dto.ProductRequestDTO;
import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.exception.InvalidIdException;
import com.benito.sells.service.ProductService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.resteasy.reactive.RestResponse;

import java.net.URI;

@Path("api/v1/product")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    //DEPENDENCY INJECTION -> Service & jwt
    private final ProductService _productService;
    private final JsonWebToken jwt;
    public ProductResource(ProductService productService, JsonWebToken jwt)
    {
        this._productService = productService;
        this.jwt = jwt;
    }

    @GET
    @PermitAll
    //TODO: RATE-LIMIT OR USE ANY CONSTRAINT METHOD TO PREVENT A MASSIVE AMOUNT OF REQUESTS IN THIS UNAUTHENTICATED RESOURCE
    public ProductsPaginatedDTO getAllProducts(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                               @QueryParam("pageSize") @Positive @DefaultValue("10") @Max(30) int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _productService.getAllProducts(page, pageSize);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    //TODO: RATE-LIMIT OR USE ANY CONSTRAINT METHOD TO PREVENT A MASSIVE AMOUNT OF REQUESTS IN THIS UNAUTHENTICATED RESOURCE
    public ProductResponseDTO getProductById(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);

        //Quarkus return this with a 200 status code
        return _productService.getProductById(validatedId);
    }


    @POST
    @RolesAllowed("admin")
    public RestResponse<ProductResponseDTO> createProduct(@Valid ProductRequestDTO requestProduct,
                                                          @Context UriInfo uriInfo)
    {
        ProductResponseDTO createdProduct = _productService.createProduct(requestProduct, jwt.getSubject());

        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(createdProduct.id)
                              .build();

        return RestResponse.ResponseBuilder.<ProductResponseDTO>created(location)
                                           .entity(createdProduct)
                                           .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("admin")
    public ProductResponseDTO updateProduct(@PathParam("id") String id, @Valid ProductRequestDTO requestProduct) {

        ObjectId validatedId = validateId(id);

        //Quarkus return this with a 200 status code
        return _productService.updateProduct(validatedId, requestProduct, jwt.getSubject());
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed("admin")
    public RestResponse<Void> deleteProduct(@PathParam("id") String id)
    {
        ObjectId validatedId = validateId(id);
        _productService.deleteProduct(validatedId, jwt.getSubject());
        return RestResponse.noContent();
    }




    //HELPER
    private ObjectId validateId(String id)
    {
        if (!ObjectId.isValid(id))
            throw new InvalidIdException(id);

        return new ObjectId(id);
    }
}
