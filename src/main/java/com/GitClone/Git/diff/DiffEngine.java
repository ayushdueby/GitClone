package com.GitClone.Git.diff;

import com.GitClone.Git.gitEnum.DiffType;
import com.GitClone.Git.model.DiffLine;

import java.util.*;
public class DiffEngine {

    public List<DiffLine> diff(String original, String modified) {

        String[] originalLines = original.split("\n");
        String[] modifiedLines = modified.split("\n");

        List<Map<Integer, Integer>> trace = buildEditGraph(originalLines, modifiedLines);

        return reconstructDiff(trace, originalLines, modifiedLines);
    }

    /**
     * Step 1: Build trace using Myers algorithm -- > this one was too hard
     *
     * right (delete)
     * down (insert)
     * diagonal (matched)
     *
     * okay soooo original one is on x axis and modified one is one y axis, this means
     * when we move right we are matching the next element in original string..
     * and when we move down we are matching the next element in modified string.
     *
     * x+1 = delete
     * y+1 = insert
     * x+1,y+1 = match
     */
    public List<Map<Integer, Integer>> buildEditGraph(String[] original, String[] modified) {

        int n = original.length;
        int m = modified.length;
        int maxSteps = n + m;

        // diagonal → farthest x reached
        Map<Integer, Integer> diagonalToX = new HashMap<>();
        diagonalToX.put(1, 0);

        List<Map<Integer, Integer>> trace = new ArrayList<>();

        for (int steps = 0; steps <= maxSteps; steps++) {

            // store snapshot for backtracking
            trace.add(new HashMap<>(diagonalToX));

            for (int diagonal = -steps; diagonal <= steps; diagonal += 2) {

                int currentX;

                // Decide move: down (insert) OR right (delete)
                if (diagonal == -steps ||
                        (diagonal != steps &&
                                diagonalToX.getOrDefault(diagonal - 1, 0)
                                        < diagonalToX.getOrDefault(diagonal + 1, 0))) {

                    // Move DOWN → insertion
                    currentX = diagonalToX.getOrDefault(diagonal + 1, 0);

                } else {
                    // Move RIGHT → deletion
                    currentX = diagonalToX.getOrDefault(diagonal - 1, 0) + 1;
                }

                int currentY = currentX - diagonal;

                // Follow diagonal (matching lines)
                while (currentX < n &&
                        currentY < m &&
                        original[currentX].equals(modified[currentY])) {

                    currentX++;
                    currentY++;
                }

                diagonalToX.put(diagonal, currentX);

                // Reached end → done
                if (currentX >= n && currentY >= m) {
                    trace.add(new HashMap<>(diagonalToX));
                    return trace;
                }
            }
        }

        return trace;
    }

    /**
     * backtrack kyu?
     * taki trace to add kr sake
     * Backtrack to build diff result -> to get the stackTrace for delete,insert and unchanged ones
     */
    public List<DiffLine> reconstructDiff(List<Map<Integer, Integer>> trace,
                                           String[] original,
                                           String[] modified) {

        List<DiffLine> result = new ArrayList<>();

        int x = original.length;
        int y = modified.length;

        for (int step = trace.size() - 1; step > 0; step--) {

            Map<Integer, Integer> currentV = trace.get(step);
            int diagonal = x - y;

            int previousDiagonal;

            // Decide previous move
            if (diagonal == -step ||
                    (diagonal != step &&
                            currentV.getOrDefault(diagonal - 1, 0)
                                    < currentV.getOrDefault(diagonal + 1, 0))) {

                previousDiagonal = diagonal + 1; // came from DOWN

            } else {
                previousDiagonal = diagonal - 1; // came from RIGHT
            }

            int previousX = trace.get(step - 1).get(previousDiagonal);
            int previousY = previousX - previousDiagonal;

            // Traverse diagonal (UNCHANGED lines)
            while (x > previousX && y > previousY) {
                result.add(new DiffLine(original[x - 1], DiffType.UNCHANGED));
                x--;
                y--;
            }

            // Handle ADD or DELETE
            if (x == previousX) {
                result.add(new DiffLine(modified[y - 1], DiffType.ADDED));
                y--;
            } else {
                result.add(new DiffLine(original[x - 1], DiffType.DELETED));
                x--;
            }
        }

        Collections.reverse(result);
        return result;
    }
}