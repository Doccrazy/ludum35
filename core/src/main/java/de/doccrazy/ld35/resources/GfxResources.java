package de.doccrazy.ld35.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.shared.core.ResourcesBase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GfxResources extends ResourcesBase {
    /*public TextureRegion introFull = new TextureRegion(texture("intro-full.png"));
    public TextureRegion introSplash = new TextureRegion(texture("intro2.png"));
    public TextureRegion intermezzo = new TextureRegion(texture("intermezzo.png"));
    public TextureRegion continueTx = new TextureRegion(texture("continue.png"));
    public TextureRegion thanksTx = new TextureRegion(texture("thanks.png"));
    public TextureRegion defeat = new TextureRegion(texture("defeat.png"));*/

    public Sprite[] player = new Sprite[]{atlas.createSprite("player_walk"), atlas.createSprite("player_roll"), atlas.createSprite("player_fly")};
    public Sprite[] blood = new Sprite[]{atlas.createSprite("blood1"), atlas.createSprite("blood2"), atlas.createSprite("blood3"), atlas.createSprite("blood4")};

    public Map<String, ParticleEffectPool> particles = new HashMap<String, ParticleEffectPool>() {{
        put("fire", particle("fire.p", 0.01f));
        put("smoke", particle("smoke.p", 0.01f));
    }};

    public XmlReader.Element level1 = xml("level.svg");
    public XmlReader.Element level2 = xml("level2.svg");
    public TextureRegion level1Tex = new TextureRegion(textureFilter("level.png"));
    public TextureRegion level2Tex = new TextureRegion(textureFilter("level2.png"));

    public GfxResources() {
        super("game.atlas");
    }

    protected XmlReader.Element xml(String filename) {
        try {
            return new XmlReader().parse(Gdx.files.internal(filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Texture textureFilter(String filename) {
        Texture tex = texture(filename);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }
}
