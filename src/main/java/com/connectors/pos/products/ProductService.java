package com.connectors.pos.products;

import com.connectors.pos.exceptions.BusinessRuleException;
import com.connectors.pos.exceptions.CategoryNotFoundException;
import com.connectors.pos.exceptions.NullCategoryIdException;
import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

if(create.barcode()!=null && create.barcode().isEmpty()){
    product.setBarcode(null);
}

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

    if(dto.barcode()!=null && dto.barcode().isEmpty()){
        product.setBarcode(null);
    }
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
public ImportResult processFile(MultipartFile file) throws IOException {
    String content = decodeCsv(file.getBytes());
    List<List<String>> rows = parseCsv(content);
    List<Products> productsToSave = new ArrayList<>();
    Set<String> names = new HashSet<>();
    Set<String> partNumbers = new HashSet<>();
    Set<String> barcodes = new HashSet<>();
    int skipped = 0;

    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
        List<String> columns = rows.get(rowIndex);
        if (columns.stream().allMatch(value -> value.trim().isEmpty())) {
            continue;
        }
        if (rowIndex == 0 && isHeader(columns)) {
            continue;
        }
        if (columns.size() < 6) {
            skipped++;
            continue;
        }

        String partNumber = clean(columns.get(0));
        String name = clean(columns.get(1));
        String description = clean(columns.get(3));
        if (partNumber.isEmpty() || name.isEmpty()
                || description.isEmpty()
                || partNumber.length() > 50 || name.length() > 255 || description.length() > 255
                || productRepo.existsByName(name) || productRepo.existsByPartNumber(partNumber)
                || !names.add(name) || !partNumbers.add(partNumber)) {
            skipped++;
            continue;
        }

        try {
            BigDecimal sellingPrice = parseDecimal(columns.get(2));
            BigDecimal purchasePrice = parseDecimal(columns.get(4));
            if (sellingPrice == null || purchasePrice == null
                    || sellingPrice.signum() <= 0 || purchasePrice.signum() <= 0) {
                skipped++;
                continue;
            }

            Products product = new Products();
            product.setPartNumber(partNumber);
            product.setName(name);
            product.setSellingPrice(sellingPrice);
            product.setDescription(description);
            product.setPurchasePrice(purchasePrice);
            product.setStock(parseLong(columns.get(5), 0L));
            if (columns.size() > 6) {
                String barcode = clean(columns.get(6));
                if (!barcode.isEmpty() && barcode.length() <= 50
                        && barcodes.add(barcode) && productRepo.findByBarcode(barcode).isEmpty()) {
                    product.setBarcode(barcode);
                }
            }
            productsToSave.add(product);
        } catch (NumberFormatException | ArithmeticException exception) {
            skipped++;
        }
    }

    productRepo.saveAll(productsToSave);
    return new ImportResult(productsToSave.size(), skipped);
}

private static String clean(String value) {
    return value == null ? "" : value.trim();
}

private static BigDecimal parseDecimal(String value) {
    String cleaned = clean(value).replace(" ", "");
    return cleaned.isEmpty() ? null : new BigDecimal(cleaned.replace(',', '.'));
}

private static Long parseLong(String value, long defaultValue) {
    String cleaned = clean(value);
    return cleaned.isEmpty() ? defaultValue : Long.valueOf(cleaned);
}

private static boolean isHeader(List<String> columns) {
    if (columns.size() < 3) {
        return false;
    }
    String first = clean(columns.get(0));
    String third = clean(columns.get(2));
    return first.toLowerCase().contains("part")
            || clean(columns.get(1)).toLowerCase().contains("name")
            || (!isLong(first) && !isDecimal(third));
}

private static boolean isLong(String value) {
    try {
        Long.parseLong(value);
        return true;
    } catch (NumberFormatException exception) {
        return false;
    }
}

private static boolean isDecimal(String value) {
    try {
        new BigDecimal(value.replace(',', '.'));
        return true;
    } catch (NumberFormatException exception) {
        return false;
    }
}

private static String decodeCsv(byte[] bytes) {
    if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xef
            && (bytes[1] & 0xff) == 0xbb && (bytes[2] & 0xff) == 0xbf) {
        return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
    }
    if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xfe) {
        return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }
    if (bytes.length >= 2 && (bytes[0] & 0xff) == 0xfe && (bytes[1] & 0xff) == 0xff) {
        return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
    }
    try {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
    } catch (CharacterCodingException exception) {
        return new String(bytes, java.nio.charset.Charset.forName("windows-1256"));
    }
}

private static List<List<String>> parseCsv(String content) {
    List<List<String>> rows = new ArrayList<>();
    List<String> row = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    char delimiter = detectDelimiter(content);
    for (int i = 0; i < content.length(); i++) {
        char current = content.charAt(i);
        if (current == '"') {
            if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                field.append('"');
                i++;
            } else {
                quoted = !quoted;
            }
        } else if (current == delimiter && !quoted) {
            row.add(field.toString());
            field.setLength(0);
        } else if ((current == '\n' || current == '\r') && !quoted) {
            if (current == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') {
                i++;
            }
            row.add(field.toString());
            rows.add(row);
            row = new ArrayList<>();
            field.setLength(0);
        } else {
            field.append(current);
        }
    }
    if (field.length() > 0 || !row.isEmpty()) {
        row.add(field.toString());
        rows.add(row);
    }
    return rows;
}

private static char detectDelimiter(String content) {
    int firstLineEnd = content.indexOf('\n');
    String firstLine = firstLineEnd < 0 ? content : content.substring(0, firstLineEnd);
    if (firstLine.indexOf(';') >= 0 && firstLine.indexOf(',') < 0) {
        return ';';
    }
    if (firstLine.indexOf('\t') >= 0 && firstLine.indexOf(',') < 0) {
        return '\t';
    }
    return ',';
}

public record ImportResult(int imported, int skipped) {
}


@Transactional(readOnly = true)

    public ProductResponseDto findById(Long id){

    Products product = productRepo.findById(id)
            .orElseThrow(()->new ProductNotFoundException("product not found with that id "+ id));

    return productMapper.toResponse(product);
}

@Transactional(readOnly = true)
public ProductsReorderPoint viewAndCountReorderPoints(Pageable pageable){

    Page<Products> products = productRepo.extractAllProductsThatHitTheReorderPoint(pageable);
    Page<ProductResponseDto> response = products.map(productMapper::toResponse);
Long count = products.getTotalElements();

 ProductsReorderPoint point = new ProductsReorderPoint(response,count);
return point;
}



}
