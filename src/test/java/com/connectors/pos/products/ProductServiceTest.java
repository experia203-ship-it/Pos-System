package com.connectors.pos.products;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.CategoryNotFoundException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.CreateProductDto;
import com.connectors.pos.products.productdtos.ProductMapper;
import com.connectors.pos.products.productdtos.ProductResponseDto;
import com.connectors.pos.products.productdtos.ProductUpdateDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepo;
    @Spy
    private ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
    @Mock
    private CategoryService categoryService;
    @Mock
    private CategoryRepository categoryRepo;
    @InjectMocks
    private ProductService productService;

    @Test
    void createsProductWithResolvedCategoryAndMappedResponse() {
        when(productRepo.existsByName("Oil filter")).thenReturn(false);
        when(categoryService.findCategoryById(3L)).thenReturn(new CategoryResponseDto(3L, "Filters", "Vehicle filters"));
        when(productRepo.save(any(Products.class))).thenAnswer(invocation -> {
            Products product = invocation.getArgument(0);
            product.setId(11L);
            return product;
        });

        ProductResponseDto result = productService.createProduct(new CreateProductDto("Oil filter", "OF-1",
                "Replacement filter", new BigDecimal("12.50"), new BigDecimal("7.25"), 20L, 3L,
                "BC-11", 4L, Map.of("brand", "Acme")));

        assertThat(result.id()).isEqualTo(11L);
        assertThat(result.name()).isEqualTo("Oil filter");
        assertThat(result.categoryId()).isEqualTo(3L);
        assertThat(result.barcode()).isEqualTo("BC-11");
        assertThat(result.customFields()).containsEntry("brand", "Acme");
        verify(categoryService).findCategoryById(3L);
    }

    @Test
    void rejectsDuplicateProductNameBeforeLookingUpCategory() {
        when(productRepo.existsByName("Oil filter")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> productService.createProduct(
                new CreateProductDto("Oil filter", "OF-2", "Replacement filter", BigDecimal.TEN,
                        BigDecimal.ONE, 1L, 3L, null, 0L, Map.of())));

        verifyNoInteractions(categoryService);
        verify(productRepo, never()).save(any());
    }

    @Test
    void updatesProductFieldsAndCategory() {
        Categories oldCategory = Categories.builder().id(3L).name("Old").build();
        Categories newCategory = Categories.builder().id(4L).name("New").build();
        Products product = Products.builder().id(11L).name("Old name").partNumber("P-1")
                .description("Old description").sellingPrice(BigDecimal.ONE).purchasePrice(BigDecimal.ONE)
                .stock(2L).category(oldCategory).build();
        when(productRepo.findById(11L)).thenReturn(Optional.of(product));
        when(categoryRepo.findById(4L)).thenReturn(Optional.of(newCategory));

        ProductResponseDto updated = productService.updateProduct(11L, new ProductUpdateDto(
                "New name", null, "Updated description", new BigDecimal("15.00"), null, 8L,
                4L, "BC-12", 3L, Map.of("grade", "A")));

        assertThat(updated.name()).isEqualTo("New name");
        assertThat(updated.description()).isEqualTo("Updated description");
        assertThat(updated.stock()).isEqualTo(8L);
        assertThat(updated.categoryId()).isEqualTo(4L);
        assertThat(updated.barcode()).isEqualTo("BC-12");
        assertThat(updated.customFields()).containsEntry("grade", "A");
    }

    @Test
    void rejectsUpdateWhenProductOrCategoryDoesNotExist() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L,
                new ProductUpdateDto(null, null, null, null, null, null, 1L, null, null, Map.of())));

        when(productRepo.findById(11L)).thenReturn(Optional.of(Products.builder().id(11L).build()));
        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFoundException.class, () -> productService.updateProduct(11L,
                new ProductUpdateDto(null, null, null, null, null, null, 99L, null, null, Map.of())));
    }

    @Test
    void deletesActiveProductAndRejectsMissingOrAlreadyDeletedProduct() {
        Products product = Products.builder().id(11L).build();
        when(productRepo.checkIsActiveStatusById(11L)).thenReturn(true);
        when(productRepo.findById(11L)).thenReturn(Optional.of(product));

        productService.deleteProductById(11L);

        verify(productRepo).delete(product);

        when(productRepo.checkIsActiveStatusById(12L)).thenReturn(false);
        assertThrows(BusinessRuleException.class, () -> productService.deleteProductById(12L));
        when(productRepo.checkIsActiveStatusById(99L)).thenReturn(null);
        assertThrows(ProductNotFoundException.class, () -> productService.deleteProductById(99L));
    }

    @Test
    void mapsPagedProductResults() {
        Products product = Products.builder().id(11L).name("Oil filter").partNumber("OF-1")
                .description("Replacement filter").sellingPrice(BigDecimal.TEN).purchasePrice(BigDecimal.ONE)
                .stock(20L).category(Categories.builder().id(3L).name("Filters").build()).build();
        PageRequest pageable = PageRequest.of(0, 5);
        when(productRepo.findByCategoryId(3L, pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 1));

        var result = productService.filterProductsByCategories(3L, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(ProductResponseDto::name).containsExactly("Oil filter");
        verify(productRepo).findByCategoryId(3L, pageable);
    }

    @Test
    void importsValidCsvRowsAndCountsSkippedRows() throws Exception {
        when(productRepo.existsByName(any())).thenReturn(false);
        when(productRepo.existsByPartNumber(any())).thenReturn(false);
        when(productRepo.findByBarcode("BC-1")).thenReturn(Optional.empty());
        MockMultipartFile csv = new MockMultipartFile("file", "products.csv", "text/csv",
                ("part_number,name,selling_price,description,purchase_price,stock,barcode\n"
                        + "P-1,Oil filter,12.50,Replacement filter,7.25,20,BC-1\n"
                        + "P-2,Invalid,not-a-price,Invalid price,3.00,1,\n"
                        + "P-3,Oil filter,10.00,Duplicate name,5.00,2,\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        ProductService.ImportResult result = productService.processFile(csv);

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        verify(productRepo).saveAll(argThat(products -> {
            java.util.Iterator<Products> iterator = products.iterator();
            if (!iterator.hasNext()) {
                return false;
            }
            Products imported = iterator.next();
            if (iterator.hasNext()) {
                return false;
            }
            return imported.getName().equals("Oil filter")
                    && imported.getPartNumber().equals("P-1")
                    && imported.getSellingPrice().compareTo(new BigDecimal("12.50")) == 0
                    && imported.getBarcode().equals("BC-1");
        }));
    }
}
