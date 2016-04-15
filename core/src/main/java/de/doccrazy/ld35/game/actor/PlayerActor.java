package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.base.CollisionListener;
import de.doccrazy.shared.game.base.KeyboardMovementListener;
import de.doccrazy.shared.game.base.MovementInputListener;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;

public class PlayerActor extends ShapeActor<GameWorld> implements CollisionListener {
    private static final float RADIUS = 0.5f;

    private MovementInputListener movement;

    public PlayerActor(GameWorld world, Vector2 spawn) {
        super(world, spawn, false);
        setzOrder(50);
        //setScaleX(Resource.GFX.mower.getWidth() / Resource.GFX.mower.getHeight());
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        return BodyBuilder.forDynamic(spawn)
                .fixShape(ShapeBuilder.circle(RADIUS)).fixSensor();
    }

    public void setupKeyboardControl() {
        movement = new KeyboardMovementListener();
        addListener((InputListener)movement);
    }

    public void setupController(MovementInputListener movement) {
        this.movement = movement;
    }

    @Override
    protected void doAct(float delta) {
        super.doAct(delta);

    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

    }

    private void drawParticle(Batch batch, ParticleEffectPool.PooledEffect effect, Vector2 attach, float rotation) {
        Vector2 center = new Vector2(getX() + getOriginX(), getY() + getOriginY());
        float r = getRotation() + rotation;
        Vector2 p = attach.rotate(r).add(center);
        effect.setPosition(p.x, p.y);
        effect.getEmitters().first().getAngle().setHigh(190 + r, 170 + r);
        effect.update(Gdx.graphics.getDeltaTime());
        effect.draw(batch);
    }

    @Override
    public boolean beginContact(Body me, Body other, Vector2 normal, Vector2 contactPoint) {
        return false;
    }

    @Override
    public void endContact(Body other) {

    }

    @Override
    public void hit(float force) {

    }
}
