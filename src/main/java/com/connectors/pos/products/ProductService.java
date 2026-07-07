package com.connectors.pos.products;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.CategoryNotFoundException;
import com.connectors.pos.exceptions.NullCategoryIdException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.CreateProductDto;
import com.connectors.pos.products.productdtos.ProductMapper;
import com.connectors.pos.products.productdtos.ProductResponseDto;
import com.connectors.pos.products.productdtos.ProductUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Service
public class ProductService {


    private final ProductRepository productRepo;
    private final ProductMapper productMapper;
    private final CategoryService categoryService;
    private final CategoryRepository catRepo;
    /**
     *  create a new product
     *  delete an existing one
     *  load all existing products in pages
     *  update a product
     *  check product stock
     */
@Transactional
public ProductResponseDto createProduct (CreateProductDto create){



    if(productRepo.existsByName(create.name())){
        throw new BusinessRuleException("an product already exists with that name , please choose another");
    }

    Products product = productMapper.toEntity(create);



CategoryResponseDto found = categoryService.findCategoryById(create.categoryId());
Categories category = Categories.builder().
    name(found.name()).description(found.description())
        .id(found.id()).build();

product.setCategory(category);

Products saved = productRepo.save(product);

return productMapper.toResponse(saved);
}

@Transactional
public void deleteProductById(Long id){

    Boolean isActive = productRepo.checkIsActiveStatusById(id);

    if(isActive == null){
throw new ProductNotFoundException("product not found with this id: "+id);
    }

if(!isActive){

throw new BusinessRuleException("this product was already deleted");
}

Products product = productRepo.findById(id)
        .orElseThrow(()->new ProductNotFoundException("product not found with this id: "+id));
productRepo.delete(product);
}


@Transactional
    public ProductResponseDto updateProduct(Long productId,ProductUpdateDto dto){

Products product = productRepo.findById(productId)
        .orElseThrow(()->new ProductNotFoundException("product not found"));

      Long catId= dto.categoryId();

      Categories cat = catRepo.findById(catId)
                      .orElseThrow(()->new CategoryNotFoundException("this category doesn't exist with that id "+ catId));

 productMapper.updateEntityFromDto(dto,product);
product.setCategory(cat);

return productMapper.toResponse(product);
}


@Transactional(readOnly = true)
public Page<ProductResponseDto> viewAllProducts(Pageable pageable){

    Page<Products> products = productRepo.findAll(pageable);

    return products.map(productMapper::toResponse);


}

@Transactional(readOnly = true)

    public Page<ProductResponseDto> filterProductsByCategories(Long catId,Pageable pageable){

    Page<Products> products = productRepo.findByCategoryId(catId,pageable);

    return products.map(productMapper::toResponse);
}

@Transactional(readOnly = true)

    public Page<ProductResponseDto> filterByKeyword(String keyword ,Pageable pageable){

    Page<Products>  results = productRepo.searchByKeywordPartialMatch(keyword,pageable);

    return results.map(productMapper::toResponse);
}
@Transactional(readOnly = true)

    public ProductResponseDto getProductById(Long id){

    Products product = productRepo.findById(id)
            .orElseThrow(()->new ProductNotFoundException("product doesn't exist"));

    return productMapper.toResponse(product);

}

@Transactional


public int processFile(MultipartFile file) throws IOException {
    List<Products> produtcsToSaved=new ArrayList<>();

    try(BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))){
    String line;
boolean isFirstRow=true;

while((line=reader.readLine())!=null){

    if(isFirstRow){
        isFirstRow=false;
        continue;
    }
    String[] columns = line.split(",");

    if(columns.length>=6){
Products product = new Products();

product.setPartNumber(columns[0].trim());
product.setName(columns[1].trim());
product.setSellingPrice( new BigDecimal(columns[2].trim()));
product.setDescription(columns[3].trim());
product.setPurchasePrice( new BigDecimal(columns[4].trim()));
product.setStock(Long.parseLong(columns[5].trim()));

produtcsToSaved.add(product);
    }


}

productRepo.saveAll(produtcsToSaved);

return produtcsToSaved.size();
    }

}




}
