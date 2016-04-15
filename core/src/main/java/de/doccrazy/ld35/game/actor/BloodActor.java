package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.WorldActor;

public class BloodActor extends WorldActor<GameWorld> {
    private Sprite sprite = Resource.GFX.blood[MathUtils.random(Resource.GFX.blood.length-1)];

    public BloodActor(GameWorld world, Vector2 spawn) {
        super(world);
        setPosition(spawn.x, spawn.y);
        setSize(0.6f, 0.6f);
        setOrigin(0.3f, 0.3f);
        setzOrder(15);
    }

    @Override
    protected void doAct(float delta) {
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(0.8f, 0, 0, 0.8f);
        batch.draw(sprite, getX(), getY(), getOriginX(), getOriginY(),
                getWidth(), getHeight(), getScaleX(), getScaleY(), getRotation());
        batch.setColor(1, 1, 1, 1);
    }
}
