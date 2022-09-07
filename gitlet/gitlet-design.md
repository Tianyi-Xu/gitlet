# Gitlet Design Document

**Name**: Tianyi Xu

## Classes and Data Structures

### Commit

This class represents the commit AKA the snapshot of the repository.

#### field:

1. `String message`: the commit message 
2. `Date time`:  the timestamp of the commit
3. `Commit parent` : the parent  of the commit (the SHA of the commit where this  commit comes from )
4. `Commit parent2`: the other parent  of the commit if it has (If merge happens)
5. `Map<String String> blobs`: the files in this commit. Key values are filename to SHA of the file

### GTTree

This class represents the commit tree of the repository.

`Commit HEAD` The pointer to the most recent commit on the branch we current at

`String branch`:  the branch we current at

`Map<String, String> stagedFile` The staged files (added files). key is file name, value is the SHA of the file content .  Has functions to add a file to the stage (Serialize the file at the same time), remove file from the stage and clear stage and make a new commit

`Map<String, String> branches`:  a list of all the branches of the commit tree. Key values are branch name to the SHA of most recent commit on the branch. (Used for check out branches)



### Main class

This class is the driver class of the program  which contains different git instructions.





## Algorithms

### Commit

constructor method of commit

saveCommit: method to save a serialized commit object to the disk;

static readCommit: method to deserialize a commit from the disk to a object.

get methods to get field of the commit

### GTTree

#### init():

Check if the GTTree has been initliazed before. 

If not:

​	make direcories inside the working directory.

​	create the first commit

​	initialize Master branch, and HEAD pointer to the first commit

#### add(File file):

Before staging the file, compare the file to be added with:

1. The files in the commit HEAD pointing to.  If the file is in the commit, compare the file contents, if matches exactly, no action needed. 
2. The files in the staging area.  if the file to be added is in the staging area already, compare the content of the files, if matches exactly, no action needed.

Stage the file by create a blob for the file and save the file to the staging area.



#### commit(String message)

If staging area is clean, no action needed.

make a copy the commit the HEAD pointed at. update the new commit object's message, timestamp and parents. Also modify blobs by the stagedFiles.

 Advance the HEAD and the according branch pointer to the current commit. 

clear staging area.



#### checkout()

`checkout(String filename`)

Check if the file exists in the commit HEAD pointed to. 

If the file exists, check if the working directory has the file, if not, create new file, if yes, overwrite the existing file.

`java gitlet.Main checkout `

First check if the user provide correct id and filename.

if file does exists in the commit, read the file content in the commit and write it back to the file.

checkout(String filename)

`java gitlet.Main checkout [branch name]`

Check if the branch exists

Check if the branch is the branch current at

 Check if there's untracked file in the working directory but not in the commit, throw error  and exit if it does.

Take all the files in the most recent commit branch back to the working directory,  if the file not in the working directory, create new one. If the file already in the working directory, overwrite it.

move HEAD and branch pointer to the commit.

Clear staging area



### Main

main(String[] args): Create a git tree object. Call commands based on the args user provided on the git tree object.

Contains methods check command arguments.





## Persistence

In order to persist the history of the repository since the initialization,  we need to keep track of the version of the files at each commit. 

1. Write the files to disk everytime we add a file.  Serialize the content of the files into bytes and also use SHA as the file id to represent the content. we then store the serilized bytes to a file named after the SHA id. All the versions of the files would be stored in the directory called `blobs` inside the `.git`folder. 



2. Write the commit objects to disk everytime we make a commit. Each commit which contains metadata, the current structure and files in the repository would also be serialized and the SHA of its serialized bytes would be computed. We save the commit object in the directory called `commits` inside the `.git`folder.  

If we want to revert the current repository back to the previous commit, we need to search for the files in the working directory. Since the SHA is a hashcode has property that for two different byte streams, they won't have the same hash value, our program always knows which commit and which files to look for. By calling checkout on the commit id(SHA of the commit object), we can deserialize the commit we saved in the `commits` folder and rewrite the files to a previous version saved in the commit by finding those files by their SHA id in the `commits` folder and deserialize those files. Similarly, by calling checkout on the commit id and some filenames, we would only revert those files state back while leaving other files untouched.













