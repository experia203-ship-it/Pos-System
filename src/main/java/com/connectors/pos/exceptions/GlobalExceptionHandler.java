package com.connectors.pos.exceptions;

import com.connectors.pos.i18n.Messages;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            CustomerNotFoundException.class,
            OrderNotFoundException.class,
            CategoryNotFoundException.class,
            EntityNotFoundException.class
    })
    public ModelAndView handleEntityNotFound(RuntimeException ex, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage", ex.getMessage());
        mav.setStatus("true".equalsIgnoreCase(request.getHeader("HX-Request"))
                ? HttpStatus.OK : HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)

    public ModelAndView handleDublicateDatabaseEntry(DataIntegrityViolationException ex ){

        ModelAndView mov =new ModelAndView("fragments/auth-messages :: exceptions-response");

        mov.addObject("errorMessage", Messages.get("error.duplicateEntry"));

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

    @ExceptionHandler(ShiftRequiredException.class)
    public ModelAndView handleShiftRequired(ShiftRequiredException ex, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#list-error");
        response.setHeader("HX-Reswap", "innerHTML");
        return mav;
    }

    @ExceptionHandler(ShiftOperationException.class)
    public ModelAndView handleShiftOperation(ShiftOperationException ex, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#shift-error");
        response.setHeader("HX-Reswap", "innerHTML");
        return mav;
    }

    @ExceptionHandler(PurchaseOrderException.class)
    public ModelAndView handlePurchaseOrder(PurchaseOrderException ex, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#list-error");
        response.setHeader("HX-Reswap", "innerHTML");
        return mav;
    }

    @ExceptionHandler(SaleValidationException.class)
    public ModelAndView handleSaleValidation(SaleValidationException ex, HttpServletResponse response) {
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage", ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget", "#list-error");
        response.setHeader("HX-Reswap", "innerHTML");
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

    @ExceptionHandler(ProductNotFoundException.class)

    public ModelAndView handleProductNotFoundException(ProductNotFoundException ex,HttpServletResponse response){
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage",ex.getMessage());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");

        return mav;
    }

    @ExceptionHandler(UserManagementException.class)

    public ModelAndView handleUserManagementException(UserManagementException ex, HttpServletResponse response){
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage",ex.getMessage());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");

        return mav;
    }

    @ExceptionHandler(UserNotFoundException.class)

    public ModelAndView handleUserNotFoundException(UserNotFoundException ex, HttpServletResponse response){
        ModelAndView mav = new ModelAndView("fragments/auth-messages :: exceptions-response");
        mav.addObject("errorMessage",ex.getMessage());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error");
        response.setHeader("HX-Reswap","innerHTML");

        return mav;
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)

    public ModelAndView handleValidationExceptions(MethodArgumentNotValidException ex,HttpServletResponse response){
        ModelAndView mov = new ModelAndView("fragments/auth-messages :: exceptions-response");

        mov.addObject("errorMessage",ex.getMessage());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("HX-Retarget","#list-error2");
        response.setHeader("HX-Reswap","innerHTML");

       return mov;
    }









}
