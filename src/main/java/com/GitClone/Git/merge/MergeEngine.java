package com.GitClone.Git.merge;

import com.GitClone.Git.diff.DiffEngine;
import com.GitClone.Git.gitEnum.MergeEnum;
import com.GitClone.Git.model.DiffLine;
import com.GitClone.Git.model.MergeChunk;
import com.GitClone.Git.model.MergeResult;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class MergeEngine {

    private final DiffEngine diffEngine;
    private MergeResult mergeResult;

    public MergeEngine(DiffEngine diffEngine)
    {
        this.diffEngine=diffEngine;
    }
    public MergeResult threeWayMerge(String base, String ours, String theirs) {

        String[] baseLines = base.split("\n");
        String[] ourLines = ours.split("\n");
        String[] theirLines = theirs.split("\n");

        int maxLines = Math.max(baseLines.length,
                Math.max(ourLines.length, theirLines.length));

        List<MergeChunk> chunks = new ArrayList<>();
        boolean hasConflicts = false;

        for (int i = 0; i < maxLines; i++) {

            String baseLine = i < baseLines.length ? baseLines[i] : "";
            String ourLine = i < ourLines.length ? ourLines[i] : "";
            String theirLine = i < theirLines.length ? theirLines[i] : "";

            if (ourLine.equals(theirLine)) {

                // both same → normal
                chunks.add(new MergeChunk(
                        MergeEnum.NORMAL,
                        List.of(ourLine)
                ));

            } else if (ourLine.equals(baseLine)) {

                // only they changed
                chunks.add(new MergeChunk(
                        MergeEnum.NORMAL,
                        List.of(theirLine)
                ));

            } else if (theirLine.equals(baseLine)) {

                // only we changed
                chunks.add(new MergeChunk(
                        MergeEnum.NORMAL,
                        List.of(ourLine)
                ));

            } else {

                // conflict
                hasConflicts = true;

                chunks.add(new MergeChunk(
                        MergeEnum.CONFLICT,
                        List.of(
                                "<<<<<<< OURS",
                                ourLine,
                                "=======",
                                theirLine,
                                ">>>>>>> THEIRS"
                        )
                ));
            }
        }
        return new MergeResult(hasConflicts, chunks);
    }
    public List<MergeChunk>getConflictMarkers(MergeResult mergeResult)
    {
        List<MergeChunk>mergeChunkList=new ArrayList<>();
        for(MergeChunk mergeChunk:mergeResult.getChunks())
        {
            if(mergeChunk.getType()==MergeEnum.CONFLICT)
                mergeChunkList.add(mergeChunk);
        }
        return mergeChunkList;
    }
    public String serializeConflictMarkers(List<MergeChunk>mergeChunkList)
    {
        StringBuilder sb = new StringBuilder();

        for(MergeChunk mergeChunk:mergeChunkList)
        {
            sb.append(mergeChunk.getMerges().get(0)+"\n");
            sb.append(mergeChunk.getMerges().get(1)+"\n");
            sb.append(mergeChunk.getMerges().get(2)+"\n");
            sb.append(mergeChunk.getMerges().get(3)+"\n");
            sb.append(mergeChunk.getMerges().get(4)+"\n"+"\n");
        }
        return sb.toString();
    }

}
