package com.anthropic.claudecode;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration options for Claude Code interactions.
 */
public class ClaudeCodeOptions {
    private List<String> allowedTools = new ArrayList<>();
    private String systemPrompt;
    private String appendSystemPrompt;
    private Object mcpServers = new LinkedHashMap<String, Object>();
    private PermissionMode permissionMode;
    private boolean continueConversation;
    private String resume;
    private Integer maxTurns;
    private List<String> disallowedTools = new ArrayList<>();
    private String model;
    private String permissionPromptToolName;
    private Path cwd;
    private String settings;
    private List<Path> addDirs = new ArrayList<>();
    private Map<String, String> env = new LinkedHashMap<>();
    private Map<String, String> extraArgs = new LinkedHashMap<>();
    private OutputStream debugStderr;
    private CanUseTool canUseTool;
    private Map<HookEvent, List<HookMatcher>> hooks;
    private String user;

    public ClaudeCodeOptions copy() {
        ClaudeCodeOptions copy = new ClaudeCodeOptions();
        copy.allowedTools = new ArrayList<>(allowedTools);
        copy.systemPrompt = systemPrompt;
        copy.appendSystemPrompt = appendSystemPrompt;
        if (mcpServers instanceof Map<?, ?> map) {
            copy.mcpServers = new LinkedHashMap<>(map);
        } else {
            copy.mcpServers = mcpServers;
        }
        copy.permissionMode = permissionMode;
        copy.continueConversation = continueConversation;
        copy.resume = resume;
        copy.maxTurns = maxTurns;
        copy.disallowedTools = new ArrayList<>(disallowedTools);
        copy.model = model;
        copy.permissionPromptToolName = permissionPromptToolName;
        copy.cwd = cwd;
        copy.settings = settings;
        copy.addDirs = new ArrayList<>(addDirs);
        copy.env = new LinkedHashMap<>(env);
        copy.extraArgs = new LinkedHashMap<>(extraArgs);
        copy.debugStderr = debugStderr;
        copy.canUseTool = canUseTool;
        if (hooks != null) {
            Map<HookEvent, List<HookMatcher>> hooksCopy = new HashMap<>();
            hooks.forEach((event, matchers) -> hooksCopy.put(event, new ArrayList<>(matchers)));
            copy.hooks = hooksCopy;
        }
        copy.user = user;
        return copy;
    }

    public List<String> getAllowedTools() {
        return allowedTools;
    }

    public ClaudeCodeOptions setAllowedTools(List<String> allowedTools) {
        this.allowedTools = Objects.requireNonNullElseGet(allowedTools, ArrayList::new);
        return this;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public ClaudeCodeOptions setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    public String getAppendSystemPrompt() {
        return appendSystemPrompt;
    }

    public ClaudeCodeOptions setAppendSystemPrompt(String appendSystemPrompt) {
        this.appendSystemPrompt = appendSystemPrompt;
        return this;
    }

    public Object getMcpServers() {
        return mcpServers;
    }

    public ClaudeCodeOptions setMcpServers(Object mcpServers) {
        this.mcpServers = mcpServers;
        return this;
    }

    public PermissionMode getPermissionMode() {
        return permissionMode;
    }

    public ClaudeCodeOptions setPermissionMode(PermissionMode permissionMode) {
        this.permissionMode = permissionMode;
        return this;
    }

    public boolean isContinueConversation() {
        return continueConversation;
    }

    public ClaudeCodeOptions setContinueConversation(boolean continueConversation) {
        this.continueConversation = continueConversation;
        return this;
    }

    public String getResume() {
        return resume;
    }

    public ClaudeCodeOptions setResume(String resume) {
        this.resume = resume;
        return this;
    }

    public Integer getMaxTurns() {
        return maxTurns;
    }

    public ClaudeCodeOptions setMaxTurns(Integer maxTurns) {
        this.maxTurns = maxTurns;
        return this;
    }

    public List<String> getDisallowedTools() {
        return disallowedTools;
    }

    public ClaudeCodeOptions setDisallowedTools(List<String> disallowedTools) {
        this.disallowedTools = Objects.requireNonNullElseGet(disallowedTools, ArrayList::new);
        return this;
    }

    public String getModel() {
        return model;
    }

    public ClaudeCodeOptions setModel(String model) {
        this.model = model;
        return this;
    }

    public String getPermissionPromptToolName() {
        return permissionPromptToolName;
    }

    public ClaudeCodeOptions setPermissionPromptToolName(String permissionPromptToolName) {
        this.permissionPromptToolName = permissionPromptToolName;
        return this;
    }

    public Path getCwd() {
        return cwd;
    }

    public ClaudeCodeOptions setCwd(Path cwd) {
        this.cwd = cwd;
        return this;
    }

    public String getSettings() {
        return settings;
    }

    public ClaudeCodeOptions setSettings(String settings) {
        this.settings = settings;
        return this;
    }

    public List<Path> getAddDirs() {
        return addDirs;
    }

    public ClaudeCodeOptions setAddDirs(List<Path> addDirs) {
        this.addDirs = Objects.requireNonNullElseGet(addDirs, ArrayList::new);
        return this;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public ClaudeCodeOptions setEnv(Map<String, String> env) {
        this.env = Objects.requireNonNullElseGet(env, LinkedHashMap::new);
        return this;
    }

    public Map<String, String> getExtraArgs() {
        return extraArgs;
    }

    public ClaudeCodeOptions setExtraArgs(Map<String, String> extraArgs) {
        this.extraArgs = Objects.requireNonNullElseGet(extraArgs, LinkedHashMap::new);
        return this;
    }

    public OutputStream getDebugStderr() {
        return debugStderr;
    }

    public ClaudeCodeOptions setDebugStderr(OutputStream debugStderr) {
        this.debugStderr = debugStderr;
        return this;
    }

    public CanUseTool getCanUseTool() {
        return canUseTool;
    }

    public ClaudeCodeOptions setCanUseTool(CanUseTool canUseTool) {
        this.canUseTool = canUseTool;
        return this;
    }

    public Map<HookEvent, List<HookMatcher>> getHooks() {
        return hooks;
    }

    public ClaudeCodeOptions setHooks(Map<HookEvent, List<HookMatcher>> hooks) {
        this.hooks = hooks;
        return this;
    }

    public String getUser() {
        return user;
    }

    public ClaudeCodeOptions setUser(String user) {
        this.user = user;
        return this;
    }
}
