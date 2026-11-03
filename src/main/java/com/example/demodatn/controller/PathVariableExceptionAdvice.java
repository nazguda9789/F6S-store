package com.example.demodatn.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import java.io.IOException;

@ControllerAdvice
public class PathVariableExceptionAdvice {
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ModelAndView invalidIdentifier(MethodArgumentTypeMismatchException error,
            HttpServletResponse response) throws IOException {
        if (error.getParameter().hasParameterAnnotation(PathVariable.class)
                && (error.getRequiredType() == Integer.class || error.getRequiredType() == Long.class)) {
            return new ModelAndView("loi/404", HttpStatus.NOT_FOUND);
        }
        response.sendError(HttpStatus.BAD_REQUEST.value());
        return null;
    }
}
