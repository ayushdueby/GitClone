package com.GitClone.Git.controller;

import com.GitClone.Git.model.DiffLine;
import com.GitClone.Git.service.DiffService;
import com.GitClone.Git.service.GitService;
import com.GitClone.Git.service.MergeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/git")
public class GitController {

    @Autowired public GitService gitService;
    @Autowired public DiffService diffService;
    @Autowired public MergeService mergeService;

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
    @PostMapping("/merge")
    public void gitMerge(@RequestParam String branch) throws DigestException, NoSuchAlgorithmException {
        mergeService.canMerge(branch,true);
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
    @GetMapping("/diff")
    public List<DiffLine> getDiff(@RequestParam String sha1,@RequestParam String sha2,@RequestParam String fileName)
    {
        return diffService.diffCommits(sha1,sha2,fileName);
    }
    @GetMapping("/diff/working")
    public List<DiffLine> getDiff(@RequestParam String file,@RequestParam String newContent)
    {
        return diffService.diffWorkingVsHead(file,newContent);
    }
    @GetMapping("/merge/preview")
    public String gitCheckMergeConflicts(@RequestParam String branch) throws DigestException, NoSuchAlgorithmException {
        boolean canMerge = mergeService.canMerge(branch, false);
        if(!canMerge)
            return mergeService.mergeConflictStore.toString();
        else
            return "No conflicts";
    }

    @DeleteMapping("/git/branch")
    public ResponseEntity<?> deleteBranch(@RequestParam String name) {
        return ResponseEntity.ok(gitService.deleteBranch(name));
    }
    @GetMapping("/git/log/graph")
    public ResponseEntity<?> getGraph() {
        return ResponseEntity.ok(gitService.getGraphLog());
    }
}
