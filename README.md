# EC2Tool
A tool for instantiating, terminating, and listing AWS EC2 instances.

#What does it do?
EC2Tool was created as a command line tool to spin up EC2 instances, but
from there it spun out of control a bit and became a tool that tries to
do everything EC2.  The tool as it exists currently can do the following 
tasks:

* Instantiate EC2 instances of whatever type and number you desire.
  * But only t2.micro Amazon Linux AMI 2014.09.1 (HVM) - ami-b5a7ea85 instances have been tested
* Terminate all existing EC2 instances under your credentials.
* Generate credentials files, as long as your ok with me messing around in your machine!
  * You can do it yourself at the following link:  http://aws.amazon.com/developers/getting-started/java/
* Generate a security group for use while the tool is running.  
  * Should you want to get rid of it later it's called "bitcrusher"
* List running ec2 instances with their status.
* And much much more!  Not really, sorry.  :(

#Prerequisites
The tool requires that you have ant installed.  You can get it here:
http://ant.apache.org/manual/index.html

If you are in linux you can get it more easily by running the
following two commands:
    sudo-apt-get update
    sudo-apt-get install ant

If you are in osx I suggest you set up
[homebrew](http://brew.sh/ "Homebrew") and 
run the following command:
    brew install ant

Doing it the apache way is not fun.

#Running the tool

For an initial build of the tool:
    ant

To create a security group:
    ant ec2grp

To create a security key (Make sure you remember the name of the key!):
    ant ec2gen

To add your ip to the existing security group:
    ant ec2ip

To spin up an ec2 instance:
    ant ec2inst

To terminate all of the running instances:
    ant ec2term

To clean up everything, this includes the following:
* Delete all .pem files
* Delete the class file
* Delete the key associated with the pem file 
	ant clean

Good luck...  It's probably borken.
