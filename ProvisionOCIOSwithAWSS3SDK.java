import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;

public class ProvisionOCIOSwithAWSS3SDK {
    public static void main(String[] args) throws Exception {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("3a8514e8914a012e82981da14e61d637e7de0927",
                "mpJEZrtEIktndD5i6QDNHSTIhB8/cEkktdA5mG9+YmQ=");

        AwsClientBuilder.EndpointConfiguration endpointConfigure = new AwsClientBuilder.EndpointConfiguration(
                "https://hktwlab.compat.objectstorage.ap-seoul-1.oraclecloud.com", "ap-seoul-1");

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withEndpointConfiguration(endpointConfigure)
                .disableChunkedEncoding()
                .enablePathStyleAccess()
                .build();

        System.out.print("****************************listBuckets****************************");
        List<Bucket> buckets = s3Client.listBuckets();
        System.out.println("Your OCI buckets are:");
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }

        System.out.println("****************************putObject****************************");
        String file_path = "/Users/caolingxin/Documents/workspaces/oci-projects/oci-os-s3-compatibility/assets/wholesale-trade-survey-mar-2022-quarter-csv.csv";
        String bucket_name = "bucket-20220722";
        String key_name = Paths.get(file_path).getFileName().toString() + UUID.randomUUID();

        System.out.format("Uploading %s to OCI bucket %s...\n", key_name, bucket_name);
        s3Client.putObject(bucket_name, key_name, new File(file_path));
        System.out.println("Done!");

        System.out.println("****************************listObjects****************************");
        ObjectListing result = s3Client.listObjects(bucket_name);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        String[] object_keys = new String[objects.size()];
        for (S3ObjectSummary os : objects) {
            System.out.println("* " + os.getKey());
            object_keys[objects.indexOf(os)] = os.getKey();
        }

        System.out.println("****************************listObjectsV2****************************");
        ListObjectsV2Result resultv2 = s3Client.listObjectsV2(bucket_name);
        List<S3ObjectSummary> objectsv2 = resultv2.getObjectSummaries();
        for (S3ObjectSummary os : objectsv2) {
            System.out.println("* " + os.getKey());
        }

        System.out.println("****************************getObject****************************");
        System.out.format("Downloading %s from OCI bucket %s...\n", key_name, bucket_name);
        S3Object o = s3Client.getObject(bucket_name, key_name);
        S3ObjectInputStream s3is = o.getObjectContent();
        FileOutputStream fos = new FileOutputStream(new File(key_name));
        byte[] read_buf = new byte[1024];
        int read_len = 0;
        while ((read_len = s3is.read(read_buf)) > 0) {
            fos.write(read_buf, 0, read_len);
        }
        s3is.close();
        fos.close();
        System.out.println("Done!");

        System.out.println("****************************getObjectAsString****************************");
        String oString = s3Client.getObjectAsString(bucket_name, key_name);
        System.out.format("length of downloaded object is %s\n", oString.length());
        System.out.println("Done!");

        System.out.println("****************************getObjectMetadata****************************");
        ObjectMetadata oMetadata = s3Client.getObjectMetadata(bucket_name, key_name);
        System.out.format("length of Content is %s\n", oMetadata.getContentLength());
        System.out.println("Done!");

        System.out.println("****************************copyObject****************************");
        String to_bucket = "bucket-20220720";
        System.out.format("Copying object %s from bucket %s to %s\n",
                key_name, bucket_name, to_bucket);
        s3Client.copyObject(bucket_name, key_name, to_bucket, key_name);
        System.out.println("Done!");

        System.out.println("****************************deleteObject****************************");
        System.out.format("Deleting object %s from OCI bucket: %s\n", key_name, to_bucket);
        s3Client.deleteObject(bucket_name, key_name);
        System.out.println("Done!");

        System.out.println("****************************listVersions****************************");
        VersionListing versions = s3Client.listVersions(bucket_name,"wholesale-trade-survey-mar-2022-quarter-csv");
        List<S3VersionSummary> versions_list = versions.getVersionSummaries();
        for (S3VersionSummary vs : versions_list) {
            System.out.println("* " + vs.getKey() + " " + vs.getVersionId());
        }

        System.out.println("****************************listNextBatchOfVersions****************************");
        VersionListing versions_next = s3Client.listNextBatchOfVersions(versions);
        List<S3VersionSummary> versions_list_next = versions_next.getVersionSummaries();
        for (S3VersionSummary vs : versions_list_next) {
            System.out.println("* " + vs.getKey() + " " + vs.getVersionId());
        }

        // System.out.println("****************************deleteObjects****************************");
        // System.out.println("Deleting objects from OCI bucket: " + bucket_name);
        // for (String k : object_keys) {
        //     System.out.println(" * " + k);
        // }
        // DeleteObjectsRequest dor = new DeleteObjectsRequest(bucket_name).withKeys(object_keys);
        // s3Client.deleteObjects(dor);
    }
}
