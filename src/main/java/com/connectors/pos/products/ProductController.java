package com.connectors.pos.products;

import com.connectors.pos.exceptions.ProductNotFoundException;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import com.connectors.pos.products.productdtos.CreateProductDto;
import com.connectors.pos.products.productdtos.ProductResponseDto;
import com.connectors.pos.products.productdtos.ProductUpdateDto;
import com.connectors.pos.products.productdtos.ProductsReorderPoint;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productServo;
    private final CategoryService catServo;
    private final CategoryRepository catRepo;


    @GetMapping
    public String viewProductsPage(Model model,
    @PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable ,
                                   @RequestHeader(value = "Hx-Request",required = false) String hxRequest){


        System.out.println("prods");
        Page<ProductResponseDto> result= productServo.viewAllProducts(pageable);

        List<Long> allCatIds = result.stream().map(ProductResponseDto::categoryId)
                        .toList();

        List<Categories> allCats = catRepo.findAllById(allCatIds);

        Map<Long,String> catsWithIds = allCats.stream().collect(Collectors.toMap(Categories::getId, Categories::getName));

model.addAttribute("catMap",catsWithIds);

        model.addAttribute("allProducts",result);

       if("true".equals(hxRequest)){

          return"products";
        }


        return "products";
    }

    @GetMapping("/search")
    public String searchByKeyword(@RequestParam String keyword ,
    @PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable , Model model,
                        @RequestHeader(value ="Hx-Request",required = false)  String hxRequest       ){

Page<ProductResponseDto> response = productServo.filterByKeyword(keyword,pageable);
model.addAttribute("allProducts",response);

if(!("true".equals(hxRequest))){return "products";}
return "products :: table-wrapper";
    }


    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id){

        productServo.deleteProductById(id);

        return "fragments/auth-messages :: deletion-success";

    }



    @GetMapping("/create")

    public String openCreateProductDialog(Model model){

        CreateProductDto create = new CreateProductDto(null,
                null,null,null,null,null,
                null,null,null,null
                );

        List<CategoryResponseDto> listOfCategories = catServo.viewAllCategories();

     model.addAttribute("createDto",create);
model.addAttribute("categories",listOfCategories);
     return "fragments/auth-messages :: pop-up";


    }



@PostMapping

    public String createNewProduct(@Valid @ModelAttribute("createDto") CreateProductDto incomingDto, BindingResult bindResult, Model model,
                                   @PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable,
                                   HttpServletResponse response){

        if(bindResult.hasErrors()){

            String errorMessages=bindResult.getFieldErrors().stream()
                    .map(error->error.getField() + "; " + error.getDefaultMessage())
                    .collect(Collectors.joining("|"));

            model.addAttribute("errorMessage",errorMessages);

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return "fragments/auth-messages :: exceptions-response";
        }

productServo.createProduct(incomingDto);

Page<ProductResponseDto> results=productServo.viewAllProducts(pageable);

model.addAttribute("allProducts",results);
response.setHeader("HX-Trigger","close-modal");

return "products :: product-table-body";


}
@GetMapping("/{id}")
    public String viewUpdateProductDialog(@PathVariable Long id ,Model model){


        ProductResponseDto product = productServo.getProductById(id);

      ProductUpdateDto update= new ProductUpdateDto(product.name(),product.partNumber(),
              product.description(),product.sellingPrice(),product.purchasePrice(),
              product.stock(),product.categoryId(),product.barcode(),product.reorderPoint(),product.customFields());

    List<CategoryResponseDto> allCats=catServo.viewAllCategories();
    model.addAttribute("categories",allCats);
      model.addAttribute("updateProduct",update);
      model.addAttribute("prodId",product.id());
if(product.categoryId()!=null) {
    CategoryResponseDto category = catServo.findCategoryById(product.categoryId());
    String catName=category.name();
    model.addAttribute("catName",catName);
    model.addAttribute("catId", product.categoryId());

}


      return "fragments/update-fragment :: update-popup";

    }
    @PatchMapping("/{id}")

    public String updateProduct(@PathVariable Long id,
                                @Valid@ModelAttribute("updateProduct") ProductUpdateDto dto,
                                BindingResult bindResult,
                                HttpServletResponse response,Model model,@PageableDefault(page = 0,size = 10,sort = "id",direction = Sort.Direction.DESC) Pageable pageable){

        if(bindResult.hasErrors()){

            response.setHeader("HX-Retarget","#form-update");
            response.setHeader("HX-Reswap","outerHTML");

            return "fragments/update-fragment :: update-popup";
        }

        productServo.updateProduct(id,dto);
       Page<ProductResponseDto> products=productServo.viewAllProducts(pageable);
       model.addAttribute("allProducts",products);

        List<Long> allCatIds = products.stream().map(ProductResponseDto::categoryId)
                .toList();

        List<Categories> allCats = catRepo.findAllById(allCatIds);

        Map<Long,String> catsWithIds = allCats.stream().collect(Collectors.toMap(Categories::getId, Categories::getName));

        model.addAttribute("catMap",catsWithIds);





       response.setHeader("Hx-Trigger","close-modal");
        return "products :: product-table-body";

    }


    @GetMapping("/search-pos")
    public String searchForPos(@PageableDefault(page = 0,size = 10,sort = "name",direction = Sort.Direction.ASC) Pageable pageable,
                               Model model , @RequestParam(name = "keyword" ,defaultValue = "") String keyword ,@RequestParam(name="mode",defaultValue = "sales") String mode){


        Page<ProductResponseDto> results;
      if(keyword ==null|| keyword.trim().isEmpty()){

          results = Page.empty(pageable);
      }

else {
          results = productServo.filterByKeyword(keyword, pageable);
      }
        model.addAttribute("results",results);

       model.addAttribute("mode",mode);

        return "fragments/search-results :: search-results-fragment";


    }


  @PostMapping("/import")

  public String importCsvFiles(@RequestParam(name="file") MultipartFile file, RedirectAttributes redirect){


        if(file.isEmpty()){

            redirect.addFlashAttribute("error","please select a file for the import");

            return "redirect:/products";
        }

        try{
     ProductService.ImportResult result = productServo.processFile(file);

redirect.addFlashAttribute("success",result.imported() + " products imported successfully"
        + (result.skipped() == 0 ? "" : "; " + result.skipped() + " rows skipped"));


        } catch (Exception e) {
            redirect.addFlashAttribute("error","error happened while executing  " +e.getMessage());
            System.out.println("reached exception" + e.getMessage());

        }
      return "redirect:/products";

  }

@GetMapping("/generate-barcode")
    @ResponseBody

    public String generateBarcode(){

        String prefix="200";

        String timeStamp = String.valueOf(System.currentTimeMillis());
        timeStamp = timeStamp.substring(timeStamp.length()-8);
        int randomPart = new Random().nextInt(90)+10;
String generatedBarcode= prefix+timeStamp+randomPart;

    String html = """
        <input id='barcode' name='barcode' class='form-control barcode-input' type='text' value='%s' readonly />
        """;

return String.format(html,generatedBarcode);

}
    @GetMapping("/print-label/{id}")
    public String printBarcodeLabel(@PathVariable Long id, Model model) {
        ProductResponseDto prod = productServo.findById(id);
        model.addAttribute("product", prod);
        return "barcode-label";
    }

    @GetMapping("/reorder-point")
    public String viewReorderPointsProducts(@PageableDefault(page = 0,size = 10) Pageable pageable ,Model model){

        ProductsReorderPoint point = productServo.viewAndCountReorderPoints(pageable);

        Page<ProductResponseDto> results = point.products();
        Long count = point.count();
List<Long> allCatIds = results.stream().map(ProductResponseDto::categoryId).filter(Objects::nonNull).toList();
List<Categories> getAllCatsWithIds = catRepo.findAllById(allCatIds);

Map<Long,String> catMap=getAllCatsWithIds.stream().collect(Collectors.toMap(Categories::getId,Categories::getName ));
       model.addAttribute("catMap",catMap);
        model.addAttribute("products",results);
        model.addAttribute("count",count);

        return "products-Reorder-Point";
    }
    @GetMapping("/custom-field-row")
    public String getCustomRow(){


        return"fragments/custom-field-row";
    }
}
