import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

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
        String key_name = Paths.get(file_path).getFileName().toString()+"10";

        System.out.format("Uploading %s to OCI bucket %s...\n", key_name, bucket_name);
        try {
            s3Client.putObject(bucket_name, key_name, new File(file_path));
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        }
        System.out.println("Done!");

        System.out.println("****************************listObjects****************************");
        ObjectListing result = s3Client.listObjects(bucket_name);
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        for (S3ObjectSummary os : objects) {
            System.out.println("* " + os.getKey());
        }

        System.out.println("****************************listObjectsV2****************************");
        ListObjectsV2Result resultv2 = s3Client.listObjectsV2(bucket_name);
        List<S3ObjectSummary> objectsv2 = resultv2.getObjectSummaries();
        for (S3ObjectSummary os : objectsv2) {
            System.out.println("* " + os.getKey());
        }
    }
}
