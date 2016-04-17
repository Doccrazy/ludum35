package de.doccrazy.ld35.game;

import com.badlogic.gdx.scenes.scene2d.Stage;

import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.actor.SVGLevelActor;
import de.doccrazy.ld35.game.ui.UiRoot;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.BaseGameScreen;

public class GameScreen extends BaseGameScreen<GameWorld, GameRenderer> {

	@Override
	protected GameWorld createWorld() {
		//return new GameWorld(Level1Actor::new, Level2Actor::new);
		return new GameWorld(w -> new SVGLevelActor(w, Resource.GFX.level2, Resource.GFX.level2Tex));
	}

	@Override
	protected GameRenderer createRenderer(GameWorld world) {
		return new GameRenderer(world);
	}

	@Override
	protected void createUI(Stage uiStage, GameWorld world, GameRenderer renderer) {
		new UiRoot(uiStage, world, renderer);
	}
}
