package com.yang.demo.service.impl;

import com.yang.demo.service.IDemoService;

public class DempoService implements IDemoService {
    @Override
    public String get(String name) {
        return "My name is " + name;
    }
}
