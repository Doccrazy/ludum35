package de.doccrazy.ld35.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Scaling;

import box2dLight.ConeLight;
import box2dLight.RayHandler;
import de.doccrazy.ld35.data.GameRules;
import de.doccrazy.ld35.game.actor.PlayerActor;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.ld35.game.world.ScreenShakeEvent;
import de.doccrazy.shared.game.BaseGameRenderer;
import de.doccrazy.shared.game.world.GameState;
import net.dermetfan.gdx.math.GeometryUtils;

public class GameRenderer extends BaseGameRenderer<GameWorld> {
	private static final float CAM_PPS = 5f;

    private Scaling bgScaling = Scaling.fill;
	private float zoom = 1;
	private float zoomDelta = 0;
	private float camY;
    private boolean animateCamera;
	private float shakeAmount = 0;

    public GameRenderer(GameWorld world) {
        super(world, new Vector2(GameRules.LEVEL_WIDTH, GameRules.LEVEL_HEIGHT));
    }

    @Override
    protected void init() {
        world.rayHandler.setAmbientLight(new Color(0.5f, 0.5f, 0.5f, 1f));
    }

    @Override
	protected void drawBackground(SpriteBatch batch) {
		Gdx.gl.glClearColor(0, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        //Vector2 bgSize = bgScaling.apply(gameViewport.x, gameViewport.y, world.stage.getWidth(), world.stage.getHeight());
        //batch.draw(Resource.GFX.backgroundHigh, world.stage.getWidth() / 2 - bgSize.x / 2, 0, bgSize.x, bgSize.y);
        //batch.draw(Resource.GFX.backgroundLow, world.stage.getWidth() / 2 - bgSize.x / 2, -bgSize.y + 0.1f, bgSize.x, bgSize.y);
    }

	@Override
	protected void beforeRender() {
        shakeAmount = shakeAmount * 0.91f;
        world.pollEvents(ScreenShakeEvent.class, screenShakeEvent -> shakeAmount += 0.1f);

	    //zoom = MathUtils.clamp(zoom + zoomDelta*0.02f, 1f, 2f);

        if (world.getGameState() != GameState.INIT) {
            Vector2 cameraCenter = GeometryUtils.keepWithin(new Vector2(world.getPlayer().getX() - GameRules.LEVEL_WIDTH / 2f, world.getPlayer().getY() - GameRules.LEVEL_HEIGHT / 2f),
                    GameRules.LEVEL_WIDTH, GameRules.LEVEL_HEIGHT,
                    0, 0, world.getLevel().getBoundingBox().width, world.getLevel().getBoundingBox().height);
            camera.position.x = cameraCenter.x + GameRules.LEVEL_WIDTH / 2f + MathUtils.random(-shakeAmount, shakeAmount);
            camera.position.y = cameraCenter.y + GameRules.LEVEL_HEIGHT / 2f + MathUtils.random(-shakeAmount, shakeAmount);
        }

        /*if (animateCamera) {
            camY -= Gdx.graphics.getDeltaTime() * CAM_PPS;
        }
        camera.position.x = GameRules.LEVEL_WIDTH / 2;
        camera.position.y = Math.max(camY, gameViewport.y/2 - GameRules.LEVEL_HEIGHT + 1);*/

        /*PlayerActor p1 = ((GameWorld)world).getPlayer(0);
        PlayerActor p2 = ((GameWorld)world).getPlayer(1);
        if (p1.isDead()) {
        	p1 = p2;
        }
        if (p2.isDead()) {
        	p2 = p1;
        }
        updateSpot(spotP1, p1);
    	updateSpot(spotP2, p2);
    	for (ConeLight2 spot : discoSpots) {
    		spot.setActive(world.getGameState() == GameState.VICTORY || world.getGameState() == GameState.DEFEAT);
    		if (Math.random() < 0.01) {
    			spot.setDirection((float)(Math.random()*160 + 190));
    		}
    	}*/
	}

	private void updateSpot(ConeLight2 spot, PlayerActor player) {
		Vector2 dir = new Vector2(player.getX() + player.getOriginX(), player.getY() + player.getOriginY());
    	dir.sub(spot.getX(), spot.getY());
    	spot.setDirection(spot.getDirection() + (dir.angle() - spot.getDirection()) * 0.05f);
	}

}

class ConeLight2 extends ConeLight {

	public ConeLight2(RayHandler rayHandler, int rays, Color color,
			float distance, float x, float y, float directionDegree,
			float coneDegree) {
		super(rayHandler, rays, color, distance, x, y, directionDegree, coneDegree);
	}

	public float getDirection() {
		return direction;
	}
}