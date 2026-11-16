package video;

import arc.Events;
import mindustry.game.EventType;
import mindustry.mod.Mod;

public class MenuVideoMod extends Mod{
    public MenuVideoMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {

        });
    }
}
