package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.College;
import com.example.thesisgenerator.service.CollegeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
@RequiredArgsConstructor
public class CollegeController {

    private final CollegeService collegeService;

    @GetMapping
    public Result<List<College>> list() {
        return Result.ok(collegeService.findAll());
    }

    @GetMapping("/{id}")
    public Result<College> get(@PathVariable Long id) {
        return Result.ok(collegeService.findById(id));
    }

    @PostMapping
    public Result<College> create(@RequestBody College college) {
        return Result.ok(collegeService.create(college));
    }

    @PutMapping("/{id}")
    public Result<College> update(@PathVariable Long id, @RequestBody College college) {
        return Result.ok(collegeService.update(id, college));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        collegeService.delete(id);
        return Result.ok();
    }
}
