package com.movtery.zalithlauncher.gdworld;

import com.movtery.zalithlauncher.game.download.jvm_server.JvmExitSignal;
import org.junit.Test;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import static org.junit.Assert.*;

public class JvmExitSignalTest {
    @Test public void fastExitIsQueuedBeforeTheWaiterStarts() throws Exception {
        for (int i = 0; i < 10; i++) {
            try (JvmExitSignal signal = new JvmExitSignal(0)) {
                JvmExitSignal.send(signal.getPort(), i);
                assertEquals(Integer.valueOf(i), signal.receive());
            }
        }
    }

    @Test public void aNewRunNeverReusesThePreviousSuccess() throws Exception {
        try (JvmExitSignal signal = new JvmExitSignal(0)) {
            JvmExitSignal.send(signal.getPort(), 0);
            assertEquals(Integer.valueOf(0), signal.receive());
        }
        try (JvmExitSignal signal = new JvmExitSignal(0)) {
            assertNull(signal.receive());
            JvmExitSignal.send(signal.getPort(), 137);
            assertEquals(Integer.valueOf(137), signal.receive());
        }
    }

    @Test public void malformedPacketsDoNotBecomeSuccessfulOrTruncateTheNextCode() throws Exception {
        try (JvmExitSignal signal = new JvmExitSignal(0); DatagramSocket sender = new DatagramSocket()) {
            byte[] invalid = {'x'};
            sender.send(new DatagramPacket(invalid, invalid.length, InetAddress.getByName("127.0.0.1"), signal.getPort()));
            assertNull(signal.receive());
            JvmExitSignal.send(signal.getPort(), -123);
            assertEquals(Integer.valueOf(-123), signal.receive());
        }
    }

    @Test public void closingTheReceiverReleasesThePort() throws Exception {
        JvmExitSignal first = new JvmExitSignal(0);
        int port = first.getPort();
        first.close();
        try (JvmExitSignal next = new JvmExitSignal(port)) {
            JvmExitSignal.send(port, 1);
            assertEquals(Integer.valueOf(1), next.receive());
        }
    }

    @Test public void occupiedPortsFailBeforeStartingAChild() throws Exception {
        try (JvmExitSignal first = new JvmExitSignal(0)) {
            try (JvmExitSignal ignored = new JvmExitSignal(first.getPort())) { fail("occupied port"); }
            catch (IOException expected) { }
        }
    }
}
