package com.benito.sales.service;

import com.benito.sales.dto.ProductResponseDTO;
import com.benito.sales.entity.Product;
import com.benito.sales.exception.ProductNotFoundException;
import com.benito.sales.repository.ProductRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Test
    void getProductById_happyPath_productFound()
    {
        //ARRANGE
        ProductRepository fakeRepository = mock(ProductRepository.class);
        ProductService service = new ProductService(fakeRepository);
        ObjectId id = new ObjectId();

        Product product = new Product();
        product.id = id;
        product.name = "Mouse";
        product.description = "Wireless mouse";
        product.price = new BigDecimal("25.00");
        product.status = true;

        when(fakeRepository.findById(id)).thenReturn(product);

        //ACT
        ProductResponseDTO result = service.getProductById(id);

        //ASSERT
        assertAll(() -> assertEquals(id.toString(), result.id),
                  () -> assertEquals("Mouse", result.name),
                  () -> assertEquals("Wireless mouse", result.description),
                  () -> assertEquals(new BigDecimal("25.00"), result.price));
    }

    @Test
    void getProductById_throwsException_productIsDeactivated()
    {
        //ARRANGE
        ProductRepository fakeRepository = mock(ProductRepository.class);
        ProductService service = new ProductService(fakeRepository);
        ObjectId id = new ObjectId();

        //THE PRODUCT EXISTS, BUT IT WAS DELETED (DEACTIVATED)
        Product product = new Product();
        product.id = id;
        product.name = "Mouse";
        product.status = false;

        when(fakeRepository.findById(id)).thenReturn(product);

        //ACT + ASSERT
        assertThrows(ProductNotFoundException.class, () -> service.getProductById(id));
    }

    @Test
    void deleteProduct_deactivatesTheProduct_productExists()
    {
        //ARRANGE
        ProductRepository fakeRepository = mock(ProductRepository.class);
        ProductService service = new ProductService(fakeRepository);
        ObjectId id = new ObjectId();

        Product product = new Product();
        product.id = id;
        product.name = "Mouse";
        product.status = true;

        when(fakeRepository.findById(id)).thenReturn(product);

        //ACT
        service.deleteProduct(id, "admin");

        //ASSERT
        //THE PRODUCT IS NOT REMOVED, IT IS MARKED AS INACTIVE
        assertAll(() -> assertFalse(product.status),
                  () -> assertEquals("admin", product.updatedBy),
                  () -> assertNotNull(product.updatedAt));

        verify(fakeRepository).update(product);
    }
}