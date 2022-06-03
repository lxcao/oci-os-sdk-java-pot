import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import com.oracle.bmc.objectstorage.transfer.UploadConfiguration;
import com.oracle.bmc.objectstorage.transfer.UploadManager;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadRequest;
import com.oracle.bmc.objectstorage.transfer.UploadManager.UploadResponse;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import javax.crypto.KeyGenerator;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;

public class UploadObjectwithSSEC {
    public static void main(String[] args) throws Exception {

        String configurationFilePath = "~/.oci/config";
        String profile = "specialist2-4sdk";

        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Unexpected number of arguments received. Object Name and location are required.");
        }

        String namespaceName = "ocichina001";
        String bucketName = "bucket-20220501-1555";
        String objectName = args[0];
        Map<String, String> metadata = null;
        String contentType = null;
        String contentEncoding = null;
        String contentLanguage = null;
        String encryptionAlgorithm = "AES256";
        byte[] encryptionKey = createAES256Key();
        String base64EncryptionKey = createBase64EncodedString(encryptionKey);
        String base64EncrpytionSha = createBase64EncodedString(createSHA256Hash(encryptionKey));
        System.out.println("base64EncrpytionKey: " + base64EncryptionKey);
        System.out.println("base64EncrpytionSha: " + base64EncrpytionSha);
        File body = new File(args[1]);
        

        final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configurationFilePath, profile);

        final AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(configFile);

        ObjectStorage client = new ObjectStorageClient(provider);
        client.setRegion(Region.AP_SINGAPORE_1);

        // configure upload settings as desired
        UploadConfiguration uploadConfiguration = UploadConfiguration.builder()
                .allowMultipartUploads(true)
                .allowParallelUploads(true)
                .build();

        UploadManager uploadManager = new UploadManager(client, uploadConfiguration);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucketName(bucketName)
                .namespaceName(namespaceName)
                .objectName(objectName)
                .contentType(contentType)
                .contentLanguage(contentLanguage)
                .contentEncoding(contentEncoding)
                .opcMeta(metadata)
                .opcSseCustomerAlgorithm(encryptionAlgorithm)
                .opcSseCustomerKey(base64EncryptionKey)
                .opcSseCustomerKeySha256(base64EncrpytionSha)
                .build();

        UploadRequest uploadDetails = UploadRequest.builder(body).allowOverwrite(true).build(request);

        // upload request and print result
        // if multi-part is used, and any part fails, the entire upload fails and will
        // throw BmcException
        UploadResponse response = uploadManager.upload(uploadDetails);
        System.out.println(response);
        client.close();
    }

    // method to create AES256 key
    private static byte[] createAES256Key() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        return keyGenerator.generateKey().getEncoded();
    }

    // method to convert to Base64 encoded string
    private static String createBase64EncodedString(byte[] content) {
            //return Base64.encodeBase64String(b);
            return Base64.getEncoder().encodeToString(content);
    }

    // method to create SHA256 Hash
    private static byte[] createSHA256Hash(byte[] secret) throws DecoderException {
        //return DigestUtils.sha256Hex(secret);
        return DigestUtils.sha256(secret);
    }
}
