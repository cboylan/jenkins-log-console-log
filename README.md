# [AWS CloudWatch Logs Publisher](https://wiki.jenkins-ci.org/display/JENKINS/AWS+CloudWatch+Logs+Publisher+Plugin)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/aws-cloudwatch-logs-publisher-plugin/master)](https://ci.jenkins.io/job/Plugins/job/aws-cloudwatch-logs-publisher-plugin/job/master)

This Jenkins plugin allows you to send the console log of your builds to [Amazon CloudWatch Logs](https://aws.amazon.com/about-aws/whats-new/2014/07/10/introducing-amazon-cloudwatch-logs/ "Amazon CloudWatch Logs").

In your job configuration page, go to the section **Post-build Actions**,
 click the **Add post-build action** button and select the item **AWS CloudWatch Logs Publisher**.


In **Manage Jenkins > Configure System > AWS Logs Publisher**, you have to configure the **AWS Access Key Id** and **AWS Secret Key**.
If no account information is specified (**AWS Access Key Id** and **AWS Secret Key**) the plugin will use the default credential provider chain, which looks for credentials in this order:
- Environment variables
- Java system properties
- The default credential profiles file
- Amazon ECS container credentials
- Instance profile credentials

For more information [Working with AWS Credentials](http://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html "Working with AWS Credentials").

The AWS account requires *logs:CreateLogStream* and *logs:PutLogEvents* rights, as in the example below:
~~~~
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Action": [
                  "logs:CreateLogStream",
                  "logs:PutLogEvents"
              ],
              "Effect": "Allow",
              "Resource": "arn:aws:logs:::/jenkins/jobs:*"
          }
      ]
  }
~~~~


