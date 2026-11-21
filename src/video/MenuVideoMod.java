package video;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.func.Boolc;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
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

public class MenuVideoMod extends Mod{
    VideoPlayer player = VideoPlayerCreator.createVideoPlayer();
    Boolc f1 = b -> Reflect.set(Vars.ui.menufrag, "renderer", b && Vars.dataDirectory.child("video").exists() && Vars.dataDirectory.child("video").child(Core.settings.getString("video-path")).exists() ? new VideoMenuRenderer() : new MenuRenderer());
    Runnable f2 = () -> {
        String str = Core.settings.getString("video-path");
        if(str.isEmpty()) return;
        Fi file = Vars.dataDirectory.child("video").child(Core.settings.getString("video-path"));
        try {
            player.load(file);
        } catch (FileNotFoundException e) {
            Log.warn("[MenuVideo] fail to loading video file: " + e);
        }
    };

    @Override
    public void init() {
        FfMpeg.loadLibraries();
    }

    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if(!Vars.dataDirectory.child("video").exists()) Vars.dataDirectory.child("video").mkdirs();
            Vars.ui.settings.addCategory("@settings.video-settings", tab -> {
                tab.checkPref("video-enabled", false, f1);
                tab.textPref("video-path", "");
                tab.sliderPref("video-volume", 100, 0, 100, m -> m + "%");
                tab.button("@settings.video-reload.name", f2).tooltip("@settings.video-reload.description");
            });
            f1.get(Core.settings.getBool("video-enabled"));
            f2.run();
        });
    }

    public class VideoMenuRenderer extends MenuRenderer {
        @Override
        public void render() {
            if(player.isDisposed() || isDisposed()) return;
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
