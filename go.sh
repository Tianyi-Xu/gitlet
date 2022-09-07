# compile code
make

# remove the directory in which I have performed testing
# because it now contains old code/.class files
rm -rf ~/test-gitlet

# make the directory in which I will perform testing
mkdir ~/test-gitlet
mkdir ~/test-gitlet/gitlet

# copy all class files into ~/Downloads/testing, which is where I test my gitlet project
cp -r gitlet/*.class ~/test-gitlet/gitlet

# change to the testing directory
cd ~/test-gitlet


# prelude1.inc setup1.inc setup2.inc
java -ea gitlet.Main init
touch g.txt
touch f.txt
java -ea gitlet.Main add g.txt
java -ea gitlet.Main add f.txt
java -ea gitlet.Main commit "Two files"



java -ea gitlet.Main branch other 
touch h.txt
java -ea gitlet.Main add h.txt
java -ea gitlet.Main rm g.txt
java -ea gitlet.Main commit "Add h.txt and remove g.txt"


java -ea gitlet.Main checkout other
java -ea gitlet.Main rm f.txt
touch k.txt
java -ea gitlet.Main add k.txt
java -ea gitlet.Main commit "Add k.txt and remove f.txt"

java -ea gitlet.Main checkout master
java -ea gitlet.Main merge other
java -ea gitlet.Main log
 # touch m.txt
 # java -ea gitlet.Main add m.txt

# commmit 
# java -ea gitlet.Main commit Firstcommit



# log
# java -ea gitlet.Main global-log

# find
# java -ea gitlet.Main find Firstcommit

# status
# java -ea gitlet.Main status

# # branch
#  java -ea gitlet.Main branch doop

#  java -ea gitlet.Main checkout doop

#  echo "Hello world!" > hello.txt


# # add to repo
# java -ea gitlet.Main add hello.txt

# # commmit 
# java -ea gitlet.Main commit "first commit on doop"


# java -ea gitlet.Main rm hello.txt

# java -ea gitlet.Main status








