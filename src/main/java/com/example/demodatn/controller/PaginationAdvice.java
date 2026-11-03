package com.example.demodatn.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@ControllerAdvice
public class PaginationAdvice implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {
    @Override public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(new org.springframework.web.servlet.HandlerInterceptor() {
            @Override public void postHandle(HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response,Object handler,org.springframework.web.servlet.ModelAndView model) {
                if(model==null || !"POST".equals(request.getMethod()) || !request.getRequestURI().contains("/delete/"))return;
                String view=model.getViewName(),referer=request.getHeader("Referer");
                if(view==null || !view.startsWith("redirect:/admin/") || referer==null)return;
                try {var previous=java.net.URI.create(referer);String destination=view.substring("redirect:".length());
                    if(previous.getPath().equals(destination) && previous.getRawQuery()!=null)model.setViewName(view+"?"+previous.getRawQuery());
                }catch(IllegalArgumentException ignored){ /* Referrer không hợp lệ: dùng redirect mặc định. */ }
            }
        });
    }
    @ModelAttribute("pagingLinks")
    public Links links(HttpServletRequest request) { return new Links(request); }
    public static class Links {
        private final String path;
        private final Map<String,String[]> parameters;
        Links(HttpServletRequest request) {path=request.getRequestURI();parameters=new LinkedHashMap<>(request.getParameterMap());}
        public String url(String parameter,int page) {
            var pairs=new ArrayList<String>();
            parameters.forEach((key,values)->{if(!key.equals(parameter))for(String value:values)pairs.add(encode(key)+"="+encode(value));});
            pairs.add(encode(parameter)+"="+page);return path+"?"+String.join("&",pairs);
        }
        private String encode(String value){return java.net.URLEncoder.encode(value,java.nio.charset.StandardCharsets.UTF_8);}
        public List<Integer> numbers(int current,int total) {
            var result=new ArrayList<Integer>();int previous=-1;
            var candidates=new TreeSet<Integer>();candidates.add(0);candidates.add(total-1);
            for(int n=Math.max(0,current-1);n<=Math.min(total-1,current+1);n++)candidates.add(n);
            for(int i:candidates) {
                if(previous>=0 && i-previous>1) result.add(-1);
                result.add(i);previous=i;
            }
            return result;
        }
    }
}
