import java.io.File;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadResponse;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.util.StreamUtils;

public class UploadObjectFromInstance {

    public static void main(String[] args) throws Exception {

        // fixed file and location for testing
        // if (args.length != 1) {
        // throw new IllegalArgumentException(
        // "Unexpected number of arguments received. Object Location are required.");
        // }

        // static paramaters
        String namespaceName = "ocichina001";
        System.out.println("Namespace: " + namespaceName);
        String bucketName = "oci.ezviz.singapore";
        System.out.println("Bucket: " + bucketName);
        String objectNamePrefix = "camera/001/";

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss");

        final String ORGIN_STR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom secureRandom = new SecureRandom();

        //Catch cal
        Calendar cal = Calendar.getInstance();
        // System.out.println("The original Date: " + cal);

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

        // Set client, configuration and upload manager
        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(Region.AP_SINGAPORE_1);

        //file size is only 780KB, no need multipart
        UploadConfiguration uploadConfiguration = UploadConfiguration.builder()
                .allowMultipartUploads(false)
                .allowParallelUploads(true)
                .build();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);

        //Create thread pool
        ExecutorService fixedPool = Executors.newFixedThreadPool(10);

        StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        // multi file prepare
        // File sourceFile = new File("./assets/currybeef.mp4");

        //batch for 160 files
        for (int i = 0; i < 160; i++) {
            // multi file prepare
            // File destinationFile = new File("./assets/currybeef-"+i+".mp4");
            // FileUtils.copyFile(sourceFile, destinationFile);

            File bodyFile = new File("./assets/currybeef-" + i + ".mp4");

            // try one large file
            // File bodyFile = new File("./assets/currybeef24M.mp4");
            // try inputstream
            // InputStream bodyStream = StreamUtils.toInputStream(bodyFile);
            // Long bodyStreamLength = bodyFile.length();

            //create random string which length is 12
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = 0; j < 12; j++) {
                stringBuilder.append(ORGIN_STR.charAt(secureRandom.nextInt(ORGIN_STR.length())));
            }

            //set object properties
            String objectName = objectNamePrefix + dateFormatter.format(cal.getTime()) + "/"
                    + timeFormatter.format(cal.getTime()) + "-" + stringBuilder.toString();
            //System.out.println("Object: " + objectName);
            Map<String, String> metadata = null;
            String contentType = null;
            String contentEncoding = null;
            String contentLanguage = null;

            //create request
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucketName(bucketName)
                    .namespaceName(namespaceName)
                    .objectName(objectName)
                    .contentType(contentType)
                    .contentLanguage(contentLanguage)
                    .contentEncoding(contentEncoding)
                    .opcMeta(metadata)
                    .build();

            //create uploadRequest
            UploadRequest uploadDetails = UploadRequest.builder(bodyFile).allowOverwrite(true).build(request);
            //test stream method
            // UploadRequest uploadDetails = UploadRequest.builder(bodyStream,bodyStreamLength).allowOverwrite(true).build(request);

            //launch thread
            fixedPool.submit(new Runnable() {
                @Override
                public void run() {
                    uploadObject(uploadManager, uploadDetails);
                }
            });
            System.out.println(i + " sumitted");
        };

        //close thread pool
        fixedPool.shutdown();
        fixedPool.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println("Total upload consume: " + stopWatchTotal.getTime(TimeUnit.MILLISECONDS) + " ms.");
        stopWatchTotal.stop();

    }

    // method to invoke upload object
    private static void uploadObject(UploadManager uploadManager, UploadRequest uploadDetails) {

        StopWatch stopWatch = new StopWatch();

        stopWatch.start();
        // upload request and print result
        UploadResponse uploadResponse = uploadManager.upload(uploadDetails);
        System.out.println("upload consume: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms.");
        System.out.println(Thread.currentThread().getName() + " : etag : " + uploadResponse.getETag());

        stopWatch.stop();
    }

}
