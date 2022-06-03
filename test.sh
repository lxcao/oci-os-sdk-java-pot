#！ /bin/bash

java -version

# 设置环境变量
OCI_JAVA_SDK_LOCATION=$HOME/Documents/workspaces/oci-projects/oci-java-sdk-2.25.0-individual-modules
echo $OCI_JAVA_SDK_LOCATION

OCI_JAVA_SDK_FULL_JAR_LOCATION=$OCI_JAVA_SDK_LOCATION/lib/oci-java-sdk-full-2.25.0.jar
echo $OCI_JAVA_SDK_FULL_JAR_LOCATION

#compile and run ObjectStorageSyncExample
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* ObjectStorageSyncExample.java
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* ObjectStorageSyncExample

#compile and run UploadObjectExample
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectExample.java
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectExample $(echo "currybeefmovie"$(date +%Y%m%d%H%M)) ./assets/currybeef.mp4

#compile and run UploadObjectwithInstancePrincipals in the instance
scp -i ../credentials/ssh-key-oci-linux-cloud-dev.key UploadObjectwithInstancePrincipals.java  opc@138.2.76.103:/home/opc/workspaces/oci-pot/
javac -cp ../oci-sdk/oci-java-sdk/2.26.0/lib/oci-java-sdk-full-2.26.0.jar:../oci-sdk/oci-java-sdk/2.26.0/third-party/lib/\* UploadObjectwithInstancePrincipals.java
java -cp .:../oci-sdk/oci-java-sdk/2.26.0/lib/oci-java-sdk-full-2.26.0.jar:../oci-sdk/oci-java-sdk/2.26.0/third-party/lib/\* UploadObjectwithInstancePrincipals $(echo "currybeefmovie"$(date +%Y%m%d%H%M)) ./assets/currybeef.mp4

#compile and run PutLogsExample
#no exception when run, but no log in log service
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* PutLogsExample.java
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* PutLogsExample

#compile and run UploadObjectFromInstance
scp -i ../credentials/ssh-key-oci-linux-cloud-dev.key UploadObjectFromInstance.java  opc@138.2.76.103:/home/opc/workspaces/oci-pot/
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectFromInstance.java
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectFromInstance ./assets/currybeef.mp4

# sse-c upload by sdk, download by cli, you have to supply the optional parameter --encryption-key-file
oci os object get -ns ocichina001 -bn bucket-20220501-1555 --name currybeefmovie202206031033 --file a.mp4 --encryption-key-file ./sse-c.key --profile specialist2 --auth security_token