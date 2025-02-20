package org.example;

import org.identityconnectors.framework.common.objects.OperationOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BashConnectorTest {

    @Test
    void testScriptExecution() {
        BashConnector connector = new BashConnector();
        BashConfiguration config = new BashConfiguration();

        //For bash
        config.setScriptPath("test.sh");
        config.setShell("C:\\Program Files\\Git\\bin\\bash.exe");

        // For Windows
        /*config.setScriptPath("test.bat");
        config.setShell("cmd.exe");*/

        connector.init(config);

        // Execute a test operation
        String username = "johndoe";
        String email = "johndoe@example.com";
        String firstName = "John";
        String lastName = "Doe";

        // Pass the arguments to the script method
        String output = connector.executeScript("createUser", username, email, firstName, lastName);

        // Debugging output
        System.out.println(output); // Print the output to the console for inspection

        // Assertions to validate the output
        Assertions.assertNotNull(output);
        assertTrue(output.contains("User created successfully"));

    }
}

