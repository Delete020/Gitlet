# Gitlet
CS61B Sp22 Project 3: Gitlet, your own version-control system

# Gitlet Design Document

author: Delete020

## 1. Classes and Data Structures

#### Main

Program startup class that parses the gitlet command and will decide to execute a method based on the input parameters

### GitletRepository

The main logic of the program is implemented where the non-remote commands of the gitlet are handled

#### instance variables

- `String CWD = System.getProperty("user.dir");`The Current Working Directory.
- `File GITLET_DIR = Utils.join(CWD, ".gitlet");` The `.gitlet` hidden folder, all gitlet related data will be stored in this directory
- `File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");` The object folder, which stores all blob fi
- `File COMMIT_DIR;` The commit folder, which stores all commit object
- `File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");` The folder where all branches are stored and each branch is persisted as a file in this folder
- `File HEAD = Utils.join(GITLET_DIR, "HEAD");`Head file, pointing to the current branch
- `File STAGE = Utils.join(GITLET_DIR, "stage");`The staging area file that records the files to be added to the next commit

#### static variables

- `ZONE_DATE_TIME_FORMATTER` data formatter, convert standard time formats

#### RemoteRepository

Contains remote command execution logic, similar to GitletRepository, but can manipulate remote gitlets

### Commit

### instance variables

- Message - the message of a commit
- Timestamp - Automatically set to current commit time
- Parent - The sha1 string of the parent commit
- blobs - Record each file that is committed

### Stage

#### instance variables

- `additionMap` - Record the files added to the staging area
- `removalMap` - Record the files to be removed in the next commit

## 2. Algorithms

### GitletRepository

1. `init() `- Creates a new Gitlet version-control system in the current directory.

   - `saveStage(Stage stage)` - Persistent stage object
   - `persistentCommit(String sha1, Serializable obj)` - Persistent commit object to 

2. `add(String filename)` - Adds a copy of the file as it currently exists to the staging area, use Stage object check file available to add 

   - `getObjectFile(String sha1) `- Get the file directory of the commit or blob by SHA1
   - Check the file is identical to the parent commit file or not, remove it from the staging area

3. `commit(String message)` - Create a new commit for saves a snapshot of tracked files

   - Get the new blobs by using the blobs of the last commit and the staging area
   - Clear staging area
   - Persistent new commit object
   - Update branch or head

4. `rm(String filename)` - Remove file from staging area or current working directory

5. `log() `- Display information about each commit backwards along the commit tree until the initial commit.

   - `displayCommitInfo(Commit commit, String commitSha1)` - Print all the information of a commit object

6. `globalLog()` - also using  displayCommitInfo method to display information about all commits ever made

7. `find(String message)` - Prints out the ids of all commits that have the given commit message

   - `Utils.plainFilenamesIn(File dir)` - Returns a list of the names of all plain files in the directory DIR

8. `status()` - Display the current branch information by checking the branch folder and staging area and comparing it with the working directory

   - `differentFile(String filename, Map<String, String> compare, List<String> modifyList)` - Check the sha1 of file in working directory same as given map

9. `checkout(String... args) `- Checkout of branch or file, three cases.

   - first check argument, it can updates the files in the working directory to match the version stored in given commit or current head commit, or Restore the entire working directory to the version of the specified branch
   - `restoreVersion(Map<String, String> headBlobs, String commitSha1) `- Restore working directory to the given commit version
   - `restoreWorkingDirectory(Map<String, String> restoreBlobs, Map<String, String> currentBlobs) `- Clear working directory file, then copy given blobs files to working directory

10. `reset(String commitId)` - Restore previous version and moves the current branch's head to that commit node. It is essentially `checkout` of an arbitrary commit

11. `branch(String branchName)` -  Create new branch

12. `rmBranch(String branchName)` - Remove a branch

13. `merge(String branchName) `-  Merges files from the given branch into the current branch. 

    - get current head blob, ancestor blobs, merge branch blobs to determine the final content of the file

    - `dfs(String sha1, Map<String, Integer> commitMap, int depth) `- Deep first search, for traverse current commit tree

    - `commonAncestor(String mergeSha1, String branchName)` -  find the latest common commit. use dfs get current commit tree. BFS traverse merge commit tree for search the common ancestor.

    - Determining which files (if any) have a conflict.

      - Representing the conflict in the file.
      - `restoreWorkingDirectory` Clear working directory file, then copy given blobs files to working directory
      - create merge commit

      * update branch

14. `diff(String... branches)` - Compares the contents of a commit with a working directory or compares two commits

    - Check the parameters to determine which is the case
    - `branchNotExist(String branch)` -  Check branch is not in the current gitlet
    - Get blobs and working directory files, diff for both files
    - `diffs(String filename, File firstVersion, File secondVersion) `- Use diff object to compare content of two file,  output compare information and sequence of edits

### RemoteRepository

1. `addRemote(String removeName, String path)` - add a remote gitlet directory as file save to current gitlet remote directory
2. `rmRemote(String removeName)` - remove remote gitlet file
3. `push(String remoteName, String remoteBranchName)` - push current commit to remote gitlet repository
   - get two gitletRepository
   - check the remote branch's head is in the history of the current local head
   - check current commit history
   - `pushFile(File currentGitlet, File remoteGitlet, Commit head)` - Push current files of head commit to the objects directory of remote gitlet
   - create new commit object and persistent commit to remote gitlet directory
4. `fetch(String remoteName, String remoteBranchName)` - Brings down commits from the remote Gitlet repository into the local Gitlet repository.
   - if branch not exists create new branch in current gitlet
   - copies all remote commits and blobs to current gitlet
   - copy remote commit object 
   - use `pushFile()` copy blobs to current objects directory
5. `pull(String remoteName, String remoteBranchName)` - Simple fetch and merge remote branch

## 3. Persistence

### .gitlet directory structure

```
CWD                             <==== Whatever the current working directory is.
└── .gitlet
    ├── branches
    │   └── master
    ├── commit
    │   ├── 321c9bb225085ff1e775613cc5e295448fa21a5f
    │   └── 34d677ab852a48acb18ccf6a5e6d35ee65e432cb
    ├── HEAD
    ├── objects
    │   ├── a0
    │   │   └── c0f3cb8c293ea70a7576c30f6d61226cba7679
    ├── remote
    └── stage
```

- `.gitlet/ `-- top level folder for all persistent data

- `objects/` - folder containing all blobs
- `branches/` - folder containing all of the persistent data for branch
- `commit/` - folder containing all of the persistent data for commit object
- `remote/ `- folder containing all of the remote gitlet repository directory
- `HEAD` - file  holds the branch or commit sha1 that currently pointing to.
- `STAGE` - staging area files, serialization of stage objects, saving of added and removed files

### GitletRepository

The `GitletRepository` will handle all gitlet commands

### init

1. Check the command operands correct or not, if not only one argument, exit program 
2. If the `.gitlet` folder doesn’t exist in `CWD`, create .gitlet directory structure
3. use `saveStage(Stage stage)` persistent stage object to `.gitlet/stage`  file
4. Create first commit with an init message and no file saved. use `persistentCommit(String sha1, Serializable obj)` persistent commit object to `.gitlet/commit` directory
5. use `Utils.writeContents` add the sha1 to branch and head file

#### add

when use `java gitlet.Main add [file name]`

1. Check if the given file exists in the working directory
2. Use `Utils.readObject` Read `.gitlet/stage` and add this file using its internal methods
3. Remove the given file from removalMap
4. Check if it is the same as the one saved in the current branch, and if it is, remove the file from the additionMap and return
5. Add this file to the staging area 
6. If the file already exists, do nothing, otherwise copy the file from the working directory to the `.gitlet/object`

#### commit

when use ` java gitlet.Main commit [message]`

1. if staging area is not empty, create a new commit object and add file in the staging area to blobs, remove file by the  rm command from blobs 
2. clear staging area
3. update head or currently used branch to point to the new commit
4. use `persistentCommit(String sha1, Serializable obj)`persistent new commit object as file with sha1 of filename in `.gitlet/commit`

#### rm

when use` java gitlet.Main rm [file name]`

1. use `getStage()` read stage object
2. if file currently staged in addition, remove it from addition
3. if file is not staged in addition, but tracked in the current head commit, add to stage removal map and delete this file from the working directory `CWD` 
4. use `Utils.writeObject` persistent stage file 

#### checkout

when use `java gitlet.Main checkout -- [file name]` and `java gitlet.Main checkout [commit id] -- [file name]`

1. use `Utils.restrictedDelete` delete file
2. use `Files.copy` copy file from `.gitlet/objects` to `CWD`

when use `java gitlet.Main checkout [branch name]`

1. if a commit id is given, restore the file to that commit version, which by default is to restore the file to the current commit version
2. if a branch name is given and there are no untracked file and unstaged file in the working directory, delete all file in the working directory, and copy all file by the given branch commit  to working directory
3. use `Utils.writeContents` change current branch to the given branch

#### reset

1. check commit exists or not
2. if working directory had modified file do nothing
3. in order to not overwrite untracked files, check working file is untracked, but have same filename of restore commit
4. use `Utils.restrictedDelete` delete all files in the working directory
5. use `Files.copy` copy all blobs file in `.gitlet/objects` to working directory`CWD`
6. reset staging area file
7. update branch

#### branch

1. check branch is already exits or not
2. Create new branch file in `.gitlet/branches/`

#### rm-branch

1. check branch is already exits or not
2. delete that branch file from `.gitlet/branches/`

#### merge

1. if file conflict, merge two file content and use `Utils.writeContents` save file to `.gitlet/objects`
2. merge will clear `CWD` directory
3. use `Files.copy` copy all blobs file from `.gitlet/objects` to `CWD`
4. create new merge commit object, save new commit file to `.gitlet/commit`
5. update branch

### RemoteRepository

#### add-remote

1. create new file in `.gitlet/remote` directory
2. file with content text of remove directory

#### rm-remote

1. delete file in `.gitlet/remote` directory

#### push

1. Copy all the files in the current head commit blobs to the remote, from the current `.gitlet/objects`to the remote `.gitlet/objects`
2. create new commit object in remote gitlet

#### fetch

1. create new branch to `.gitlet/branch` with remote name and remote branch name as current branch name 
2. Copy all commits in the remote `.gitlet/commit` to the current `.gitlet/commit`
3. Copy all the files from the remote `.gitlet/objects`to the current `.gitlet/objects`

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.
