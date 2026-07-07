package com.connectors.pos.exceptions;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)

    public ModelAndView handleDublicateDatabaseEntry(DataIntegrityViolationException ex ){

        ModelAndView mov =new ModelAndView("fragments/auth-messages :: exceptions-response");

        mov.addObject("errorMessage","duplicate name or partNumber");

        mov.setStatus(HttpStatus.BAD_REQUEST);

        return mov;


    }

    @ExceptionHandler(BusinessRuleException.class)
public ModelAndView  handleBusinessRuleExceptions(BusinessRuleException ex){

        ModelAndView mov = new ModelAndView("fragments/auth-messages :: exceptions-response");

        mov.addObject("errorMessage", ex.getMessage());

        mov.setStatus(HttpStatus.BAD_REQUEST);

        return mov;
    }

@ExceptionHandler(NullCategoryIdException.class)

    public ModelAndView handleNullCategoryIdException(NullCategoryIdException ex){

        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
  mav.setStatus(HttpStatus.NOT_ACCEPTABLE);
  mav.addObject("errorMessage", ex.getMessage());

  return mav;

    }

    @ExceptionHandler(CustomerMustBeProvidedException.class)

     public ModelAndView handeCustomerMustBeProvidedException(CustomerMustBeProvidedException ex,HttpServletResponse response){

        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#customer-error");
        response.setHeader("HX-Reswap","innerHTML");
        mav.addObject("errorMessage",ex.getMessage());
        return mav;
    }

    @ExceptionHandler(YouMustProvideAtLeastOneItem.class)

    public ModelAndView handleEmptyItemsList(YouMustProvideAtLeastOneItem ex , HttpServletResponse response){

        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage",ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");

        return mav;
    }


    @ExceptionHandler(InsuffecientStockException.class)

    public ModelAndView hanldeInsufficentStocException(InsuffecientStockException ex , HttpServletResponse response){

        ModelAndView mav  = new ModelAndView("fragments/auth-messages :: exceptions-response");

        mav.addObject("errorMessage",ex.getMessage());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");


        return mav;


    }
}
