package de.doccrazy.ld35.game.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import de.doccrazy.ld35.game.ui.UiRoot;
import de.doccrazy.shared.game.world.GameState;

public class GameInputListener extends InputListener {
    private GameWorld world;
    private UiRoot root;

    public GameInputListener(UiRoot root) {
        this.root = root;
        this.world = root.getWorld();
        reset();
    }

    @Override
    public boolean keyTyped(InputEvent event, char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(InputEvent event, float x, float y) {
        world.setMouseTarget(new Vector2(x, y));
        return true;
    }

    @Override
    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
        if (world.getGameState() != GameState.GAME) {
            return false;
        }
        if (button == 0) {
            //world.getPlayer().setJump(true);
            return true;
            //start = world.createAttachedPoint(new Vector2(x, y), 0.02f);
        }
        if (button == 2) {
            //world.createFly(new Vector2(x, y));
        }
        return false;
    }

    @Override
    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
        if (world.getGameState() != GameState.GAME) {
            return;
        }
        if (button == 0) {
            //world.getPlayer().setJump(false);
        }
    }

    public void reset() {
    }
}
