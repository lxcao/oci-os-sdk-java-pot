import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.security.SecureRandom;

import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.oracle.bmc.Region;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;

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

        // Set client, configuration and upload manager
        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(Region.AP_SINGAPORE_1);

        // file size is only 780KB, no need multipart
        UploadConfiguration uploadConfiguration = UploadConfiguration.builder()
                .allowMultipartUploads(false)
                .allowParallelUploads(true)
                .build();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);

        // create log file and upload to object storage
        String logRecord = dateFormatter.format(cal.getTime()) + "-" + timeFormatter.format(cal.getTime());
        File logFile = new File("./logs/" + logRecord + ".log");
        FileUtils.writeStringToFile(logFile, cal.toString(), Charset.defaultCharset());
        String logName = objectNamePrefix + "log/" + logRecord;
        UploadRequest uploadLogDetails = createUploadRequest(bucketName, namespaceName, logName, logFile);
        Long logFileConsumeTime = uploadObject(uploadManager, uploadLogDetails);
        System.out.println("logFileConsumeTime: " + logFileConsumeTime + " ms");

        // Create thread pool
        int threadNumber = 100;
        ExecutorService fixedPool = Executors.newFixedThreadPool(threadNumber);

        StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        // multi file prepare
        // File sourceFile = new File("./assets/currybeef.mp4");

        // static list
        List<Future<Long>> cTimeList = Collections.synchronizedList(new ArrayList<Future<Long>>());

        // batch for 160 files
        int filesInBatch = 160;
        for (int i = 0; i < filesInBatch; i++) {
            // multi file prepare
            // File destinationFile = new File("./assets/currybeef-"+i+".mp4");
            // FileUtils.copyFile(sourceFile, destinationFile);

            File bodyFile = new File("./assets/currybeef-" + i + ".mp4");

            // try one large file
            // File bodyFile = new File("./assets/currybeef24M.mp4");
            // try inputstream
            // InputStream bodyStream = StreamUtils.toInputStream(bodyFile);
            // Long bodyStreamLength = bodyFile.length();

            // create random string which length is 12
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = 0; j < 12; j++) {
                stringBuilder.append(ORGIN_STR.charAt(secureRandom.nextInt(ORGIN_STR.length())));
            }

            // set object properties
            String objectName = objectNamePrefix + dateFormatter.format(cal.getTime()) + "/"
                    + timeFormatter.format(cal.getTime()) + "-" + stringBuilder.toString();
            // System.out.println("Object: " + objectName);

            UploadRequest uploadDetails = createUploadRequest(bucketName, namespaceName, objectName, bodyFile);

            // launch thread without future return
            // fixedPool.submit(new Runnable() {
            // @Override
            // public void run() {
            // uploadObject(uploadManager, uploadDetails);
            // }
            // });

            // launch thread with consume time
            Callable<Long> callableTask = () -> {
                return uploadObject(uploadManager, uploadDetails);
            };
            Future<Long> cTime = fixedPool.submit(callableTask);

            // add consume time into list
            cTimeList.add(cTime);
        }
        ;

        System.out.println("Uploading " + filesInBatch + " files with " + threadNumber + " threads");
        // close thread pool
        fixedPool.shutdown();
        fixedPool.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println("Total upload consume: " + stopWatchTotal.getTime(TimeUnit.MILLISECONDS) + " ms");
        stopWatchTotal.stop();

        // statistics
        List<Long> cTimeListLong = cTimeList.stream().map(x -> {
            try {
                return x.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        LongSummaryStatistics stats = cTimeListLong.stream().mapToLong(x -> x).summaryStatistics();
        System.out.println("Number of object uploaded: " + stats.getCount());
        System.out.println("Average upload consume time: " + stats.getAverage() + " ms");
        System.out.println("Max upload consume time: " + stats.getMax() + " ms");
        System.out.println("Min upload consume time: " + stats.getMin() + " ms");

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
