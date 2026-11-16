package video;

import arc.Events;
import com.badlogic.gdx.video.VideoPlayer;
import com.badlogic.gdx.video.VideoPlayerCreator;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MenuVideoMod extends Mod{
    static VideoPlayer player = VideoPlayerCreator.createVideoPlayer();

    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {

        });
    }
}
