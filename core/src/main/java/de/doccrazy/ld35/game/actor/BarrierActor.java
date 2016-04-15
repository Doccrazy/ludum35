package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;

public class BarrierActor extends ShapeActor<GameWorld> {
    private final Rectangle rect;

    public BarrierActor(GameWorld world, Rectangle rect) {
        super(world, Vector2.Zero, false);
        this.rect = rect;
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        Vector2 center = new Vector2();
        return BodyBuilder.forStatic(rect.getCenter(center))
                .fixShape(ShapeBuilder.box(rect.getWidth()/2, rect.getHeight()/2));
    }
}
