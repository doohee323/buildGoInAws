# buildGoInAws

# required: 
# make ami: ami-6ca2c80c

# install aws cli
sudo mkdir /usr/local/ec2
sudo wget http://s3.amazonaws.com/ec2-downloads/ec2-api-tools.zip
sudo unzip ec2-api-tools.zip -d /usr/local/ec2

# setting aws cli
export JAVA_HOME=$(/usr/libexec/java_home)
export EC2_HOME=/usr/local/ec2/ec2-api-tools-1.7.5.1

# golang2
export AWS_ACCESS_KEY=
export AWS_SECRET_KEY=
export PATH=.:$PATH:/data3/play-2.2.0:/usr/local/mysql/bin:$EC2_HOME/bin 

# aws ec2 describe-regions
aws configure

# create user
#aws iam get-user --user-name golang2
aws iam create-user --user-name golang2
#aws iam delete-user --user-name golang2

# create access-key
aws iam create-access-key --user-name golang2
# AWS_ACCESS_KEY, AWS_SECRET_KEY
# "SecretAccessKey": "", 
# "AccessKeyId": ""

# create group and assign user to group
aws iam create-group --group-name golang2
aws iam add-user-to-group --user-name golang2 --group-name golang2 
#aws iam remove-user-from-group --user-name golang2 --group-name golang2 
#aws iam delete-group --group-name golang2

aws ec2 create-security-group --group-name golang2 --description "golang2 group"
aws ec2 authorize-security-group-ingress --group-name golang2 --protocol tcp --port 22 --cidr 0.0.0.0/0
#aws ec2 delete-security-group --group-name golang2

# create key-pair
aws ec2 describe-key-pairs --key-name golang2
aws ec2 create-key-pair --key-name golang2 --query 'KeyMaterial' --output text > golang2.pem
#aws ec2 delete-key-pair --key-name golang2

#ec2-describe-regions
#ec2-describe-availability-zones --region us-west-1

cd /Users/mac/Documents/workspace/etc/build_go_in_aws/target/
java -jar build_go_in_aws-0.0.1-SNAPSHOT-jar-with-dependencies.jar


