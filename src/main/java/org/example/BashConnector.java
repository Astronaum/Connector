package org.example;

import java.util.*;
import java.util.logging.Filter;

import java.io.*;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.operations.APIOperation;
import org.identityconnectors.framework.api.operations.ResolveUsernameApiOp;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

@ConnectorClass(configurationClass = BashConfiguration.class, displayNameKey = "bash.connector.display")
public class BashConnector implements Connector, CreateOp, DeleteOp, TestOp, SearchOp<Filter>, SchemaOp {

    private BashConfiguration configuration;

    @Override
    public Configuration getConfiguration() {
        return null;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = (BashConfiguration) configuration;
    }

    @Override
    public void dispose() {
        // Cleanup if needed
    }

    @Override
    public void test() {
        executeScript("echo 'Connector Test Successful'");
    }

    @Override
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
        return null;
    }

    @Override
    public void executeQuery(ObjectClass objectClass, Filter filter, ResultsHandler handler, OperationOptions options) {
        String output = executeScript("getUsers");
        for (String user : output.split("\n")) {
            ConnectorObject obj = new ConnectorObjectBuilder()
                    .setUid(user.trim())
                    .setName(user.trim())
                    .build();
            handler.handle(obj);
        }
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions options) {
        String username = AttributeUtil.getAsStringValue(AttributeUtil.find(Name.NAME, attributes));
        executeScript("createUser", username);
        return new Uid(username);
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        executeScript("deleteUser", uid.getUidValue());
    }

    public String executeScript(String operation, String... args) {
        try {
            String script = configuration.getScriptContent();
            File scriptFile = (script != null && !script.isEmpty()) ? writeScriptToFile(script) : new File(configuration.getScriptPath());

            String shell = configuration.getShell(); // Get shell from config
            List<String> command = new ArrayList<>();

            if (shell.contains("cmd.exe")) {
                command.add("cmd.exe");
                command.add("/c");
            } else {
                command.add(shell); // Use provided shell (e.g., /bin/bash, /usr/bin/sh)
            }

            command.add(scriptFile.getAbsolutePath());
            command.add(operation);
            command.addAll(List.of(args));

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();

            if (script != null) scriptFile.delete(); // Cleanup temp script
            return output.toString();
        } catch (Exception e) {
            throw new RuntimeException("Script execution failed", e);
        }
    }

    private File writeScriptToFile(String scriptContent) throws IOException {
        String extension = configuration.getShell().contains("cmd.exe") ? ".bat" : ".sh";
        File tempScript = File.createTempFile("midpoint_script_", extension);
        tempScript.setExecutable(true);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempScript))) {
            writer.write(scriptContent);
        }
        return tempScript;
    }

    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(BashConnector.class);

        // Define object class for user accounts
        ObjectClassInfoBuilder objectClassBuilder = new ObjectClassInfoBuilder();
        objectClassBuilder.setType(ObjectClass.ACCOUNT_NAME);

        // Define required and optional attributes for provisioning
        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(Name.NAME).setRequired(true).build());
        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("email").setRequired(false).build());
        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("roles").setMultiValued(true).build());

        schemaBuilder.defineObjectClass(objectClassBuilder.build());

        return schemaBuilder.build();
    }

}
