import java.util.List;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;

public class ProvisionOCIOSwithAWSS3SDK {
    public static void main(String[] args) throws Exception {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials("3a8514e8914a012e82981da14e61d637e7de0927",
                "mpJEZrtEIktndD5i6QDNHSTIhB8/cEkktdA5mG9+YmQ=");

        AwsClientBuilder.EndpointConfiguration endpointConfigure = new AwsClientBuilder.EndpointConfiguration(
                "https://hktwlab.compat.objectstorage.ap-seoul-1.oraclecloud.com", "ap-seoul-1");

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withEndpointConfiguration(endpointConfigure)
                .build();

        System.out.print("****************************listBuckets****************************");
        List<Bucket> buckets = s3Client.listBuckets();
        System.out.println("Your OCI buckets are:");
        for (Bucket b : buckets) {
            System.out.println("* " + b.getName());
        }

    }
}
