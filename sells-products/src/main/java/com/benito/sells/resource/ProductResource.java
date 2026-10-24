package com.benito.sells.resource;

import com.benito.sells.dto.ProductRequestDTO;
import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.service.ProductService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("api/v1/product")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    //DEPENDECY INJECTION -> Service & jwt
    private final ProductService _productService;
    private final JsonWebToken jwt;
    public ProductResource(ProductService productService, JsonWebToken jwt)
    {
        this._productService = productService;
        this.jwt = jwt;
    }

    @GET
    @PermitAll
    public ProductsPaginatedDTO getAllProducts(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                               @QueryParam("pageSize") @Positive @DefaultValue("10") int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _productService.getAllProducts(page, pageSize);
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public ProductResponseDTO getProductById(String id)
    {
        if (!ObjectId.isValid(id))
            throw new BadRequestException();

        return _productService.getProductById(new ObjectId(id));
    }

    @POST
    @RolesAllowed("admin")
    public ProductResponseDTO createProduct(@Valid ProductRequestDTO requestProduct)
    {
        return _productService.createProduct(requestProduct, jwt.getSubject());
    }


}
