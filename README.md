# AWS CloudWatch Logs Publisher

This Jenkins plugin allows you to send the console log of your builds to [Amazon CloudWatch Logs](https://aws.amazon.com/about-aws/whats-new/2014/07/10/introducing-amazon-cloudwatch-logs/ Amazon CloudWatch Logs).

In your job configuration page, go to the section **Post-build Actions**,
 click the **Add post-build action** button and select the item **AWS CloudWatch Logs Publisher**.

In **Manage Jenkins > Configure System > AWS Logs Publisher**, you have to configure the **AWS Access Key Id** and **AWS Secret Key** of an account with *logs:CreateLogStream* and *logs:PutLogEvents* rights, as in the example below:

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
