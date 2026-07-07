package com.connectors.pos.products;


import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.CategoryNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryCreateDto;
import com.connectors.pos.products.categorydtos.CategoryMapper;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepo;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepo;

    @Transactional
    public CategoryResponseDto createCategory(CategoryCreateDto create){

        Categories category = categoryMapper.toEntity(create);

        Categories saved = categoryRepo.save(category);

        return categoryMapper.toResponse(saved);

    }

    @Transactional(readOnly = true)

    public CategoryResponseDto findCategoryById(Long id){

        Categories category = categoryRepo.findById(id)
                .orElseThrow(()->new CategoryNotFoundException("category wasn't found with this id: "+id));

        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)

    public List<CategoryResponseDto> viewAllCategories(){

        List<Categories> cats = categoryRepo.findAll();

        return categoryMapper.toDtoResponse(cats);

    }

    @Transactional
    public void deleteCategory(Long id){

        Categories category = categoryRepo.findById(id)
                .orElseThrow(()->new CategoryNotFoundException("category is either deleted or doesn't exist"));

     if(productRepo.existsByCategoryId(id)){

         throw new BusinessRuleException("can't delete a category that still contain products");
     }

     categoryRepo.delete(category);
    }

}
