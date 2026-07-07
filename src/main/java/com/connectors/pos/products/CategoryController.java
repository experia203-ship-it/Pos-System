package com.connectors.pos.products;

import com.connectors.pos.products.categorydtos.CategoryCreateDto;
import com.connectors.pos.products.categorydtos.CategoryResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequiredArgsConstructor
@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService catServo;



    @GetMapping("/new")

    public String openCreateNewCatPage(Model model){

        CategoryCreateDto dto = new CategoryCreateDto(null,null);

        model.addAttribute("categoryDto",dto);

        return "fragments/auth-messages :: pop-up2";
    }


    @PostMapping

    public String createNewCategory(@Valid @ModelAttribute("categoryDto") CategoryCreateDto createDto, BindingResult bindResult, Model model, HttpServletResponse response){


        if(bindResult.hasErrors()){

            return "fragments/auth-messages :: pop-up2";
        }

        catServo.createCategory(createDto);
        List<CategoryResponseDto> allCats=catServo.viewAllCategories();
        model.addAttribute("categories",allCats);
       return "fragments/auth-messages :: category-success" ;
    }

}
