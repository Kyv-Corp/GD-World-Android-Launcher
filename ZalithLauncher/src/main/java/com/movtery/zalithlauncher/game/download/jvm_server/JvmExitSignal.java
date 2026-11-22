package com.movtery.zalithlauncher.game.download.jvm_server;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/** Bind before starting the child; a fast exit must remain queued until receive(). */
public final class JvmExitSignal implements Closeable {
    private final DatagramSocket socket;

    public JvmExitSignal(int port) throws IOException {
        socket = new DatagramSocket(port, InetAddress.getByName("127.0.0.1"));
        socket.setSoTimeout(250);
    }

    public int getPort() { return socket.getLocalPort(); }

    public Integer receive() throws IOException {
        byte[] bytes = new byte[128];
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        try { socket.receive(packet); }
        catch (SocketTimeoutException ignored) { return null; }
        String value = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.US_ASCII);
        try { return Integer.valueOf(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    public static void send(int port, int code) throws IOException {
        byte[] bytes = Integer.toString(code).getBytes(StandardCharsets.US_ASCII);
        try (DatagramSocket sender = new DatagramSocket()) {
            sender.send(new DatagramPacket(bytes, bytes.length, InetAddress.getByName("127.0.0.1"), port));
        }
    }

    @Override public void close() { socket.close(); }
}
