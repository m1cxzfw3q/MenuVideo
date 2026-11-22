package video;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.files.ZipFi;
import arc.func.Boolc;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.util.OS;
import arc.util.Reflect;
import arc.video.FfMpeg;
import arc.video.VideoPlayer;
import arc.video.VideoPlayerCreator;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.MenuRenderer;
import mindustry.mod.Mod;
import org.jline.utils.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MenuVideoMod extends Mod{
    boolean loaded = false;
    VideoPlayer player = VideoPlayerCreator.createVideoPlayer();
    Boolc f1 = b -> Reflect.set(Vars.ui.menufrag, "renderer", b && Vars.dataDirectory.child("video").exists() && Vars.dataDirectory.child("video").child(Core.settings.getString("video-path")).exists() && loaded ? new VideoMenuRenderer() : new MenuRenderer());
    Runnable f2 = () -> {
        String str = Core.settings.getString("video-path");
        if(str.isEmpty()) return;
        Fi file = Vars.dataDirectory.child("video").child(Core.settings.getString("video-path"));
        try {
            loaded = player.load(file);
        } catch (FileNotFoundException e) {
            Log.warn("[MenuVideo] fail to loading video file: " + e);
        }
    };

    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (!OS.isAndroid) {
                if (!Vars.dataDirectory.child("video").exists()) Vars.dataDirectory.child("video").mkdirs();
                String libName = mapLibraryName("arc-video-desktop");
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(libName)) {
                    Fi file = Vars.dataDirectory.child("cache").child(libName);
                    if(!file.exists()) file.mkdirs();
                    file.write(is, false);
                } catch (IOException ex) {
                    throw new RuntimeException("[MenuVideo] fail to loading native library file: " + ex);
                }
                FfMpeg.setLibraryFilePath(Vars.dataDirectory.child("cache").path());
                FfMpeg.loadLibraries();
            }

            Vars.ui.settings.addCategory("@settings.video-settings", tab -> {
                tab.checkPref("video-enabled", false, f1);
                tab.textPref("video-path", "");
                tab.sliderPref("video-volume", 100, 0, 100, m -> m + "%");
                tab.row();
                tab.button("@setting.video-reload.name", f2).width(100f).tooltip("@setting.video-reload.description");
            });
            f1.get(Core.settings.getBool("video-enabled"));
            f2.run();
        });
    }

    public String mapLibraryName(String libraryName) {
        if (OS.isWindows) {
            return libraryName + (OS.is64Bit ? "64.dll" : ".dll");
        } else if (OS.isLinux) {
            return "lib" + libraryName + (OS.isARM ? "arm" : "") + (OS.is64Bit ? "64.so" : ".so");
        } else {
            return OS.isMac ? "lib" + libraryName + (OS.isARM ? "arm" : "") + (OS.is64Bit ? "64.dylib" : ".dylib") : libraryName;
        }
    }

    public class VideoMenuRenderer extends MenuRenderer {
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
