import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceStateChange;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;


/**
 * EC2Tool is a class that spawns ec2 instances, lists
 * the running instances, and terminates ec2 instances.  
 * 
 * To facilitate that use the class also includes options
 * to create a default security group, add ips to the whitelist
 * for the security group, and to create credentials files.
 * 
 * Prerequisites: Users must have ant installed.  
 * Ant install and download information is here:
 * http://ant.apache.org/manual/index.html
 * 
 * Usage:  
 * 
 * @author Matthew Neal
 *
 */
public class EC2Tool {
	/**Constants*/
	
	/** AWS return code for an image that is shutting down */
	private static final Integer SHUTTING_DOWN = 32;
	/** EC2 image for Amazon Linux, this ID will go out of date eventually */
	private static final String IMAGE_ID = "ami-b5a7ea85";
	/** Constant for the number of instances to spawn by default */
	private static final int NUM_INSTANCES = 1;
	/** Constant for the type of instant to spawn */
	private static final String INSTANCE_TYPE = "t2.micro";
	/** Default group name for EC2 Tool */
	private static final String GROUP = "bitcrusher";
	/** Length of key ids for AWS */
	private static final int KEY_ID_LENGTH = 20;
	/** Length of keys for AWS */
	private static final int KEY_LENGTH = 40;
	/** Port number to open for SSH */
	private static final Integer SSH = 22;
	
	/**Global Variables*/
	
	/** amazonec2 instance to use within the class */
	private static AmazonEC2 ec2 = null;
	/** Name of the key to be used */
	private static String keyname = null;
	
	
	public static void main(String[] args) {
		if(args.length < 1){
	    	System.out.println("Usage: java -jar DemoRunner.jar <options>");
	    	System.out.println("Options: -l list instances, -g generate key, -i new instance, "
	    			+ "-t terminate instances, -s generate security group -c generate credentials");
	    	System.out.println("-ip reset security group ip");
	    	System.exit(0);
	    }
		
		if(args[0].equals("-c")){
			genCredentials();
		}
		
		//If not generating credentials begin credentialing.
		
		AWSCredentials credentials = null;
	    try {
	        credentials = new ProfileCredentialsProvider("default").getCredentials();
	    } catch (Exception e) {
	        throw new AmazonClientException(
	                "Credentials could not be loaded.",
	                e);
	    }
	    
	    ec2 = new AmazonEC2Client(credentials);
	    ec2.setEndpoint("ec2.us-west-2.amazonaws.com");
		
	    if(args[0].equals("-g")){
	    	genKey();
	    }
	    if(args[0].equals("-i")){
	    	spawnInstance();	
	    }
	    
	    if(args[0].equals("-t")){
	    	terminateInstances();
	    }
	    if(args[0].equals("-l")){
	    	listLiveInstances();
	    }
	    if(args[0].equals("-s")){
	    	genSecGroup();
	    }
	    
	    if(args[0].equals("-ip")){
	    	setIpForGroup();
	    }
	    if(args[0].equals("-d")){
	    	delKey();
	    }
	}
	
	private static void setIpForGroup() {
		System.out.println("This program will authorize your ip with the bitcrusher group");
		
		String ip = "0.0.0.0/0";
		
		try{
			InetAddress addy = InetAddress.getLocalHost();
			ip = addy.getHostAddress() + "/10";
		}catch(UnknownHostException e){
			System.out.println("Error getting your ip.");
			System.exit(0);
		}
		
		List<String> ipRanges = Collections.singletonList(ip);
		
		IpPermission ipPerm = new IpPermission();
		ipPerm.setFromPort(SSH);
		ipPerm.setToPort(SSH);
		ipPerm.setIpRanges(ipRanges);
		ipPerm.setIpProtocol("tcp");
		
		List<IpPermission> ipPermList = Collections.singletonList(ipPerm);
		
		try{
			AuthorizeSecurityGroupIngressRequest ingRequest = 
					new AuthorizeSecurityGroupIngressRequest("bitcrusher", ipPermList);
			ec2.authorizeSecurityGroupIngress(ingRequest);
			System.out.println("Your ip has been authorized with the bitcrusher group.");
			
		}catch(AmazonServiceException e){
			System.out.println("Ip already authorized");
			System.exit(0);
		}
		
	}

	/**
	 * This is broken right now.  The ip is set to a general ip, so anyone can access via
	 * ssh.  It's unlikely there would be a problem since a user would still need to have a 
	 * private key that is authorized, but this should be fixed.
	 */
	private static void genSecGroup() {
		System.out.println("This Program will generate a security group");
		System.out.println("with the name bitcrusher and ssh ability for");
		System.out.println("your ip address.");
		try{
			CreateSecurityGroupRequest secGrpRequest = new CreateSecurityGroupRequest("bitcrusher", "Ingress group for AWSTool");
			CreateSecurityGroupResult  res = ec2.createSecurityGroup(secGrpRequest);
			System.out.println("Group " + res.getGroupId() + "created.");
			
		}catch(AmazonServiceException ase){
			System.out.println("Group exists.  If needed set permissions with -ip option");
			System.exit(0);
		}
		
		String ip = "0.0.0.0/0";
		
		
		List<String> ipRanges = Collections.singletonList(ip);
		
		IpPermission ipPerm = new IpPermission();
		ipPerm.setFromPort(SSH);
		ipPerm.setToPort(SSH);
		ipPerm.setIpRanges(ipRanges);
		ipPerm.setIpProtocol("tcp");
		
		List<IpPermission> ipPermList = Collections.singletonList(ipPerm);
		
		try{
			AuthorizeSecurityGroupIngressRequest ingRequest = 
					new AuthorizeSecurityGroupIngressRequest("bitcrusher", ipPermList);
			ec2.authorizeSecurityGroupIngress(ingRequest);
			System.out.println("Your ip has been authorized with the bitcrusher group.");
			
		}catch(AmazonServiceException e){
			System.out.println("Ip already authorized");
			System.exit(0);
		}
	}

	private static void genCredentials(){
		System.out.println("This program will generate a credentials file for you at: ~/.aws/credentials");
		System.out.println("Do you want to continue? (y/n)");
		Scanner console = new Scanner(System.in);
		if(console.hasNext()){
			String in = console.next();
			if(in.equals("y")){
				//do nothing
			}else{
				System.exit(0);
			}
		}else{
			System.exit(0);
		}
		
		System.out.println("Enter aws access key id:");
		String id = null;
		String key = null;
		id = console.next();
		if(id.length() < KEY_ID_LENGTH){
			System.out.println("Incorrect key id format.");
			System.exit(0);
		}
		
		
		
		System.out.println("Enter aws access key id:");
		key = console.next();
		console.close();
		if(key.length() < KEY_LENGTH){
			System.out.println("Incorrect key format.");
			System.exit(0);
		}
		
			
		String def = "[default]\n";
		String nwln = "\n";
		
		String idln = "aws_access_key_id = ";
		idln = idln.concat(id);
		idln = idln.concat(nwln);
		
		
		String keyln = "aws_secret_access_key = ";
		keyln = keyln.concat(key);
		keyln = keyln.concat(nwln);
		
		FileOutputStream out = null;
		
		try{
			String homeDir = System.getProperty("user.home");
			File dir = new File(homeDir + "/.aws");
			if(!dir.exists()){
				if(!dir.mkdir()){
					System.out.println("Failed to create directory!");
					System.exit(0);
				}
			}
			
			File f = new File(homeDir + "/.aws/credentials");
			if(f.exists()){
				System.out.println("AWS Credentials exist already!");
				System.exit(0);
			}
			
			out = new FileOutputStream(f);
			byte[] by = def.getBytes();
			out.write(by);
			by = idln.getBytes();
			out.write(by);;
			by = keyln.getBytes();
			out.write(by);
			out.flush();
			out.close();
			
			
		}catch (IOException e){
			System.out.println("IO failed!");
		
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					System.out.println("IO failed!");
				}
			}
		}
		System.out.println("File written.");
		System.exit(0);
		
		
		
	}
	
	
	private static String listLiveInstances(){
		DescribeInstancesResult descrInst = ec2.describeInstances();
		   
		List<Reservation> reservations = descrInst.getReservations();
		String ip = null;
		String ip2 = null;
		for(int i = 0; i < reservations.size(); i++){
		   Reservation cur = reservations.get(i);
		   String resId = cur.getReservationId();
		   List<Instance> instances = cur.getInstances();
		   for(int j = 0; j < instances.size(); j++){
			   Instance curInst = instances.get(j);
			   ip = curInst.getPublicIpAddress();
			   InstanceState status = curInst.getState();
			   if(ip == null){
				   //Do nothing
			   }else{
				   System.out.println("Live ip on reservation id " + resId + " is: " + ip + " status is: " + status.getName());
				   //Added so that only a non null ip will be returned
				   ip2 = ip;
			   }
		   }
		}
		return ip2;
	}
	
	private static void terminateInstances(){
	   DescribeInstancesResult descrInst = ec2.describeInstances();
	   List<Reservation> reservations = descrInst.getReservations();
	   for(int i = 0; i < reservations.size(); i++){
		   Reservation cur = reservations.get(i);
		   List<Instance> instances = cur.getInstances();
		   for(int j = 0; j < instances.size(); j++){
			   Instance curInst = instances.get(j);
			   String ip = curInst.getPublicIpAddress();
			   if(ip == null){
				   //Do nothing
			   }else{
				   String instId = curInst.getInstanceId();
				   System.out.println(ip + " about to be terminated!");
				   TerminateInstancesRequest term = new TerminateInstancesRequest();
				   term.withInstanceIds(instId);
				   TerminateInstancesResult termRes = ec2.terminateInstances(term);
				   List<InstanceStateChange> stateList = termRes.getTerminatingInstances();
				   //Hardcoded to zero since there should only be one instance
				   if(stateList.get(0).getCurrentState().getCode() == SHUTTING_DOWN){
					   System.out.println("Terminated");
				   }
			   }
		   }
	   }
	}
	
	/**
	 * Creates a new instance 
	 */
	private static void spawnInstance(){
		String instanceType = null;
		String imageId = null; 
		int numInstances = 0;
		
		Scanner in = new Scanner(System.in);
		
		System.out.println("Name of key to use?");
		keyname = in.next();
		
		System.out.println("Instance type? (use d for default: t2.micro)");
		instanceType = in.next();
		
		if(instanceType.equals("d")){
			instanceType = INSTANCE_TYPE;
		}
		
		
		
		System.out.println("Image id? (use d for default: ami-b5a7ea85, Amazon Linux AMI)");
		imageId = in.next();
		
		if(imageId.equals("d")){
			imageId = IMAGE_ID; 
		}
		
		System.out.println("Number of instances?");
		numInstances = in.nextInt();

		in.close();
	
		RunInstancesRequest req = new RunInstancesRequest();
		
	    String group = GROUP;
	        
	    req.withImageId(imageId)
	       .withInstanceType(instanceType)
	       .withMinCount(numInstances)
	       .withMaxCount(numInstances)
	       .withKeyName(keyname)
	       .withSecurityGroups(group);
	        
	   RunInstancesResult reqRes = ec2.runInstances(req);
	   Reservation res = reqRes.getReservation();
	   System.out.println("Reservation number is " + res.getReservationId());
	   try {
   		System.out.println("Just a moment while the instance is spun up...");
			TimeUnit.SECONDS.sleep(10);
		} catch (InterruptedException e) {
			System.out.println("Instantiation failed!");
		}
   		String ip = listLiveInstances();
   		System.out.println("");
   		System.out.println("Connect with 'ssh -i " + keyname + ".pem ec2-user@" + ip +"'");
   		System.out.println("If connection fails, run with -l option and check status.");
   		System.exit(0);
	}
	
	
	/**
	 * The genKey method creates a pem file in the directory that the 
	 * program is executed in, and generates a key on amazon's servers.
	 * 
	 */
	private static void genKey(){
		System.out.println("Name to use for key?");
		Scanner in = new Scanner(System.in);
		keyname = in.next();
		in.close();
		
		CreateKeyPairRequest createKPReq = new CreateKeyPairRequest();
		createKPReq.withKeyName(keyname);
		CreateKeyPairResult resultPair = null;
		try{
			resultPair = ec2.createKeyPair(createKPReq);
		}catch(AmazonServiceException e){
			System.out.println("Key already exists!");
			System.exit(0);
		}

		KeyPair keyPair = new KeyPair();
		keyPair = resultPair.getKeyPair();
		String privateKey = keyPair.getKeyMaterial();
		FileOutputStream out = null;
		
		
		try{
			File f = new File(keyname + ".pem");
			out = new FileOutputStream(f);
			byte[] privateKeyByte = privateKey.getBytes();
			out.write(privateKeyByte);
			out.flush();
			out.close();
			
			
		}catch (IOException e){
			System.out.println("IO failed!");
		
		}finally{
			if(out != null){
				try {
					out.close();
				} catch (IOException e) {
					System.out.println("IO failed!");
				}
			}
		}
	
		System.out.println("Key generated: " + keyname + ".pem");
	}
	
	
	private static void delKey(){
		System.out.println("Name of key to delete?");
		Scanner in = new Scanner(System.in);
		String killkey = in.next();
		in.close();
		DeleteKeyPairRequest delkeyreq = new DeleteKeyPairRequest(killkey);
		try{
			ec2.deleteKeyPair(delkeyreq);
		}catch(AmazonServiceException e){
			System.out.println(e.getMessage());
		}
		System.out.println("Key " + killkey + " was deleted.");
	}
}
