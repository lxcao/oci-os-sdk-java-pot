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

        // if (args.length != 1) {
        //         throw new IllegalArgumentException(
        //                 "Unexpected number of arguments received. Object Location are required.");
        //     }

        //static paramaters
        String namespaceName = "ocichina001";
        System.out.println("Namespace: " + namespaceName);
        String bucketName = "oci.ezviz.singapore";
        System.out.println("Bucket: " + bucketName);
        String objectNamePrefix = "camera/001/";


                //OCI Instance Principal 
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

                //OCI Object Storage
                ObjectStorage client = new ObjectStorageClient(provider);
                    client.setRegion(Region.AP_SINGAPORE_1);
                // configure upload settings as desired
                UploadConfiguration uploadConfiguration =
                    UploadConfiguration.builder()
                        .allowMultipartUploads(true)
                        .allowParallelUploads(true)
                        .build();

        ExecutorService fixedPool = Executors.newFixedThreadPool(1);

        StopWatch stopWatchTotal = new StopWatch();
        stopWatchTotal.start();
        //multi file prepare
        //File sourceFile = new File("./assets/currybeef.mp4");
        for(int i = 0; i < 1; i++){
            //multi file prepare
            // File destinationFile = new File("./assets/currybeef-"+i+".mp4");
            // FileUtils.copyFile(sourceFile, destinationFile);
            //File bodyFile = new File("./assets/currybeef-"+i+".mp4");
            File bodyFile = new File("./assets/currybeef24M.mp4");
            // try inputstream
            // InputStream bodyStream = StreamUtils.toInputStream(bodyFile);
            // Long bodyStreamLength = bodyFile.length();
            fixedPool.submit(new Runnable() {
                @Override
                public void run() {
                    uploadObject(client, uploadConfiguration, objectNamePrefix, bucketName, namespaceName, bodyFile/*bodyStream, bodyStreamLength*/);
                }
            });
            System.out.println(i + " sumitted");
         };
        fixedPool.shutdown();
        fixedPool.awaitTermination(60, TimeUnit.SECONDS);
        System.out.println("Total upload consume: " + stopWatchTotal.getTime(TimeUnit.MILLISECONDS) + " ms.");
        stopWatchTotal.stop();

    }

    private static void uploadObject(ObjectStorage client, UploadConfiguration uploadConfiguration, String objectNamePrefix, String bucketName, String namespaceName, File bodyFile/*InputStream bodyStream, Long bodyStreamLength*/) {
        
        
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd"); 
        SimpleDateFormat timeFormatter = new SimpleDateFormat("HHmmss"); 

        final String ORGIN_STR = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder stringBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        Calendar cal = Calendar.getInstance();
        //System.out.println("The original Date: " + cal);

        for(int j = 0; j < 12; j++) {
            stringBuilder.append(ORGIN_STR.charAt(secureRandom.nextInt(ORGIN_STR.length())));
        }

        String objectName = objectNamePrefix + dateFormatter.format(cal.getTime()) + "/" + timeFormatter.format(cal.getTime()) + "-" + stringBuilder.toString();
        System.out.println("Object: " + objectName);
        Map<String, String> metadata = null;
        String contentType = null;
        String contentEncoding = null;
        String contentLanguage = null;

        StopWatch stopWatch = new StopWatch();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucketName(bucketName)
                        .namespaceName(namespaceName)
                        .objectName(objectName)
                        .contentType(contentType)
                        .contentLanguage(contentLanguage)
                        .contentEncoding(contentEncoding)
                        .opcMeta(metadata)
                        .build();

        UploadRequest uploadDetails = UploadRequest.builder(bodyFile).allowOverwrite(true).build(request);
        //UploadRequest uploadDetails = UploadRequest.builder(bodyStream,bodyStreamLength).allowOverwrite(true).build(request);
        

        stopWatch.start();
        // upload request and print result
        // if multi-part is used, and any part fails, the entire upload fails and will throw BmcException
        UploadResponse uploadResponse = uploadManager.upload(uploadDetails);
        System.out.println("upload consume: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms.");
        System.out.println(Thread.currentThread().getName() + " : etag : " + uploadResponse.getETag());
        //StreamUtils.closeQuietly(bodyStream);


        //download object
        // stopWatch.reset();
        // stopWatch.start();
        // // fetch the object just uploaded
        // GetObjectResponse downloadResponse =
        //         client.getObject(
        //                 GetObjectRequest.builder()
        //                         .namespaceName(namespaceName)
        //                         .bucketName(bucketName)
        //                         .objectName(objectName)
        //                         .build());

        // // stream contents should match the file uploaded
        // try (final InputStream fileStream = downloadResponse.getInputStream()) {
        //     // use fileStream
        //     File targetFile = new File("/home/opc/workspaces/oci-pot/tmp/"+objectName);
        //     FileUtils.copyInputStreamToFile(fileStream, targetFile);
        //     fileStream.close();
        // } // try-with-resources automatically closes fileStream
        // System.out.println("download consume: " + stopWatch.getTime(TimeUnit.MILLISECONDS) + " ms.");
        // // use the response's function to print the fetched object's metadata
        // System.out.println(downloadResponse);
        stopWatch.stop();
    }

 

}
