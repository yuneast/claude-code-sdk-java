package com.anthropic.claudecode.transport;

import com.anthropic.claudecode.ClaudeCodeOptions;
import com.anthropic.claudecode.exceptions.CLIConnectionError;
import com.anthropic.claudecode.exceptions.CLIJSONDecodeError;
import com.anthropic.claudecode.exceptions.CLINotFoundError;
import com.anthropic.claudecode.exceptions.ClaudeSDKException;
import com.anthropic.claudecode.exceptions.ProcessError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transport implementation backed by the Claude Code CLI subprocess.
 */
public class SubprocessCLITransport implements Transport {
    private static final Logger LOGGER = Logger.getLogger(SubprocessCLITransport.class.getName());
    private static final int MAX_BUFFER_SIZE = 1024 * 1024;

    private final boolean streaming;
    private final String prompt;
    private final ClaudeCodeOptions options;
    private final String cliPath;
    private final ObjectMapper mapper = new ObjectMapper();

    private Process process;
    private BufferedWriter stdin;
    private BufferedReader stdout;
    private Transport.MessageHandler handler;
    private final AtomicBoolean ready = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "claude-cli-reader");
        thread.setDaemon(true);
        return thread;
    });
    private Future<?> readerTask;
    private volatile ClaudeSDKException exitError;

    public SubprocessCLITransport(boolean streaming, String prompt, ClaudeCodeOptions options)
            throws ClaudeSDKException {
        this.streaming = streaming;
        this.prompt = prompt;
        this.options = options;
        this.cliPath = locateCli();
    }

    private String locateCli() throws CLINotFoundError {
        String which = System.getenv().getOrDefault("CLAUDE_CODE_CLI_PATH", "");
        if (!which.isBlank()) {
            return which;
        }
        String systemWhich = findExecutableOnPath("claude");
        if (systemWhich != null) {
            return systemWhich;
        }
        List<Path> locations = List.of(
                Path.of(System.getProperty("user.home"), ".npm-global/bin/claude"),
                Path.of("/usr/local/bin/claude"),
                Path.of(System.getProperty("user.home"), ".local/bin/claude"),
                Path.of(System.getProperty("user.home"), "node_modules/.bin/claude"),
                Path.of(System.getProperty("user.home"), ".yarn/bin/claude"));
        for (Path path : locations) {
            if (path.toFile().isFile()) {
                return path.toAbsolutePath().toString();
            }
        }
        boolean nodeInstalled = findExecutableOnPath("node") != null;
        if (!nodeInstalled) {
            throw new CLINotFoundError(
                    "Claude Code requires Node.js. Install Node.js and @anthropic-ai/claude-code.");
        }
        throw new CLINotFoundError(
                "Claude Code CLI not found. Install with 'npm install -g @anthropic-ai/claude-code'.");
    }

    private String findExecutableOnPath(String name) {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }
        String[] parts = path.split(System.getProperty("path.separator"));
        for (String dir : parts) {
            Path candidate = Path.of(dir, name);
            if (candidate.toFile().canExecute()) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }

    private List<String> buildCommand() throws ClaudeSDKException {
        List<String> cmd = new ArrayList<>();
        cmd.add(cliPath);
        cmd.add("--output-format");
        cmd.add("stream-json");
        cmd.add("--verbose");

        if (options.getSystemPrompt() != null) {
            cmd.add("--system-prompt");
            cmd.add(options.getSystemPrompt());
        }
        if (options.getAppendSystemPrompt() != null) {
            cmd.add("--append-system-prompt");
            cmd.add(options.getAppendSystemPrompt());
        }
        if (!options.getAllowedTools().isEmpty()) {
            cmd.add("--allowedTools");
            cmd.add(String.join(",", options.getAllowedTools()));
        }
        if (options.getMaxTurns() != null) {
            cmd.add("--max-turns");
            cmd.add(String.valueOf(options.getMaxTurns()));
        }
        if (!options.getDisallowedTools().isEmpty()) {
            cmd.add("--disallowedTools");
            cmd.add(String.join(",", options.getDisallowedTools()));
        }
        if (options.getModel() != null) {
            cmd.add("--model");
            cmd.add(options.getModel());
        }
        if (options.getPermissionPromptToolName() != null) {
            cmd.add("--permission-prompt-tool");
            cmd.add(options.getPermissionPromptToolName());
        }
        if (options.getPermissionMode() != null) {
            cmd.add("--permission-mode");
            cmd.add(options.getPermissionMode().getValue());
        }
        if (options.isContinueConversation()) {
            cmd.add("--continue");
        }
        if (options.getResume() != null) {
            cmd.add("--resume");
            cmd.add(options.getResume());
        }
        if (options.getSettings() != null) {
            cmd.add("--settings");
            cmd.add(options.getSettings());
        }
        if (!options.getAddDirs().isEmpty()) {
            for (Path path : options.getAddDirs()) {
                cmd.add("--add-dir");
                cmd.add(path.toString());
            }
        }
        Object mcpServers = options.getMcpServers();
        if (mcpServers instanceof Map<?, ?> map && !map.isEmpty()) {
            Map<String, Object> processed = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (value instanceof Map<?, ?> serverConfig) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    serverConfig.forEach((k, v) -> {
                        if (!Objects.equals(k, "instance")) {
                            copy.put(String.valueOf(k), v);
                        }
                    });
                    processed.put(String.valueOf(key), copy);
                }
            });
            if (!processed.isEmpty()) {
                try {
                    cmd.add("--mcp-config");
                    cmd.add(mapper.writeValueAsString(Map.of("mcpServers", processed)));
                } catch (JsonProcessingException e) {
                    throw new ClaudeSDKException("Failed to serialize MCP configuration", e);
                }
            }
        } else if (mcpServers instanceof String str && !str.isBlank()) {
            cmd.add("--mcp-config");
            cmd.add(str);
        } else if (mcpServers instanceof Path path) {
            cmd.add("--mcp-config");
            cmd.add(path.toString());
        }

        options.getExtraArgs().forEach((flag, value) -> {
            cmd.add("--" + flag);
            if (value != null && !value.isBlank()) {
                cmd.add(value);
            }
        });

        if (streaming) {
            cmd.add("--input-format");
            cmd.add("stream-json");
        } else if (prompt != null) {
            cmd.add("--print");
            cmd.add("--");
            cmd.add(prompt);
        }
        return cmd;
    }

    @Override
    public void connect() throws ClaudeSDKException {
        if (process != null) {
            return;
        }
        List<String> command = buildCommand();
        ProcessBuilder builder = new ProcessBuilder(command);
        if (options.getCwd() != null) {
            builder.directory(options.getCwd().toFile());
        }
        Map<String, String> env = builder.environment();
        env.putAll(System.getenv());
        env.putAll(options.getEnv());
        env.put("CLAUDE_CODE_ENTRYPOINT", "sdk-java");
        if (options.getCwd() != null) {
            env.put("PWD", options.getCwd().toString());
        }
        if (options.getUser() != null) {
            builder.environment().put("USER", options.getUser());
        }
        try {
            process = builder.start();
            if (process.getOutputStream() != null) {
                stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            }
            InputStream stdoutStream = process.getInputStream();
            stdout = new BufferedReader(new InputStreamReader(stdoutStream, StandardCharsets.UTF_8));
            ready.set(true);
        } catch (IOException e) {
            throw new CLIConnectionError("Failed to start Claude Code CLI", e);
        }
    }

    @Override
    public synchronized void write(String data) throws ClaudeSDKException {
        if (!ready.get() || stdin == null) {
            throw new CLIConnectionError("ProcessTransport is not ready for writing");
        }
        if (process != null && !process.isAlive()) {
            throw new CLIConnectionError("Cannot write to terminated process");
        }
        if (exitError != null) {
            throw new CLIConnectionError("Process exited with error", exitError);
        }
        try {
            stdin.write(data);
            stdin.flush();
        } catch (IOException e) {
            ready.set(false);
            exitError = new CLIConnectionError("Failed to write to process stdin", e);
            throw exitError;
        }
    }

    @Override
    public synchronized void endInput() throws ClaudeSDKException {
        if (stdin != null) {
            try {
                stdin.close();
            } catch (IOException e) {
                throw new CLIConnectionError("Failed to close stdin", e);
            } finally {
                stdin = null;
            }
        }
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public void readMessages(MessageHandler handler) {
        this.handler = handler;
        readerTask = executor.submit(this::readLoop);
    }

    private void readLoop() {
        StringBuilder buffer = new StringBuilder();
        try {
            String line;
            while ((line = stdout.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (buffer.length() + trimmed.length() > MAX_BUFFER_SIZE) {
                    buffer.setLength(0);
                    throw new CLIJSONDecodeError(
                            "JSON message exceeded maximum buffer size of " + MAX_BUFFER_SIZE + " bytes");
                }
                buffer.append(trimmed);
                try {
                    Map<String, Object> message = mapper.readValue(buffer.toString(), new TypeReference<>() {});
                    buffer.setLength(0);
                    if (handler != null) {
                        handler.onMessage(message);
                    }
                } catch (JsonProcessingException e) {
                    // Wait for more data
                    continue;
                }
            }
            if (handler != null) {
                handler.onClosed();
            }
        } catch (CLIJSONDecodeError e) {
            exitError = e;
            if (handler != null) {
                handler.onError(e);
            }
        } catch (IOException e) {
            if (closed.get()) {
                return;
            }
            if (handler != null) {
                handler.onError(new CLIConnectionError("Failed to read from CLI", e));
            }
        } finally {
            checkExitCode();
        }
    }

    private void checkExitCode() {
        if (process == null) {
            return;
        }
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String stderr = readStream(process.getErrorStream());
                exitError = new ProcessError(
                        String.format(Locale.ROOT, "Command failed with exit code %d", exitCode), exitCode, stderr);
                if (handler != null) {
                    handler.onError(exitError);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String readStream(InputStream stream) {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
            return sb.toString();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to read stderr", e);
            return "";
        }
    }

    @Override
    public void close() throws ClaudeSDKException {
        closed.set(true);
        ready.set(false);
        if (readerTask != null) {
            readerTask.cancel(true);
        }
        if (stdin != null) {
            try {
                stdin.close();
            } catch (IOException ignored) {
            }
        }
        if (stdout != null) {
            try {
                stdout.close();
            } catch (IOException ignored) {
            }
        }
        if (process != null) {
            process.destroy();
            try {
                process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        executor.shutdownNow();
        if (exitError != null) {
            throw exitError;
        }
    }
}
