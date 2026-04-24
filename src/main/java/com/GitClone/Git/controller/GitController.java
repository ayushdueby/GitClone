package com.GitClone.Git.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/git")
public class GitController {

    @PostMapping("/init")
    public void gitInit()
    {

    }
    @PostMapping("/add")
    public void gitAdd(@RequestParam String path, @RequestBody String body)
    {

    }
    @GetMapping("/log")
    public void gitLog()
    {

    }

}
