package gdworld.android;

import pro.gravit.launcher.base.modules.*;
import pro.gravit.launcher.base.modules.events.PreConfigPhase;
import pro.gravit.launchserver.socket.WebSocketService;
import pro.gravit.utils.Version;

public final class GDWorldAndroidModule extends LauncherModule {
    public GDWorldAndroidModule() {
        super(new LauncherModuleInfoBuilder().setName("GDWorldAndroid")
            .setVersion(new Version(1, 0, 0, 1, Version.Type.STABLE))
            .setDependencies(new String[]{"LaunchServerCore"}).createLauncherModuleInfo());
    }
    @Override public void init(LauncherInitContext context) {
        registerEvent(event -> WebSocketService.providers.register("gdworldAndroid", AndroidLauncherResponse.class), PreConfigPhase.class);
    }
}
