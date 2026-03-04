package com.example.ros2_android_test_app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SshViewModel extends ViewModel {

    public static class SshConnection {
        public String ip = "";
        public int port = 22;
        public String username = "";
        public String password = "";
    }

    private final MutableLiveData<SshConnection> ssh = new MutableLiveData<>(new SshConnection());
    private final MutableLiveData<String> outputData = new MutableLiveData<>("SSH ready");
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private Session session;
    private ChannelShell shellChannel;
    private OutputStream shellInput;
    private InputStream shellOutput;
    private volatile boolean reading;

    public LiveData<SshConnection> getSSH() {
        return ssh;
    }

    public LiveData<String> getOutputData() {
        return outputData;
    }

    public LiveData<Boolean> isConnected() {
        return connected;
    }

    public void connectViaSSH() {
        SshConnection cfg = ssh.getValue();
        if (cfg == null) cfg = new SshConnection();
        final SshConnection finalCfg = cfg;

        ioExecutor.execute(() -> {
            try {
                if (isSessionAlive()) {
                    appendLine("Already connected: " + finalCfg.username + "@" + finalCfg.ip + ":" + finalCfg.port);
                    connected.postValue(true);
                    return;
                }

                appendLine("Connecting to " + finalCfg.username + "@" + finalCfg.ip + ":" + finalCfg.port + " ...");

                JSch jsch = new JSch();
                Session newSession = jsch.getSession(finalCfg.username, finalCfg.ip, finalCfg.port);
                newSession.setPassword(finalCfg.password);
                newSession.setConfig("StrictHostKeyChecking", "no");
                newSession.setServerAliveInterval(15000);
                newSession.setTimeout(10000);
                newSession.connect(10000);

                Channel channel = newSession.openChannel("shell");
                ChannelShell newShell = (ChannelShell) channel;
                newShell.setPty(true);
                newShell.setPtyType("vt102");
                OutputStream in = newShell.getOutputStream();
                InputStream out = newShell.getInputStream();
                newShell.connect(8000);

                session = newSession;
                shellChannel = newShell;
                shellInput = in;
                shellOutput = out;
                connected.postValue(true);

                appendLine("Connected.");
                appendLine("Tip: 输入命令后回车，支持持续交互。可用 Abort 发送 Ctrl+C。\n");

                startReadLoop();
            } catch (Exception e) {
                appendLine("Connect failed: " + safeMsg(e));
                cleanupConnection();
                connected.postValue(false);
            }
        });
    }

    public void stopSsh() {
        ioExecutor.execute(() -> {
            appendLine("Disconnecting ...");
            cleanupConnection();
            connected.postValue(false);
            appendLine("Disconnected.");
        });
    }

    public void sendViaSSH(String message) {
        ioExecutor.execute(() -> {
            if (!isSessionAlive() || shellInput == null) {
                appendLine("Not connected. Please connect first.");
                return;
            }
            try {
                String cmd = message == null ? "" : message;
                if (cmd.trim().isEmpty()) {
                    return;
                }
                shellInput.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
                shellInput.flush();
                appendLine("$ " + cmd);
            } catch (Exception e) {
                appendLine("Send failed: " + safeMsg(e));
            }
        });
    }

    public void abortAction() {
        ioExecutor.execute(() -> {
            if (!isSessionAlive() || shellInput == null) {
                appendLine("Not connected.");
                return;
            }
            try {
                shellInput.write(3); // Ctrl+C
                shellInput.flush();
                appendLine("^C");
            } catch (Exception e) {
                appendLine("Abort failed: " + safeMsg(e));
            }
        });
    }

    public void setSshIp(String ip) {
        SshConnection cfg = ensureCfg();
        cfg.ip = ip == null ? "" : ip.trim();
        ssh.postValue(cfg);
    }

    public void setSshPort(String port) {
        SshConnection cfg = ensureCfg();
        int p = 22;
        try {
            p = Integer.parseInt(port == null ? "22" : port.trim());
        } catch (Exception ignored) {
        }
        cfg.port = p <= 0 ? 22 : p;
        ssh.postValue(cfg);
    }

    public void setSshPassword(String password) {
        SshConnection cfg = ensureCfg();
        cfg.password = password == null ? "" : password;
        ssh.postValue(cfg);
    }

    public void setSshUsername(String username) {
        SshConnection cfg = ensureCfg();
        cfg.username = username == null ? "" : username.trim();
        ssh.postValue(cfg);
    }

    private void startReadLoop() {
        if (reading) return;
        reading = true;

        ioExecutor.execute(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(shellOutput, StandardCharsets.UTF_8));
                String line;
                while (reading && isSessionAlive() && (line = reader.readLine()) != null) {
                    outputData.postValue(line);
                }
            } catch (Exception e) {
                if (reading) {
                    appendLine("SSH read stopped: " + safeMsg(e));
                }
            } finally {
                reading = false;
                if (!isSessionAlive()) {
                    connected.postValue(false);
                }
            }
        });
    }

    private boolean isSessionAlive() {
        return session != null && session.isConnected()
                && shellChannel != null && shellChannel.isConnected();
    }

    private void cleanupConnection() {
        reading = false;

        try {
            if (shellChannel != null) shellChannel.disconnect();
        } catch (Exception ignored) {
        }
        try {
            if (session != null) session.disconnect();
        } catch (Exception ignored) {
        }

        shellChannel = null;
        shellInput = null;
        shellOutput = null;
        session = null;
    }

    private void appendLine(String line) {
        outputData.postValue(line == null ? "" : line);
    }

    private String safeMsg(Throwable t) {
        if (t == null) return "unknown";
        String m = t.getMessage();
        return m == null || m.trim().isEmpty() ? t.getClass().getSimpleName() : m;
    }

    private SshConnection ensureCfg() {
        SshConnection cfg = ssh.getValue();
        if (cfg == null) {
            cfg = new SshConnection();
            ssh.postValue(cfg);
        }
        return cfg;
    }

    @Override
    protected void onCleared() {
        cleanupConnection();
        ioExecutor.shutdownNow();
        super.onCleared();
    }
}
