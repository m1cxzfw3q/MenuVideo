package video;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Boolc;
import arc.func.Intc;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.AdvanceSharedLibraryLoader;
import arc.util.OS;
import arc.util.Reflect;
import arc.video.FfMpeg;
import arc.video.VideoDecoder;
import arc.video.VideoPlayer;
import arc.video.VideoPlayerCreator;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.MenuRenderer;
import mindustry.mod.Mod;
import arc.util.Log;

import java.io.FileNotFoundException;

public class MenuVideoMod extends Mod{
    boolean loaded = false;
    Fi videoPath = Vars.dataDirectory.child("video");
    VideoPlayer player;
    Boolc f1 = b -> {
        MenuRenderer renderer = b && videoPath.exists() && loaded ? new VideoMenuRenderer() : new MenuRenderer();
        if(Reflect.get(Vars.ui.menufrag, "renderer").getClass() != renderer.getClass()){
            Reflect.set(Vars.ui.menufrag, "renderer", renderer);
        }
    };
    Runnable f2 = () -> {
        String str = Core.settings.getString("video-path");
        if(str.isEmpty()) return;
        Fi file = videoPath.child(Core.settings.getString("video-path"));
        try {
            Log.info(VideoDecoder.class.getClassLoader());
            Log.info(VideoDecoder.class.getProtectionDomain().getCodeSource().getLocation());
            loaded = player.load(file);
        } catch (FileNotFoundException e) {
            Log.warn("[MenuVideo] fail to loading video file: " + e);
        }
    };
    Intc f3 = vol -> player.setVolume(vol / 100f);

    @Override
    public void init() {
        if ((OS.isWindows && !OS.isARM) || OS.isLinux || OS.isMac) {
            String libraryPath = Vars.mods.getMod(this.getClass()).file.path();
            AdvanceSharedLibraryLoader libLoader = libraryPath == null ? new AdvanceSharedLibraryLoader() : new AdvanceSharedLibraryLoader(libraryPath);

            Log.info(libLoader.getClass().getClassLoader());
            try {
                libLoader.load("arc-video-desktop");
            } catch (Exception ignored) {}
            Reflect.set(FfMpeg.class, "loaded", true);
            if(!FfMpeg.isLoaded()) throw new RuntimeException("[MenuVideo] fail to loading native library.");
        }
    }

    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (!videoPath.exists()) videoPath.mkdirs();
            player = VideoPlayerCreator.createVideoPlayer();
            Vars.ui.settings.addCategory("@settings.video-settings", tab -> {
                tab.checkPref("video-enabled", false, f1);
                tab.textPref("video-path", "");
                tab.sliderPref("video-volume", 100, 0, 100, m -> m + "%", f3);
                tab.row();
                tab.button("@setting.video-reload.name", f2).width(100f).tooltip("@setting.video-reload.description");
            });
            f1.get(Core.settings.getBool("video-enabled"));
            f2.run();
            f3.get(Core.settings.getInt("video-volume"));
        });
    }

    private class VideoMenuRenderer extends MenuRenderer {
        @Override
        public void render() {
            if(player.isDisposed() || isDisposed() || !loaded) return;
            if(!player.isPlaying()) player.play();
            player.update();
            TextureRegion tex = new TextureRegion(player.getTexture());
            Draw.rect(tex, 0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
            Draw.flush();
        }

        @Override
        public void dispose() {
            player.dispose();
        }
    }
}
