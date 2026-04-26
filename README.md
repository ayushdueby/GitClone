# Git version controle with AI-Powered Semantic Diff

A production-grade version control system built from scratch in Java and Spring Boot, implementing core Git internals including a content-addressable object store, commit DAG, Myers diff algorithm, 3-way merge engine, and an AI-powered semantic diff layer that understands the intent behind code changes.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Core Components](#core-components)
- [AI Semantic Diff Engine](#ai-semantic-diff-engine)
- [API Reference](#api-reference)
- [Data Structures and Algorithms](#data-structures-and-algorithms)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)

---

## Overview

Most developers use Git every day without understanding what happens underneath. This project is a ground-up implementation of Git's core internals — not a wrapper around Git, not a library, but a working version control engine built from first principles.

On top of the core engine sits an AI semantic diff layer that goes beyond what Myers algorithm can tell you. Where Myers sees line deletions and insertions, the semantic layer understands that five scattered changes are part of one refactor, that a deletion and addition on adjacent lines is actually a rename, and that a PR is too large and should be split.

This combination — a hand-built Git engine with an AI layer that understands intent — does not exist as an open product today.

---

## Architecture

```
REST API Layer (Spring Boot Controllers)
            |
            v
  +---------+-----------+----------+
  |         |           |          |
Object    Commit      Ref        AI Semantic
Store     DAG         Manager    Diff Engine
  |         |           |          |
  +---------+-----------+----------+
            |
            v
  +---------+---------+
  |                   |
Diff Engine       Merge Engine
(Myers)           (3-way merge)
  |                   |
  +-------------------+
            |
            v
      Storage Layer
  (.git folder simulation)
```

The system is layered deliberately. Each component has a single responsibility and can be reasoned about in isolation. The AI layer sits above the diff engine and consumes its output rather than replacing it.

---

## Core Components

### Object Store

The object store is the foundation of the entire system. Every piece of data — file contents, directory snapshots, commit records — is stored as an object identified by the SHA-256 hash of its content.

This is content-addressable storage. The same content always produces the same SHA. If two files have identical content, they are stored exactly once. This is how Git achieves deduplication across your entire history.

Three object types exist in the store:

**Blob** represents raw file content. A blob has no filename — it is purely the content. The filename lives in the tree that references the blob. This separation means renaming a file creates no new blob if the content has not changed.

**Tree** represents a directory snapshot. It is a map of filenames to blob SHAs. When a file is modified, a new blob is created and a new tree is created pointing to the new blob. The old tree and blob are preserved forever, which is how Git keeps your full history.

**Commit** represents a point in time. It holds a pointer to a tree (the state of all files at that moment), a pointer to its parent commit or commits, and metadata including author, timestamp, and message. The SHA of a commit is computed from all of this together, which means you cannot alter a commit without changing its SHA and breaking the chain.

### Commit DAG

The commit history is a directed acyclic graph. Each commit node points to its parent or parents. It is acyclic because a commit cannot reference a future commit — causality flows in one direction.

The DAG supports multiple parents, which is what makes merge commits possible. A merge commit has two parents: the tip of the current branch and the tip of the branch being merged in.

Key operations on the DAG:

`getHistory(sha)` performs an iterative depth-first traversal from a given commit backwards through parent pointers, collecting the full ancestor chain.

`findLCA(sha1, sha2)` finds the lowest common ancestor of two commits — the point where two branches diverged. This is the base commit used in 3-way merge. The algorithm collects all ancestors of sha1 into a set, then walks sha2's ancestors until it finds one present in that set.

`isAncestor(potentialAncestor, ofSha)` determines whether one commit is reachable from another. This is used to detect fast-forward merge situations where no merge commit is needed.

### Ref Manager

Refs are named pointers to commit SHAs. A branch is a ref. HEAD is a special ref that points to the current branch name rather than directly to a SHA.

This indirection is important. When you commit, the branch ref is updated to point to the new commit. HEAD continues to point to the branch name, so it implicitly moves forward with every commit. Detached HEAD occurs when HEAD points directly to a SHA instead of a branch name.

### Staging Index

The index is a flat map of file paths to blob SHAs representing files that have been staged with `add` but not yet committed. When `commit` is called, the index is used to build a tree, the tree is used to create a commit, and the index is cleared.

---

## AI Semantic Diff Engine

This is the component that does not exist elsewhere in this form.

### The problem with Myers diff alone

Myers algorithm finds the shortest edit script between two versions of a file. It is optimal in terms of the number of insertions and deletions. But shortest edit distance does not mean most meaningful diff.

Consider a function that is renamed and has a bug fixed inside it in the same commit. Myers produces a sequence of deleted and added lines. It does not know that the rename and the bugfix are separate concerns. It does not know that the rename is low risk and the bugfix is high risk. It does not know that this PR should probably have been two separate commits.

That understanding requires reading the diff the way a senior engineer would read it.

### How the semantic layer works

The semantic diff engine sits on top of Myers. It takes the `List<DiffLine>` output from the existing diff engine, serializes it into a structured representation, and sends it to an AI model with a prompt that asks for semantic analysis.

The prompt instructs the model to return structured JSON containing:

- A one-line summary of what changed
- A classification of the change type: REFACTOR, BUGFIX, FEATURE, RENAME, or CLEANUP
- Semantic groups that cluster related lines together under a human-readable explanation
- Specific improvement suggestions based on the changes
- A risk level of LOW, MEDIUM, or HIGH with a reason

The response is parsed from JSON into a typed `SemanticDiffResult` model. If the AI response cannot be parsed — for instance if the model wraps the JSON in markdown code fences — the raw text is stripped before parsing, and a fallback result is returned if parsing still fails.

### Semantic grouping

The most valuable output of the semantic layer is semantic grouping. A single diff might contain lines that belong to conceptually different changes spread across the file. A rename at the top, a null check fix in the middle, an unused import removal at the bottom. Myers presents these as a flat sequence. The semantic layer groups them:

```json
{
  "semanticGroups": [
    {
      "explanation": "Method rename from getUserById to fetchUserFromDB — low risk, purely cosmetic",
      "lines": [...]
    },
    {
      "explanation": "Null check added before database call — prevents NullPointerException on missing user",
      "lines": [...]
    },
    {
      "explanation": "Unused import removed",
      "lines": [...]
    }
  ]
}
```

This is the output a code reviewer actually wants. Not a flat list of line changes, but a structured explanation of what happened and why each part matters.

### Risk assessment

The semantic layer assigns a risk level to the overall diff. A pure rename with no logic changes is LOW. A change to error handling or authentication logic is HIGH. The risk level is computed by the AI based on the nature of the changes, not by a rule engine — which means it handles novel cases that a rules-based system would miss.

### PR quality signals

Beyond the diff itself, the semantic layer can flag structural problems with a PR:

- The diff spans too many unrelated concerns and should be split
- A logic change has no corresponding test change in the diff
- A public API has changed without a version bump

These are the comments a senior engineer leaves in code review. The semantic layer surfaces them automatically.

---

## API Reference

### Repository Operations

`POST /git/init`
Initializes a new repository and sets HEAD to main.

`GET /git/status`
Returns current branch, staged files, and HEAD SHA.

### File Operations

`POST /git/add?path={filePath}`
Stages a file. Request body is the raw file content.

`POST /git/commit?message={message}&author={author}`
Creates a commit from the current staging index.

### History

`GET /git/log`
Returns the commit history of the current branch from HEAD backwards.

`GET /git/log/graph`
Returns all commits across all branches as a graph structure with node and edge information suitable for visualization.

### Branching

`POST /git/branch?name={branchName}`
Creates a new branch at the current HEAD.

`GET /git/branch`
Lists all branches and their current HEAD SHAs.

`DELETE /git/branch?name={branchName}`
Deletes a branch. Blocked if the branch is currently checked out or has unmerged commits.

`POST /git/checkout?target={branchOrSha}`
Switches to a branch or a specific commit SHA. Restores the working index from the target commit's tree.

### Diff

`GET /git/diff?sha1={sha1}&sha2={sha2}&file={filePath}`
Returns a line-by-line Myers diff between a file at two commits. Each line is annotated as ADDED, DELETED, or UNCHANGED.

`GET /git/diff/working?file={filePath}&newContent={content}`
Compares the current content of a file against its HEAD version.

### Merge

`POST /git/merge?branch={branchName}`
Merges the target branch into the current branch. If the merge is a fast-forward, the branch pointer is advanced with no merge commit. If there are no conflicts, a merge commit with two parents is created automatically. If there are conflicts, conflict markers are returned and no commit is created.

`GET /git/merge/preview?branch={branchName}`
Returns the predicted conflict regions without performing the merge.

### AI Semantic Diff

`GET /git/ai/diff?sha1={sha1}&sha2={sha2}&file={filePath}`
Runs Myers diff between the two commits for the given file, then passes the result through the AI semantic layer. Returns a `SemanticDiffResult` containing summary, change type, semantic groups, suggestions, and risk level.

---

## Data Structures and Algorithms

### SHA-256 Content Addressing

Every object stored in the system is identified by the SHA-256 hash of its serialized content. This provides O(1) lookup by SHA, automatic deduplication, and integrity verification — if the content changes, the SHA changes, and any reference to the old SHA still points to the original content.

### Myers Diff Algorithm

Myers algorithm solves the shortest edit script problem by modeling the diff as a path-finding problem on a two-dimensional edit graph. The X axis represents positions in the original file and the Y axis represents positions in the modified file. Moving right is a deletion, moving down is an insertion, and moving diagonally is a match — a line present in both files.

The algorithm searches this graph in order of edit distance, starting from zero edits and expanding outward. It uses a one-dimensional array indexed by diagonal number `k = x - y` to track the furthest-reaching point on each diagonal at each edit distance. Snapshots of this array are saved at each edit distance level to enable backtracking.

Backtracking reconstructs the actual edit script by reading the snapshots in reverse, recovering which moves were made at each step and translating them into ADDED, DELETED, and UNCHANGED annotations on the diff lines.

The time complexity is O(ND) where N is the sum of the lengths of the two files and D is the number of edits. For typical code changes where D is small relative to N, this is effectively linear.

### Lowest Common Ancestor on a DAG

Finding the merge base of two branches requires finding their lowest common ancestor in the commit DAG. The algorithm collects all ancestors of the first commit into a hash set using depth-first traversal, then traverses the ancestors of the second commit in order until it finds one present in the set. The first match is the LCA.

This works correctly for DAGs with multiple parents per node, which is necessary to support merge commits in the history.

### 3-Way Merge

Three-way merge takes three versions of a file: the base (the common ancestor commit), ours (the current branch), and theirs (the branch being merged). It diffs base against ours and base against theirs separately using the Myers engine, then walks through the base line by line applying both sets of changes.

When only one side changed a line relative to base, that change is applied automatically. When both sides changed the same line relative to base, a conflict is recorded. The conflict output follows standard Git conflict marker format with the OURS and THEIRS versions delimited.

When no conflicts are found, a merge commit is created automatically with two parent SHAs. When conflicts exist, the conflicted file content is returned and no commit is created — the user must resolve conflicts and commit manually.

Fast-forward detection uses `isAncestor()` to check whether the current branch is a direct ancestor of the target branch. If so, the branch pointer is simply advanced to the target without creating a merge commit.

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- A Gemini API key for the semantic diff feature

### Setup

Clone the repository and add your Gemini API key to `application.properties`:

```
gemini.api.key=your_key_here
```

Run the application:

```
mvn spring-boot:run
```

The server starts on port 8080.

### Quick start

```bash
curl -X POST http://localhost:8080/git/init

curl -X POST "http://localhost:8080/git/add?path=Main.java" \
     -d "public class Main { public static void main(String[] args) {} }"

curl -X POST "http://localhost:8080/git/commit?message=initial+commit&author=dev"

curl -X POST "http://localhost:8080/git/branch?name=feature"
curl -X POST "http://localhost:8080/git/checkout?target=feature"

curl -X POST "http://localhost:8080/git/add?path=Main.java" \
     -d "public class Main { public static void main(String[] args) { System.out.println(\"hello\"); } }"

curl -X POST "http://localhost:8080/git/commit?message=add+output&author=dev"

curl "http://localhost:8080/git/ai/diff?sha1=FIRST_SHA&sha2=SECOND_SHA&file=Main.java"
```

---

## Project Structure

```
src/main/java/com/gitclone/
    model/
        GitObject.java
        Blob.java
        Tree.java
        Commit.java
        GitIndex.java
    store/
        ObjectStore.java
    dag/
        CommitDAG.java
    refs/
        RefManager.java
    diff/
        DiffEngine.java
        DiffLine.java
    merge/
        MergeEngine.java
        MergeResult.java
        MergeChunk.java
    ai/
        SemanticDiffService.java
        SemanticDiffResult.java
        SemanticGroup.java
    service/
        GitService.java
        DiffService.java
        MergeService.java
    controller/
        GitController.java
```
