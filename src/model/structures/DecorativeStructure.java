package model.structures;

import core.GameConfig;
import core.Vector2;
import model.entity.Structure;

/**
 * Vật trang trí trong village. Luôn là vật cản cứng.
 */
public class DecorativeStructure extends Structure {
    public DecorativeStructure(Vector2 position, String imageVariant) {
        super(position, GameConfig.getInstance().DECORATIVE_STRUCTURE_SIZE, "DECORATIVE", imageVariant, true);
    }
}
