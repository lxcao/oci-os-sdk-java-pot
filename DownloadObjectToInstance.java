import java.net.ConnectException;
import java.net.SocketTimeoutException;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.model.BucketSummary;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest;
import com.oracle.bmc.objectstorage.requests.ListBucketsRequest.Builder;
import com.oracle.bmc.objectstorage.responses.ListBucketsResponse;

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

        Builder listBucketsBuilder = ListBucketsRequest.builder()
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

        // fetch the file from the object storage
        // String objectName = null;
        // GetObjectResponse getResponse = client.getObject(
        //         GetObjectRequest.builder()
        //                 .namespaceName(namespaceName)
        //                 .bucketName(bucketName)
        //                 .objectName(objectName)
        //                 .build());

        client.close();

    }

}
