package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Turns application exceptions into the friendly error page.
 * Anything not handled here goes to Spring Boot's /error, which also renders error.html
 * (stack traces and exception messages are never included - see application.properties).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException ex) {
        return page(HttpStatus.NOT_FOUND, "Page not found", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView business(BusinessException ex) {
        return page(HttpStatus.BAD_REQUEST, "That didn't work", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView badRequest(Exception ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return page(HttpStatus.BAD_REQUEST, "Invalid request", "The link or form you used is not valid.");
    }

    private ModelAndView page(HttpStatus status, String title, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("title", title);
        mav.addObject("message", message);
        return mav;
    }
}
