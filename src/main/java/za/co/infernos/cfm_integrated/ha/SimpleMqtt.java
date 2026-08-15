package za.co.infernos.cfm_integrated.ha;

import za.co.infernos.cfm_integrated.CfmIntegrated;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * Minimal MQTT 3.1.1 client (CONNECT / PUBLISH QoS0 / SUBSCRIBE QoS0 / PINGREQ).
 */
public final class SimpleMqtt implements AutoCloseable {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String clientId;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Thread reader;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Map<String, BiConsumer<String, String>> handlers = new ConcurrentHashMap<>();
    private long lastPing;

    public SimpleMqtt(String host, int port, String username, String password, String clientId) {
        this.host = host;
        this.port = port;
        this.username = username == null ? "" : username;
        this.password = password == null ? "" : password;
        this.clientId = clientId;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public synchronized void connect() throws IOException {
        closeQuietly();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 4000);
        socket.setSoTimeout(0);
        socket.setTcpNoDelay(true);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeUtf(payload, "MQTT");
        payload.write(4); // protocol level
        int flags = 0x02; // clean session
        if (!username.isEmpty()) {
            flags |= 0x80;
        }
        if (!password.isEmpty()) {
            flags |= 0x40;
        }
        payload.write(flags);
        payload.write((30 >> 8) & 0xFF);
        payload.write(30 & 0xFF);
        writeUtf(payload, clientId);
        if (!username.isEmpty()) {
            writeUtf(payload, username);
        }
        if (!password.isEmpty()) {
            writeUtf(payload, password);
        }
        writePacket(0x10, payload.toByteArray());

        int header = in.readUnsignedByte();
        if ((header >> 4) != 2) {
            throw new IOException("Expected CONNACK, got " + header);
        }
        int remaining = readRemaining(in);
        byte[] ack = in.readNBytes(remaining);
        if (ack.length < 2 || ack[1] != 0) {
            throw new IOException("MQTT CONNACK rejected: " + (ack.length < 2 ? "?" : ack[1]));
        }
        connected.set(true);
        lastPing = System.currentTimeMillis();
        reader = new Thread(this::readLoop, "cfm-integrated-mqtt");
        reader.setDaemon(true);
        reader.start();
        CfmIntegrated.LOGGER.info("Home Assistant MQTT connected to {}:{}", host, port);
    }

    public void on(String topic, BiConsumer<String, String> handler) {
        handlers.put(topic, handler);
    }

    public synchronized void subscribe(String topic) throws IOException {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        payload.write(0);
        payload.write(1); // packet id
        writeUtf(payload, topic);
        payload.write(0); // QoS 0
        writePacket(0x82, payload.toByteArray());
    }

    public synchronized void publish(String topic, String message, boolean retain) throws IOException {
        if (!connected.get()) {
            return;
        }
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeUtf(payload, topic);
        payload.write(message.getBytes(StandardCharsets.UTF_8));
        int header = 0x30 | (retain ? 0x01 : 0);
        writePacket(header, payload.toByteArray());
    }

    public synchronized void pingIfNeeded() {
        if (!connected.get()) {
            return;
        }
        if (System.currentTimeMillis() - lastPing < 20_000) {
            return;
        }
        try {
            writePacket(0xC0, new byte[0]);
            lastPing = System.currentTimeMillis();
        } catch (IOException e) {
            connected.set(false);
        }
    }

    private void readLoop() {
        try {
            while (connected.get() && socket != null && !socket.isClosed()) {
                int header = in.readUnsignedByte();
                int remaining = readRemaining(in);
                byte[] body = in.readNBytes(remaining);
                int type = header >> 4;
                if (type == 3) {
                    handlePublish(body);
                }
            }
        } catch (IOException ignored) {
            connected.set(false);
        }
    }

    private void handlePublish(byte[] body) {
        if (body.length < 2) {
            return;
        }
        int topicLen = ((body[0] & 0xFF) << 8) | (body[1] & 0xFF);
        if (body.length < 2 + topicLen) {
            return;
        }
        String topic = new String(body, 2, topicLen, StandardCharsets.UTF_8);
        String msg = new String(body, 2 + topicLen, body.length - 2 - topicLen, StandardCharsets.UTF_8);
        BiConsumer<String, String> exact = handlers.get(topic);
        if (exact != null) {
            exact.accept(topic, msg);
            return;
        }
        for (Map.Entry<String, BiConsumer<String, String>> e : handlers.entrySet()) {
            if (topicMatch(e.getKey(), topic)) {
                e.getValue().accept(topic, msg);
            }
        }
    }

    private static boolean topicMatch(String filter, String topic) {
        if (filter.endsWith("/#")) {
            return topic.startsWith(filter.substring(0, filter.length() - 1)) || topic.equals(filter.substring(0, filter.length() - 2));
        }
        return filter.equals(topic);
    }

    private synchronized void writePacket(int header, byte[] payload) throws IOException {
        if (out == null) {
            throw new IOException("not connected");
        }
        out.writeByte(header);
        writeRemaining(out, payload.length);
        out.write(payload);
        out.flush();
    }

    private static void writeUtf(ByteArrayOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.write((b.length >> 8) & 0xFF);
        out.write(b.length & 0xFF);
        out.write(b);
    }

    private static void writeRemaining(DataOutputStream out, int length) throws IOException {
        do {
            int digit = length % 128;
            length /= 128;
            if (length > 0) {
                digit |= 0x80;
            }
            out.writeByte(digit);
        } while (length > 0);
    }

    private static int readRemaining(DataInputStream in) throws IOException {
        int multiplier = 1;
        int value = 0;
        int digit;
        do {
            digit = in.readUnsignedByte();
            value += (digit & 127) * multiplier;
            multiplier *= 128;
        } while ((digit & 128) != 0);
        return value;
    }

    private void closeQuietly() {
        connected.set(false);
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        out = null;
        in = null;
    }

    @Override
    public void close() {
        closeQuietly();
    }
}
