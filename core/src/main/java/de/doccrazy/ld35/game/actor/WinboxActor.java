package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.base.CollisionListener;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.GameState;
import de.doccrazy.shared.game.world.ShapeBuilder;

public class WinboxActor extends ShapeActor<GameWorld> implements CollisionListener {
    private final Rectangle rect;
    private float hitTime = 9999999f;

    public WinboxActor(GameWorld world, Rectangle rect) {
        super(world, Vector2.Zero, false);
        this.rect = rect;
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        Vector2 center = new Vector2();
        return BodyBuilder.forStatic(rect.getCenter(center))
                .fixShape(ShapeBuilder.box(rect.getWidth()/2, rect.getHeight()/2))
                .fixSensor();
    }

    @Override
    protected void doAct(float delta) {
        if (stateTime - hitTime > 1f) {
            world.transition(GameState.VICTORY);
        }
        super.doAct(delta);
    }

    @Override
    public boolean beginContact(Body me, Body other, Vector2 normal, Vector2 contactPoint) {
        if (other.getUserData() == world.getPlayer()) {
            hitTime = stateTime;
        }
        return false;
    }

    @Override
    public void endContact(Body other) {
        if (other.getUserData() == world.getPlayer()) {
            hitTime = 9999999f;
        }
    }

    @Override
    public void hit(float force) {
    }
}
