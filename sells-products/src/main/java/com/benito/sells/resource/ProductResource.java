package com.benito.sells.resource;

import com.benito.sells.dto.ProductResponseDTO;
import com.benito.sells.dto.ProductsPaginatedDTO;
import com.benito.sells.service.ProductService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;

@Path("api/v1/product")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    //DEPENDECY INJECTION -> Service
    private final ProductService _productService;
    public ProductResource(ProductService productService) { this._productService = productService; }

    @GET
    public ProductsPaginatedDTO getAllProducts(@QueryParam("page") @PositiveOrZero @DefaultValue("0") int page,
                                               @QueryParam("pageSize") @Positive @DefaultValue("10") int pageSize)
    {
        //Quarkus return this with a 200 status code
        return _productService.getAllProducts(page, pageSize);
    }

    @GET
    @Path("/{id}")
    public ProductResponseDTO getProductById(String id)
    {
        if (!ObjectId.isValid(id))
            throw new BadRequestException();

        return _productService.getProductById(new ObjectId(id));
    }
}
