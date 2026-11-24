package video;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Boolc;
import arc.func.Intc;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.util.OS;
import arc.util.Reflect;
import arc.util.Time;
import arc.video.FfMpeg;
import arc.video.VideoPlayer;
import arc.video.VideoPlayerCreator;
import mindustry.Vars;
import mindustry.audio.SoundControl;
import mindustry.game.EventType;
import mindustry.graphics.MenuRenderer;
import mindustry.mod.Mod;
import arc.util.Log;

import java.io.*;

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
            loaded = player.load(file);
        } catch (FileNotFoundException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            Log.warn("[MenuVideo] fail to loading video file: " + sw);
        }
    };
    Intc f3 = vol -> player.setVolume(vol / 100f);

    @Override
    public void init() {
        if ((OS.isWindows && !OS.isARM) || OS.isLinux || OS.isMac) {
            try {
                FfMpeg.setLibraryFilePath(Vars.mods.getMod(this.getClass()).file.path());
                FfMpeg.loadLibraries();
            } catch (Exception e) {
                Log.info("e.toString(): " + e);
                Log.info("e.getMessage(): " + e.getMessage());
            }
            if(!FfMpeg.isLoaded()) throw new RuntimeException("[MenuVideo] fail to loading native library.");
        }
    }

    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (!videoPath.exists()) videoPath.mkdirs();
            player = VideoPlayerCreator.createVideoPlayer();
            Vars.control.sound = new VideoSoundControl();
            player.setLooping(true);
            Vars.ui.settings.addCategory("@settings.video-settings", tab -> {
                tab.checkPref("video-enabled", false, f1);
                tab.textPref("video-path", "");
                tab.sliderPref("video-volume", 100, 0, 100, m -> m + "%", f3);
            });
            Vars.ui.menufrag.addButton("@video-reload", f2);
            f1.get(Core.settings.getBool("video-enabled"));
            f2.run();
            f3.get(Core.settings.getInt("video-volume"));
        });
    }

    private class VideoMenuRenderer extends MenuRenderer {
        public VideoMenuRenderer() {
        }
        @Override
        public void render() {
            if(player.isDisposed() || isDisposed() || !loaded) return;
            if(!player.isPlaying()) player.play();
            player.update();
            TextureRegion tex = new TextureRegion(player.getTexture());
            Draw.rect(tex, Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 2f, Core.graphics.getWidth(), Core.graphics.getHeight());
            Draw.flush();
        }

        @Override
        public void dispose() {
            player.dispose();
        }
    }

    private static class VideoSoundControl extends SoundControl {
        @Override
        public void update() {
            if (!Core.settings.getBool("video-enabled")) super.update();
            else {
                fade = Mathf.clamp(fade - Time.delta / foutTime);
                if (current != null) {
                    current.setVolume(fade * volumeMultiplier());
                }
                if(fade <= 0.01f)stop();
            }
        }
    }
}
