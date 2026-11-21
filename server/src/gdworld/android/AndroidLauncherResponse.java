package gdworld.android;

import io.netty.channel.ChannelHandlerContext;
import pro.gravit.launcher.base.events.RequestEvent;
import pro.gravit.launchserver.socket.Client;
import pro.gravit.launchserver.socket.response.SimpleResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

/** Registers Android builds independently of desktop launcher updates. */
public class AndroidLauncherResponse extends SimpleResponse {
    public String sha256;
    @Override public String getType() { return "gdworldAndroid"; }
    @Override public void execute(ChannelHandlerContext ctx, Client client) {
        if (client.isAuth || sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            sendError("android.build.denied");
            return;
        }
        try {
            boolean accepted = Files.readAllLines(Path.of("config/GDWorldAndroid/sha256.txt")).stream()
                .map(String::trim).anyMatch(sha256::equals);
            if (!accepted) { sendError("android.build.denied"); return; }
            client.checkSign = true;
            sendResult(new Result());
        } catch (IOException e) {
            sendError("android.build.unavailable");
        }
    }
    @Override public ThreadSafeStatus getThreadSafeStatus() { return ThreadSafeStatus.READ_WRITE; }
    public static final class Result extends RequestEvent {
        @Override public String getType() { return "gdworldAndroid"; }
    }
}
