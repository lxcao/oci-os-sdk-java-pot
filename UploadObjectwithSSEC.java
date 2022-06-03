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
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;
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
        String encryptionKey = createAES256Key();
        String encrpytionSha = createAES256KeySha256Hex(encryptionKey);
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
                .opcSseCustomerKey(encryptionKey)
                .opcSseCustomerKeySha256(encrpytionSha)
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
    private static String createAES256Key() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            // AES 256
            kg.init(256);
            SecretKey sk = kg.generateKey();
            byte[] b = sk.getEncoded();
            return Base64.encodeBase64String(b);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("no such algorithm");
        }
    }

    // method to create AES256KeySHA256Hex
    private static String createAES256KeySha256Hex(String secret) {
        return DigestUtils.sha256Hex(secret).toUpperCase();
    }
}
