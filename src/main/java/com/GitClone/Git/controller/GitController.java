package com.GitClone.Git.controller;

import com.GitClone.Git.service.GitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/git")
public class GitController {

    @Autowired public GitService gitService;

    @PostMapping("/init")
    public void gitInit()
    {
        gitService.gitInit();
    }
    @PostMapping("/add")
    public void gitAdd(@RequestParam String path, @RequestBody String body) throws DigestException, NoSuchAlgorithmException {
        gitService.gitAdd(path,body);
    }
    @PostMapping("/branch")
    public void gitCreateBranch(@RequestParam String name)
    {
        gitService.gitCreateBranch(name);
    }
    @PostMapping("/checkout")
    public void gitCheckoutBranch(@RequestParam String target)
    {
        gitService.gitCheckout(target);
    }
    @GetMapping("/log")
    public List<String> gitLog()
    {
        return gitService.gitLog();
    }
    @GetMapping("/branch")
    public List<String>gitListBranch()
    {
        return gitService.gitListBranch();
    }

}
