package com.GitClone.Git.service;

import com.GitClone.Git.dag.CommitDAG;
import com.GitClone.Git.helper.GetBlobContent;
import com.GitClone.Git.helper.SerializingChunks;
import com.GitClone.Git.merge.MergeEngine;
import com.GitClone.Git.model.*;
import com.GitClone.Git.refs.RefManager;
import com.GitClone.Git.store.ObjectStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
@Service
public class MergeService {

    @Autowired private GitService gitService;
    @Autowired private RefManager refManager;
    @Autowired private CommitDAG commitDAG;
    @Autowired private MergeEngine mergeEngine;
    @Autowired private ObjectStore objectStore;
    @Autowired private GetBlobContent getBlobContent;
    @Autowired private SerializingChunks serializeCombinedChunks;
    public StringBuilder mergeConflictStore;

    public boolean canMerge(String targetBranch, boolean shouldMerge) throws DigestException, NoSuchAlgorithmException {
        String oursSha=gitService.getHeadSha();
        String theirSha=refManager.getBranchSha(targetBranch);
        String baseSha=commitDAG.findLCA(oursSha,theirSha);
        this.mergeConflictStore=new StringBuilder();
        /*
                if no base present there
                koi ancestor nai hai
                then it's a conflicts
        */
        if(baseSha==null)
        {
            return false;
        }

        //for our->
        Commit commitOur=(Commit) objectStore.getGitObject(oursSha);
        Tree treeOur= (Tree) objectStore.getGitObject(commitOur.getTreeSha());
        Map<String,String>ourMapFileToblob=treeOur.getEntries();

        //for their->
        Commit commitTheir=(Commit) objectStore.getGitObject(theirSha);
        Tree treeTheir = (Tree) objectStore.getGitObject(commitTheir.getTreeSha());
        Map<String,String> theirMapFileToblob = treeTheir.getEntries();

        //for base->
        Commit commitBase =(Commit) objectStore.getGitObject(baseSha);
        Tree treeBase = (Tree) objectStore.getGitObject(commitBase.getTreeSha());
        Map<String,String> baseMapFileToblob = treeBase.getEntries();

        Set<String>allFileNames=new HashSet<>(ourMapFileToblob.keySet());
        allFileNames.addAll(theirMapFileToblob.keySet());
        allFileNames.addAll(baseMapFileToblob.keySet());

        Map<String, String> mergedFileToBlobSha = new HashMap<>();

        boolean hasConflict=false;
        for(String fileName:allFileNames)
        {
            String baseStr   = getBlobContent.getContentFromBlob(baseMapFileToblob.get(fileName));
            String ourStr    = getBlobContent.getContentFromBlob(ourMapFileToblob.get(fileName));
            String theirStr  = getBlobContent.getContentFromBlob(theirMapFileToblob.get(fileName));

            MergeResult mergeResult=mergeEngine.threeWayMerge(baseStr,ourStr,theirStr);

            if(mergeResult.isHasConflicts())
            {
                hasConflict=true;
                System.out.println(mergeEngine.serializeConflictMarkers(
                        mergeEngine.getConflictMarkers(mergeResult)
                ));
                List<MergeChunk> conflicts = mergeEngine.getConflictMarkers(mergeResult);
                this.mergeConflictStore.append(mergeEngine.serializeConflictMarkers(conflicts));
            }
            else
            {
                String mergedContent = serializeCombinedChunks.serializeChunks(mergeResult.getChunks());
                Blob blob=new Blob(mergedContent.getBytes(StandardCharsets.UTF_8));
                String blobSha=objectStore.store(blob);

                mergedFileToBlobSha.put(fileName,blobSha);
            }
        }
        if(hasConflict)
        {
            return false;
            //throw new RuntimeException("Merge conflict are there: ");
            //commit nai krna hai because of conflicts
        }
        else
        {
            //no mergeconflicts so we can  merge
            //2 parents vala merge krna hai witha  3rd new commit created from 2prev committs as parents
            //refmanager use hoga

            //but iff they are working on prev verisons of the commit or commit not required therelike
            /*
                main:    A → B → C
                feature:      B

                or

                main:    A → B
                feature:      B → C
             */

            if (commitDAG.isAncestor(oursSha, theirSha)) {
                // fast-forward
                refManager.updateCurrentBranchAfterCommit(theirSha);
                return true;
            }
            if (commitDAG.isAncestor(theirSha, oursSha)) {
                return true; // already up-to-date
            }
            if(shouldMerge)
            {
                mergeCommits(oursSha,theirSha,mergedFileToBlobSha);
            }
        }
        return true;
    }
    public void mergeCommits(String oursSha,String theirSha,Map<String,String>mergedFileToBlobSha) throws DigestException, NoSuchAlgorithmException {
        Tree mergedTree=new Tree();
        mergedTree.setEntries(new HashMap<>(mergedFileToBlobSha));

        String mergedTreeSha=objectStore.store(mergedTree);

        List<String>mergedParents=List.of(oursSha,theirSha);

        Commit mergedCommit=new Commit(mergedTreeSha,mergedParents,"Merged Commit","AyushDubey");
        String commitSha=objectStore.store(mergedCommit);

        commitDAG.addCommit(commitSha,mergedParents);
        refManager.updateCurrentBranchAfterCommit(commitSha);
    }

}
