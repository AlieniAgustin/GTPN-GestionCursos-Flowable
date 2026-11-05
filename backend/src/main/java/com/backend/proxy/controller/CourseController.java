package com.backend.proxy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.backend.proxy.types.CourseStatus;

@RequestMapping("/api/flowable/courses")
public class CourseController {

    @PutMapping("/{processId}")
    public void updateStatus(@PathVariable String processId, @RequestBody CourseStatus status){
    
    }
    
}
