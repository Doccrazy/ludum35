package de.doccrazy.ld35.game.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.shared.game.world.GameState;

public class DefeatScreen extends Widget {
    private final UiRoot uiRoot;
    private float stateTime;

    public DefeatScreen(UiRoot uiRoot) {
        this.uiRoot = uiRoot;
        setFillParent(true);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        boolean vis = uiRoot.getWorld().getGameState() == GameState.DEFEAT;
        setVisible(vis);
        if (vis) {
            stateTime += delta;
        } else {
            stateTime = 0;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        batch.setColor(1, 1, 1, Math.min(stateTime/4f, 1f));
        //batch.draw(Resource.GFX.defeat, getX(), getY(), getWidth(), getHeight());
        batch.setColor(1, 1, 1, 1);
    }
}
