package org.example;

import java.util.*;
import java.io.*;

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.*;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.common.security.GuardedString;

@ConnectorClass(configurationClass = BashConfiguration.class, displayNameKey = "bash.connector.display")
public class BashConnector implements Connector, CreateOp, DeleteOp, UpdateOp, TestOp, SchemaOp {

    private BashConfiguration configuration;

    @Override
    public Configuration getConfiguration() {
        return configuration;
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
    public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions options) {
        String username = AttributeUtil.getAsStringValue(AttributeUtil.find(Name.NAME, attributes));
        String email = AttributeUtil.getAsStringValue(AttributeUtil.find("email", attributes));

        executeScript("createUser", username, email);
        return new Uid(username);
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        executeScript("deleteUser", uid.getUidValue());
    }

    @Override
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions options) {
        String email = AttributeUtil.getAsStringValue(AttributeUtil.find("email", attributes));

        executeScript("updateUser", uid.getUidValue(), email);
        return uid;
    }

    public String executeScript(String operation, String... args) {
        try {
            String script = configuration.getScriptContent();
            File scriptFile = (script != null && !script.isEmpty()) ? writeScriptToFile(script) : new File(configuration.getScriptPath());

            String shell = configuration.getShell();
            List<String> command = new ArrayList<>();

            if (shell.contains("cmd.exe")) {
                command.add("cmd.exe");
                command.add("/c");
            } else {
                command.add(shell);
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

            if (script != null) scriptFile.delete();
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

    @Override
    public Schema schema() {
        SchemaBuilder schemaBuilder = new SchemaBuilder(BashConnector.class);

        ObjectClassInfoBuilder objectClassBuilder = new ObjectClassInfoBuilder();
        objectClassBuilder.setType(ObjectClass.ACCOUNT_NAME);

        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(Uid.NAME).setRequired(true).build());
        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define(Name.NAME).setRequired(true).build());
        objectClassBuilder.addAttributeInfo(AttributeInfoBuilder.define("email").setRequired(false).build());

        schemaBuilder.defineObjectClass(objectClassBuilder.build());
        return schemaBuilder.build();
    }
}