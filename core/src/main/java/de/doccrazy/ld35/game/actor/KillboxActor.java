package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.base.CollisionListener;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;

public class KillboxActor extends ShapeActor<GameWorld> implements CollisionListener {
    private final Vector2[] poly;

    public KillboxActor(GameWorld world, Vector2[] poly) {
        super(world, Vector2.Zero, false);
        this.poly = poly;
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        Vector2 center = new Vector2();
        return BodyBuilder.forStatic(poly[0])
                .fixShape(ShapeBuilder.polyRel(poly))
                .fixSensor();
    }

    @Override
    public boolean beginContact(Body me, Body other, Vector2 normal, Vector2 contactPoint) {
        if (other.getUserData() == world.getPlayer()) {
            world.getPlayer().damage(1f);
        }
        return false;
    }

    @Override
    public void endContact(Body other) {
    }

    @Override
    public void hit(float force) {
    }
}
