package com.connectors.pos.products;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.CategoryNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryCreateDto;
import com.connectors.pos.products.categorydtos.CategoryMapper;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepo;
    @Mock
    private ProductRepository productRepo;
    @Spy
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    @InjectMocks
    private CategoryService categoryService;

    @Test
    void createsCategoryAndReturnsMappedResponse() {
        when(categoryRepo.save(any(Categories.class))).thenAnswer(invocation -> {
            Categories category = invocation.getArgument(0);
            category.setId(4L);
            return category;
        });

        CategoryResponseDto result = categoryService.createCategory(new CategoryCreateDto("Filters", "Oil and air"));

        assertThat(result).isEqualTo(new CategoryResponseDto(4L, "Filters", "Oil and air"));
    }

    @Test
    void listsCategoriesAsResponseDtos() {
        when(categoryRepo.findAll()).thenReturn(List.of(
                Categories.builder().id(1L).name("Engine").description("Engine parts").build(),
                Categories.builder().id(2L).name("Electrical").description(null).build()));

        assertThat(categoryService.viewAllCategories()).containsExactly(
                new CategoryResponseDto(1L, "Engine", "Engine parts"),
                new CategoryResponseDto(2L, "Electrical", null));
    }

    @Test
    void findsCategoryAndRejectsUnknownId() {
        when(categoryRepo.findById(2L))
                .thenReturn(Optional.of(Categories.builder().id(2L).name("Brakes").description("Brake parts").build()));

        assertThat(categoryService.findCategoryById(2L))
                .isEqualTo(new CategoryResponseDto(2L, "Brakes", "Brake parts"));

        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(CategoryNotFoundException.class, () -> categoryService.findCategoryById(99L));
    }

    @Test
    void deletesOnlyWhenCategoryHasNoProducts() {
        Categories category = Categories.builder().id(6L).name("Clearance").build();
        when(categoryRepo.findById(6L)).thenReturn(Optional.of(category));
        when(productRepo.existsByCategoryId(6L)).thenReturn(false);

        categoryService.deleteCategory(6L);

        verify(categoryRepo).delete(category);

        when(productRepo.existsByCategoryId(6L)).thenReturn(true);
        assertThrows(BusinessRuleException.class, () -> categoryService.deleteCategory(6L));
        verify(categoryRepo, times(1)).delete(category);
    }

    @Test
    void rejectsDeletingMissingCategory() {
        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class, () -> categoryService.deleteCategory(99L));
        verifyNoInteractions(productRepo);
    }
}
