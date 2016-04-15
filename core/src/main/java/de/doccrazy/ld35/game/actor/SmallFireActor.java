package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.Vector2;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.WorldActor;

public class SmallFireActor extends WorldActor<GameWorld> {
    private final ParticleEffectPool.PooledEffect fire = Resource.GFX.partFire.obtain();

    public SmallFireActor(GameWorld world, Vector2 spawn) {
        super(world);
        setPosition(spawn.x, spawn.y);
        setSize(0.5f, 0.5f);
        setOrigin(0.25f, 0.25f);
        setzOrder(60);
        fire.scaleEffect(0.5f);
        fire.start();
    }

    @Override
    protected void doAct(float delta) {
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        fire.setPosition(getX() + getOriginX(), getY() + getOriginY());
        fire.update(Gdx.graphics.getDeltaTime());
        fire.draw(batch);
    }

}
