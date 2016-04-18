package de.doccrazy.ld35.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.actor.Level;
import de.doccrazy.ld35.game.actor.SVGLevelActor;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.world.GameState;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.function.Function;

public class SelectLevelScreen extends Table {
    private final UiRoot uiRoot;
    private float stateTime;

    public SelectLevelScreen(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
        setFillParent(true);

        NinePatchDrawable patch = new NinePatchDrawable(Resource.GFX.button);
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(patch, patch, patch, Resource.FONT.menu);
        style.fontColor = Color.WHITE;
        style.overFontColor = Color.BLUE;

        createLevelButton(style, Resource.GFX.level1, Resource.GFX.level1Tex);
        createLevelButton(style, Resource.GFX.level2, Resource.GFX.level2Tex);
        try {
            Path levelsPath = Paths.get("").resolve("levels");
            if (Files.isDirectory(levelsPath)) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.svg");
                Files.list(levelsPath).filter(matcher::matches).forEach(path -> {
                    try {
                        XmlReader.Element xml = new XmlReader().parse(new FileInputStream(path.toFile()));

                        Path pngPath = path.resolveSibling(path.getFileName().toString().substring(0, path.getFileName().toString().length()-4) + ".png");
                        TextureRegion tex = new TextureRegion(textureFilter(pngPath));
                        createLevelButton(style, xml, tex);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }


    protected Texture textureFilter(Path filename) {
        Texture tex = new Texture(Gdx.files.absolute(filename.toAbsolutePath().toString()));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }

    private void createLevelButton(TextButton.TextButtonStyle style, XmlReader.Element xml, TextureRegion tex) {
        row();
        TextButton button = new TextButton(xml.get("title"), style);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                uiRoot.getWorld().setLevel(w -> new SVGLevelActor(w, xml, tex));
            }
        });
        add(button).pad(10);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(Resource.GFX.introFull, getX(), getY(), getWidth(), getHeight());
        super.draw(batch, parentAlpha);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        setVisible(uiRoot.getWorld().getGameState() == GameState.INIT);
    }
}
