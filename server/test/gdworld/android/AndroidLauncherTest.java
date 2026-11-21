package gdworld.android;

import pro.gravit.launchserver.socket.Client;
import pro.gravit.launcher.base.events.RequestEvent;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AndroidLauncherTest {
    private static final class Response extends AndroidLauncherResponse {
        boolean success;
        String error;
        @Override public void sendResult(RequestEvent event) { success = true; }
        @Override public void sendError(String message) { error = message; }
    }
    public static void main(String[] args) throws Exception {
        Path allowed = Path.of("config/GDWorldAndroid/sha256.txt");
        String known = "a".repeat(64);
        Response missing = new Response(); missing.sha256 = known;
        Client client = new Client(); missing.execute(null, client);
        assert !client.checkSign && !missing.success && missing.error != null;
        Files.createDirectories(allowed.getParent()); Files.writeString(allowed, known + "\n");
        for (String invalid : new String[]{"../escape", "", "b".repeat(64), "A".repeat(64)}) {
            Response denied = new Response(); denied.sha256 = invalid; denied.execute(null, client);
            assert !client.checkSign && !denied.success && denied.error != null;
        }
        Response accepted = new Response(); accepted.sha256 = known; accepted.execute(null, client);
        assert client.checkSign && accepted.success;
        assert !client.isAuth && client.uuid == null && client.type == null;
        client.isAuth = true;
        Response already = new Response(); already.sha256 = known; already.execute(null, client);
        assert !already.success && already.error != null;
        System.out.println("Android build registration: all checks passed");
    }
}
