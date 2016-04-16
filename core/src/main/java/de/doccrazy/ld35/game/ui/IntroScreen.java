package de.doccrazy.ld35.game.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.shared.game.world.GameState;

public class IntroScreen extends Widget {
    private final UiRoot uiRoot;
    private float stateTime;

    public IntroScreen(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
        setFillParent(true);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        setVisible(uiRoot.getWorld().getGameState() == GameState.INIT);
        stateTime += delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        /*batch.draw(Resource.GFX.introFull, getX(), getY(), getWidth(), getHeight());
        if (stateTime >= 1f) {
            float scale = Interpolation.bounceOut.apply(6f, 1f, Math.max(stateTime - 1f, 0));
            batch.draw(Resource.GFX.introSplash, getX() + 300, getY() + 120,
                    Resource.GFX.introSplash.getRegionWidth() / 2f, Resource.GFX.introSplash.getRegionHeight() / 2f,
                    Resource.GFX.introSplash.getRegionWidth(), Resource.GFX.introSplash.getRegionHeight(), scale, scale, 0);
        }*/
    }
}
