package de.doccrazy.ld35.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.shared.core.ResourcesBase;

import java.io.IOException;

public class GfxResources extends ResourcesBase {
    /*public TextureRegion introFull = new TextureRegion(texture("intro-full.png"));
    public TextureRegion introSplash = new TextureRegion(texture("intro2.png"));
    public TextureRegion intermezzo = new TextureRegion(texture("intermezzo.png"));
    public TextureRegion continueTx = new TextureRegion(texture("continue.png"));
    public TextureRegion thanksTx = new TextureRegion(texture("thanks.png"));
    public TextureRegion defeat = new TextureRegion(texture("defeat.png"));*/

    public Sprite[] blood = new Sprite[]{atlas.createSprite("blood1"), atlas.createSprite("blood2"), atlas.createSprite("blood3"), atlas.createSprite("blood4")};

    public ParticleEffectPool partFire = particle("fire.p", 0.01f);
    public ParticleEffectPool partSmoke = particle("smoke.p", 0.008f);

    public XmlReader.Element level = xml("level.svg");
    public TextureRegion levelTex = new TextureRegion(texture("level.png"));

    public GfxResources() {
        super("game.atlas");
    }

    protected XmlReader.Element xml(String filename) {
        try {
            return new XmlReader().parse(Gdx.files.internal("level.svg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
