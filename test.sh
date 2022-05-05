#！ /bin/bash

java -version

# 设置环境变量
OCI_JAVA_SDK_LOCATION=$HOME/Documents/workspaces/oci-projects/oci-java-sdk-2.25.0-individual-modules
echo $OCI_JAVA_SDK_LOCATION

OCI_JAVA_SDK_FULL_JAR_LOCATION=$OCI_JAVA_SDK_LOCATION/lib/oci-java-sdk-full-2.25.0.jar
echo $OCI_JAVA_SDK_FULL_JAR_LOCATION

#compile ObjectStorageSyncExample
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* ObjectStorageSyncExample.java
#run ObjectStorageSyncExample
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* ObjectStorageSyncExample

#compile UploadObjectExample
javac -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectExample.java
#run UploadObjectExample
java -cp .:$OCI_JAVA_SDK_FULL_JAR_LOCATION:$OCI_JAVA_SDK_LOCATION/third-party/lib/* UploadObjectExample $(echo "currybeefmovie"$(date +%Y%m%d%H%M)) ./assets/currybeef.mp4
