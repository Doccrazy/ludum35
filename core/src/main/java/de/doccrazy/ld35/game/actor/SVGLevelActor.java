package de.doccrazy.ld35.game.actor;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.data.GameRules;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.svg.SVGLayer;

import java.util.ArrayList;
import java.util.List;

public class SVGLevelActor extends Level {
    public static final String LAYER_PHYSICS = "Physics";
    public static final String LAYER_META = "Meta";
    public static final String LABEL_SCREEN = "screen";
    public static final String LABEL_SPAWN = "spawn";
    public static final String PREFIX_PARTICLE = "part:";

    private final Rectangle dimensions, cameraBounds;
    private final Vector2 spawn;
    private final TextureRegion levelTexture;
    private final List<Body> bodies = new ArrayList<>();
    private final List<ParticleEffectPool.PooledEffect> particles = new ArrayList<>();

    public SVGLevelActor(GameWorld world, XmlReader.Element levelElement, TextureRegion levelTexture) {
        super(world);
        this.levelTexture = levelTexture;

        SVGLayer rootLayer = new SVGLayer(levelElement);
        Vector2 cameraBoundsForScale = rootLayer.getLayerByLabel(LAYER_META).getRectSizeImmediate(LABEL_SCREEN);
        float scale = GameRules.LEVEL_HEIGHT / cameraBoundsForScale.y;

        rootLayer.applyScale(scale);
        dimensions = rootLayer.getDimensionsTransformed();

        SVGLayer metaLayer = rootLayer.getLayerByLabel(LAYER_META);

        spawn = metaLayer.getRectCenter(LABEL_SPAWN);
        Vector2[] boundsPoly = metaLayer.getRectAsPoly(LABEL_SCREEN);
        cameraBounds = new Rectangle(boundsPoly[0].x, boundsPoly[0].y, boundsPoly[2].x - boundsPoly[0].x, boundsPoly[2].y - boundsPoly[0].y);

        SVGLayer physicsLayer = rootLayer.getLayerByLabel(LAYER_PHYSICS);
        physicsLayer.createPhysicsBodiesRecursive(bodyBuilder -> bodies.add(bodyBuilder.build(world)));

        metaLayer.processRectCenterByPrefix(PREFIX_PARTICLE, (type, center, color) -> {
            ParticleEffectPool.PooledEffect particle = Resource.GFX.particles.get(type).obtain();
            particle.setPosition(center.x, center.y);
            particles.add(particle);
        });
        metaLayer.processRectAsPolyByPrefix("kill", (s, rect, color) -> {
            world.addActor(new KillboxActor(world, rect));
        });
        metaLayer.processRectAsPolyByPrefix("win", (s, rect, color) -> {
            world.addActor(new WinboxActor(world, rect));
        });
        metaLayer.processCircleByPrefix("light", (s, circle, color) -> {
            PointLight light = new PointLight(world.rayHandler, 10, color, circle.radius*2f, circle.x, circle.y);
            light.setXray(true);
            lights.add(light);
        });
        metaLayer.processArcByPrefix("conelight", (s, arc, color) -> {
            ConeLight light = new ConeLight(world.rayHandler, 10, color, arc.r*7f, arc.x, arc.y, MathUtils.radDeg * (arc.a2 + arc.a1)/2f, MathUtils.radDeg * Math.abs(arc.a2 - arc.a1)/2f);
            light.setXray(true);
            lights.add(light);
        });
    }

    @Override
    public Rectangle getBoundingBox() {
        return dimensions;
    }

    @Override
    public Rectangle getViewportBox() {
        return cameraBounds;
    }

    @Override
    public Vector2 getSpawn() {
        return spawn;
    }

    @Override
    public int getScoreGoal() {
        return 10000;
    }

    @Override
    public float getTime() {
        return 300;
    }

    @Override
    protected void doAct(float delta) {
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(levelTexture, 0, 0, dimensions.width, dimensions.height);
        for (ParticleEffectPool.PooledEffect particle : particles) {
            drawParticle(batch, particle);
        }
    }

    private void drawParticle(Batch batch, ParticleEffectPool.PooledEffect effect) {
        effect.update(Gdx.graphics.getDeltaTime());
        effect.draw(batch);
    }

    @Override
    protected void doRemove() {
        for (Body b : bodies) {
            world.box2dWorld.destroyBody(b);
        }
        for (ParticleEffectPool.PooledEffect p : particles) {
            p.free();
        }
        super.doRemove();
    }
}
