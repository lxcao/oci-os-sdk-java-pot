import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.loggingingestion.LoggingClient;
import com.oracle.bmc.loggingingestion.model.*;
import com.oracle.bmc.loggingingestion.requests.*;
import com.oracle.bmc.loggingingestion.responses.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.Arrays;


public class PutLogsExample {
    public static void main(String[] args) throws Exception {

        String configurationFilePath = "~/.oci/config";
        String profile = "specialist2-4sdk";

        // Configuring the AuthenticationDetailsProvider. It's assuming there is a default OCI config file
        // "~/.oci/config", and a profile in that config with the name "DEFAULT". Make changes to the following
        // line if needed and use ConfigFileReader.parse(configurationFilePath, profile);

        //final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parseDefault();
        final ConfigFileReader.ConfigFile configFile = ConfigFileReader.parse(configurationFilePath, profile);

        final AuthenticationDetailsProvider provider =
                new ConfigFileAuthenticationDetailsProvider(configFile);
        /* Create a service client */
        LoggingClient client = new LoggingClient(provider);

        String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat formatter = new SimpleDateFormat(DEFAULT_PATTERN); 

        /* Create a request and dependent object(s). */
	PutLogsDetails putLogsDetails = PutLogsDetails.builder()
		.specversion("1.0")
		.logEntryBatches(new ArrayList<>(Arrays.asList(LogEntryBatch.builder()
				.entries(new ArrayList<>(Arrays.asList(LogEntry.builder()
						.data("EXAMPLE-data-Value")
						.id("ocid1.test.oc1..sha111.EXAMPLE-id-Value")
						.time(formatter.parse("2022-05-01 12:57:10"))
                        .build())))
				.source("EXAMPLE-source-Value")
				.type("EXAMPLE-type-Value")
				.subject("EXAMPLE-subject-Value")
				.defaultlogentrytime(formatter.parse("2022-05-01 12:57:11"))
                .build()))).build();

	PutLogsRequest putLogsRequest = PutLogsRequest.builder()
		.logId("ocid1.log.oc1.ap-singapore-1.amaaaaaacuco5yqaerilsfjkhhrtuob5casv3n3tkvt4ffkwf77badicd37q")
		.putLogsDetails(putLogsDetails)
		.timestampOpcAgentProcessing(formatter.parse("2022-05-01 12:57:12"))
		.opcAgentVersion("1.0")
		.opcRequestId("XVORBHLR8W6FWF3GCAEQSHA111").build();

        /* Send request to the Client */
        PutLogsResponse response = client.putLogs(putLogsRequest);
    }

    
}