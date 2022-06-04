# Gitlet Design Document
author: Delete020

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design
document should be written in markdown, a language that allows you to nicely 
format and style a text file. Organize your design document in a way that 
will make it easy for you or a course-staff member to read.  

## 1. Classes and Data Structures

Include here any class definitions. For each class list the instance
variables and static variables (if any). Include a ***brief description***
of each variable and its purpose in the class. Your explanations in
this section should be as concise as possible. Leave the full
explanation to the following sections. You may cut this section short
if you find your document is too wordy.

### GitletRepository

The main logic of the program is implemented where the non-remote commands of the gitlet are handled

#### static variables

- `static final String CWD = System.getProperty("user.dir");`The Current Working Directory.
- `static final File GITLET_DIR = Utils.join(CWD, ".gitlet");` The `.gitlet` hidden folder, all gitlet related data will be stored in this directory
- `static final File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");` The object folder, which stores all commit and blob files
- `static final File BRANCH_DIR = Utils.join(GITLET_DIR, "branches");` The folder where all branches are stored and each branch is persisted as a file in this folder
- `static final File HEAD = Utils.join(GITLET_DIR, "HEAD");`Head file, pointing to the current branch
- `static final File STAGE = Utils.join(GITLET_DIR, "stage");`The staging area file that records the files to be added to the next commit

### Commit

### instance variables

- Message - the message of a commit
- Timestamp - Automatically set to current commit time
- Parent - The sha1 string of the parent commit
- blobs - Record each file that is committed

### Stage

#### instance variables

- additionMap - Record the files added to the staging area
- removalMap - Record the files to be removed in the next commit

## 2. Algorithms

This is where you tell us how your code works. For each class, include
a high-level description of the methods in that class. That is, do not
include a line-by-line breakdown of your code, but something you would
write in a javadoc comment above a method, ***including any edge cases
you are accounting for***. We have read the project spec too, so make
sure you do not repeat or rephrase what is stated there.  This should
be a description of how your code accomplishes what is stated in the
spec.


The length of this section depends on the complexity of the task and
the complexity of your design. However, simple explanations are
preferred. Here are some formatting tips:

* For complex tasks, like determining merge conflicts, we recommend
  that you split the task into parts. Describe your algorithm for each
  part in a separate section. Start with the simplest component and
  build up your design, one piece at a time. For example, your
  algorithms section for Merge Conflicts could have sections for:

   * Checking if a merge is necessary.
   * Determining which files (if any) have a conflict.
   * Representing the conflict in the file.
  
* Try to clearly mark titles or names of classes with white space or
  some other symbols.

## 3. Persistence

Describe your strategy for ensuring that you don’t lose the state of your program
across multiple runs. Here are some tips for writing this section:

* This section should be structured as a list of all the times you
  will need to record the state of the program or files. For each
  case, you must prove that your design ensures correct behavior. For
  example, explain how you intend to make sure that after we call
       `java gitlet.Main add wug.txt`,
  on the next execution of
       `java gitlet.Main commit -m “modify wug.txt”`, 
  the correct commit will be made.
* A good strategy for reasoning about persistence is to identify which
  pieces of data are needed across multiple calls to Gitlet. Then,
  prove that the data remains consistent for all future calls.
* This section should also include a description of your .gitlet
  directory and any files or subdirectories you intend on including
  there.

The `GitletRepository` will handle all gitlet commands

### init

1. Check the command operands correct or not, if not only one argument, exit program 
2. If the `.gitlet` folder doesn’t exist, create it and initialize branch folder and objects folder,  head and stage file, and first branch master
3. Create first commit  with an init message and no file saved

#### add

1. Check if the given file exists in the working directory
2. Read the persistent stage file and add this file using its internal methods
3. Remove the given file from removalMap
4. Check if it is the same as the one saved in the current branch, and if it is, remove the file from the additionMap and return
5. Add this file to the staging area 
6. If the file already exists, do nothing, otherwise copy the file from the working directory to the objects directory

#### commit

1. if staging area is not empty, create a new commit object and add file in the staging area to blobs, remove file by the  rm command from blobs 
2. clear staging area
3. update head or currently used branch to point to the new commit

#### rm

1. if file currently staged in addition, remove it from addition
2. if file is not staged in addition, but tracked in the current head commit, add to stage removal map and delete it from the working directory 

## 4. Design Diagram

Attach a picture of your design diagram illustrating the structure of your
classes and data structures. The design diagram should make it easy to 
visualize the structure and workflow of your program.

