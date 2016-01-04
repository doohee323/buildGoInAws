# RunAwsInst

It's for running AWS instance management (launch && use && terminate).
You can define the condition for AWS instance such as price, region, instance type, and instance number using AMI which you made.
Once the instances are launched, you can use the instances through predefined scripts in commands.json.
After using the instances, they'll be terminated automatically.

# Required
```
-. AMI: instance image which you want to make  
-. install aws cli
-. make AWS_ACCESS_KEY / AWS_SECRET_KEY
-. make AWS keypair / security group
-. make pem file for AWS
```

# How to run
```
	cd ~/runAwsInst
	mvn clean package assembly:single -DskipTests=true
	cd ~/runAwsInst/target
	java -jar runAwsInst-0.0.1-SNAPSHOT-jar-with-dependencies.jar ls_tmp
	
	# ls_tmp: commands id
```

# Configuration  
```
(/runAwsInst/src/main/resources/application.property)

accesskey=
secretkey=
cmd_file=commands.json	-> predefined scripts which you want to run in the instances
```

# Example for commands.json  
```
(/runAwsInst/src/main/resources/commands.json)

{
	"awsInst": 
	[
		{
			"ls_tmp": 		-> commands id
			{
				"target": 
				[
					{
						"keypair": "golang2",
						"security_group": "golang2",
						"pem_file": "/Users/dhong/.ssh/golang2.pem",
						"ami_id": "ami-6ca2c80c",
						"instance_type": "spot",		-> instance type (spot / common)
						"instance_num": 1,				-> the instance number which you want to launch
						"inst_spec": "m3.medium",
						"cli_args": "--price 0.3",
						"region": "us-west-1",			-> check with ec2-describe-regions
						"username": "ubuntu"			-> username for ssh access (ubuntu or ec2_user)  
					},

					{
						"keypair": "golang1",
						"security_group": "golang1",
						"pem_file": "/Users/dhong/.ssh/golang1.pem",
						"ami_id": "ami-10cbd371",
						"instance_type": "spot",
						"instance_num": 1,
						"inst_spec": "m3.medium",
						"cli_args": "--price 0.3",
						"region": "us-west-2",
						"username": "ubuntu"
					}
				],

				"commands": 
				[
					"cd /tmp",
					"ls -al"
				]
			},

			"ls_tmp2": 
			{
				"target": 
				[
					{
						"keypair": "golang2",
						"security_group": "golang2",
						"pem_file": "/Users/dhong/.ssh/golang2.pem",
						"ami_id": "ami-6ca2c80c",
						"instance_type": "common",
						"instance_num": 2,
						"inst_spec": "m1.medium",
						"cli_args": "",
						"region": "us-west-1",
						"username": "ubuntu"
					}
				],

				"commands": 
				[
					"cd /tmp",
					"ls -al"
				]
			}
		}
	]
}
```

# Install AWS CLI
```
sudo mkdir /usr/local/ec2
sudo wget http://s3.amazonaws.com/ec2-downloads/ec2-api-tools.zip
sudo unzip ec2-api-tools.zip -d /usr/local/ec2

export JAVA_HOME=$(/usr/libexec/java_home)
export EC2_HOME=/usr/local/ec2/ec2-api-tools-1.7.5.1
export AWS_ACCESS_KEY=
export AWS_SECRET_KEY=
export AWS_DEFAULT_REGION=us-west-1

export PATH=.:/bin:/usr/bin:/usr/sbin:$EC2_HOME/bin 

```
# Make ACCESS_KEY / SECRET_KEY
```
# create access-key
aws iam create-access-key --user-name golang2
# AWS_ACCESS_KEY, AWS_SECRET_KEY
# "SecretAccessKey": "", 
# "AccessKeyId": ""

export AWS_ACCESS_KEY=
export AWS_SECRET_KEY=
export PATH=.:$PATH:$EC2_HOME/bin 
```

# Create user
```
#aws iam get-user --user-name golang2
aws iam create-user --user-name golang2
#aws iam delete-user --user-name golang2

# create key-pair
aws ec2 describe-key-pairs --key-name golang2
aws ec2 create-key-pair --key-name golang2 --query 'KeyMaterial' --output text > golang2.pem
#aws ec2 delete-key-pair --key-name golang2

# create group and assign user to group
aws iam create-group --group-name golang2
aws iam add-user-to-group --user-name golang2 --group-name golang2 
#aws iam remove-user-from-group --user-name golang2 --group-name golang2 
#aws iam delete-group --group-name golang2

aws ec2 create-security-group --group-name golang2 --description "golang2 group"
aws ec2 authorize-security-group-ingress --group-name golang2 --protocol tcp --port 22 --cidr 0.0.0.0/0
#aws ec2 delete-security-group --group-name golang2
```

# Check regions
```
ec2-describe-regions
ec2-describe-availability-zones --region us-west-1
```


