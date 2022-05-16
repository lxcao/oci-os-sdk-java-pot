import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.model.ListObjects;
import com.oracle.bmc.objectstorage.model.ObjectSummary;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListObjectsRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;
import com.oracle.bmc.objectstorage.responses.ListObjectsResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest;

import org.apache.commons.lang3.time.StopWatch;

public class DownloadObjectToInstance {

    public static void main(String[] args) throws Exception {

        // fixed file and location for testing
        // if (args.length != 1) {
        // throw new IllegalArgumentException(
        // "Unexpected number of arguments received. Object Location are required.");
        // }

        // static paramaters
        String namespaceName = "ocichina001";
        System.out.println("Namespace: " + namespaceName);
        String compartmentId = "ocid1.compartment.oc1..aaaaaaaayjcsmu5ii7ac3kncp5qlbsslaj7irtc3mo4oco22w7ucsiq3atmq";
        System.out.println("Compartment Id: " + compartmentId);
        String bucketName = "oci.ezviz.singapore";
        System.out.println("Bucket: " + bucketName);
        String objectNamePrefix = "camera/001/";

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");

        // Catch cal
        Calendar cal = Calendar.getInstance();

        // OCI Instance Principal provider
        final InstancePrincipalsAuthenticationDetailsProvider provider;
        try {
            provider = InstancePrincipalsAuthenticationDetailsProvider.builder().build();
        } catch (Exception e) {
            if (e.getCause() instanceof SocketTimeoutException
                    || e.getCause() instanceof ConnectException) {
                System.out.println(
                        "This sample only works when running on an OCI instance. Are you sure you are running on an OCI instance? For more info see: https://docs.cloud.oracle.com/Content/Identity/Tasks/callingservicesfrominstances.htm");
                return;
            }
            throw e;
        }

        // Set client
        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(Region.AP_SINGAPORE_1);

        // create log file and upload to object storage
        UploadConfiguration uploadConfiguration = UploadConfiguration.builder()
                .allowMultipartUploads(false)
                .allowParallelUploads(true)
                .build();
        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);
        String logRecord = dateFormatter.format(cal.getTime()) + "-" + timeFormatter.format(cal.getTime());
        File logFile = new File("./logs/" + logRecord + ".log");
        FileUtils.writeStringToFile(logFile, cal.toString(), Charset.defaultCharset());
        String logName = objectNamePrefix + "log/" + logRecord;
        UploadRequest uploadLogDetails = createUploadRequest(bucketName, namespaceName, logName, logFile);
        Long logFileConsumeTime = uploadObject(uploadManager, uploadLogDetails);
        System.out.println("logFileUpdatedConsumeTime: " + logFileConsumeTime + " ms");

        // list buckets
        com.oracle.bmc.objectstorage.requests.ListBucketsRequest.Builder listBucketsBuilder = ListBucketsRequest
                .builder()
                .namespaceName(namespaceName)
                .compartmentId(compartmentId);

        String nextToken = null;
        do {
            listBucketsBuilder.page(nextToken);
            ListBucketsResponse listBucketsResponse = client.listBuckets(listBucketsBuilder.build());
            for (BucketSummary bucket : listBucketsResponse.getItems()) {
                System.out.println("Found bucket: " + bucket.getName());
            }
            nextToken = listBucketsResponse.getOpcNextPage();
        } while (nextToken != null);

        // get object lists in bucket
        com.oracle.bmc.objectstorage.requests.ListObjectsRequest.Builder listObjectsBuilder = ListObjectsRequest
                .builder()
                .namespaceName(namespaceName)
                .bucketName(bucketName);
        ListObjectsResponse listObjectsResponse = client.listObjects(listObjectsBuilder.build());
        ListObjects objectsList = listObjectsResponse.getListObjects();
        List<ObjectSummary> objectsSummaryList = objectsList.getObjects();
        int totalObjectsNumber = objectsSummaryList.size();
        System.out.println("The number of objects in " + bucketName + ": " + totalObjectsNumber);

        // prepare 88 files to be downloaded
        List<String> objectsNameList = objectsSummaryList.subList(0, 88)
                .stream().map(x -> x.getName()).collect(Collectors.toList());
        System.out.println("The number of objects to be downloaded: " + objectsNameList.size());

        // create the first file download request from object List
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .namespaceName(namespaceName)
                .bucketName(bucketName)
                .objectName(objectsNameList.get(0))
                .build();

        // download ONE file
        Long downloadConsumeTime = downloadObject(client, getObjectRequest);
        System.out.println("Download ONE file consume time: " + downloadConsumeTime + " ms");

        client.close();
    }

    // method to invoke download object
    private static Long downloadObject(ObjectStorage client, GetObjectRequest getObjectRequest) throws IOException {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start();

        GetObjectResponse getResponse = client.getObject(getObjectRequest);
        // get object stream
        InputStream downloadInputStream = getResponse.getInputStream();
        // save to file
        File downloadFile = new File("./downloads/" + getObjectRequest.getObjectName().replaceAll("/", "-") + ".mp4");
        FileUtils.copyInputStreamToFile(downloadInputStream, downloadFile);

        Long consumeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);

        return consumeTime;
    }

    // method to invoke upload object
    private static Long uploadObject(UploadManager uploadManager, UploadRequest uploadDetails) {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        // upload request and print result
        // UploadResponse uploadResponse = uploadManager.upload(uploadDetails);
        uploadManager.upload(uploadDetails);
        Long consumeTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        // System.out.println("upload consume: " + consumeTime + " ms.");
        // System.out.println(Thread.currentThread().getName() + " : etag : " +
        // uploadResponse.getETag());

        return consumeTime;
    }

    // method to create the UploadRequest
    private static UploadRequest createUploadRequest(String bucketName, String namespaceName, String objectName,
            File bodyFile) {
        Map<String, String> metadata = null;
        String contentType = null;
        String contentEncoding = null;
        String contentLanguage = null;

        // create request
        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(bucketName)
                .namespaceName(namespaceName)
                .objectName(objectName)
                .contentType(contentType)
                .contentLanguage(contentLanguage)
                .contentEncoding(contentEncoding)
                .opcMeta(metadata)
                .build();

        // create uploadRequest
        UploadRequest uploadDetails = UploadRequest.builder(bodyFile).allowOverwrite(true).build(request);
        // test stream method
        // UploadRequest uploadDetails =
        // UploadRequest.builder(bodyStream,bodyStreamLength).allowOverwrite(true).build(request);
        return uploadDetails;
    }

}
