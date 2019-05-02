package com.yang.demo.mvc.action;

import com.yang.demo.service.IDemoService;
import com.yang.mvcframework.annotation.ZYAutowried;
import com.yang.mvcframework.annotation.ZYCountroller;
import com.yang.mvcframework.annotation.ZYRequestMapping;
import com.yang.mvcframework.annotation.ZYRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ZYCountroller
@ZYRequestMapping("/demo")
public class DemoAction {
    @ZYAutowried
    private IDemoService demoService;
    @ZYRequestMapping("/query.*")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @ZYRequestParam("name") String name){
        String result = "My name is " + name;
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZYRequestMapping("/add")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @ZYRequestParam("a") Integer a, @ZYRequestParam("b") Integer b){
        try {
            resp.getWriter().write(a + "+" + b + "=" + (a + b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZYRequestMapping("/sub")
    public void add(HttpServletRequest req, HttpServletResponse resp,
                    @ZYRequestParam("a") Double a, @ZYRequestParam("b") Double b){
        try {
            resp.getWriter().write(a + "-" + b + "=" + (a - b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ZYRequestMapping("/remove")
    public String  remove(@ZYRequestParam("id") Integer id){
        return "" + id;
    }
}
