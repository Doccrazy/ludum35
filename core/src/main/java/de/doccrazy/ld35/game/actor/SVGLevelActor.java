package de.doccrazy.ld35.game.actor;

import box2dLight.PointLight;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.data.GameRules;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;
import org.apache.batik.parser.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SVGLevelActor extends Level {
    public static final String LAYER_PHYSICS = "Physics";
    public static final String LAYER_META = "Meta";
    public static final String ATTR_LABEL = "inkscape:label";
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

        XmlReader.Element physicsLayer = childByLabel(levelElement, "g", LAYER_PHYSICS);
        XmlReader.Element metaLayer = childByLabel(levelElement, "g", LAYER_META);

        //read "screen" meta rect to adjust level scale accordingly
        XmlReader.Element cameraBoundsForScale = childByLabel(metaLayer, "rect", LABEL_SCREEN);
        if (cameraBoundsForScale == null) {
            throw new IllegalArgumentException("Please create a rect with label 'screen' on the Meta layer");
        }
        float scale = GameRules.LEVEL_HEIGHT / Float.parseFloat(cameraBoundsForScale.getAttribute("height"));

        //parse dimensions (size of full level)
        String[] dimStr = levelElement.get("viewBox").split(" ");
        dimensions = new Rectangle(Float.parseFloat(dimStr[0]) * scale, Float.parseFloat(dimStr[1]) * scale, Float.parseFloat(dimStr[2]) * scale, Float.parseFloat(dimStr[3]) * scale);

        //flip y and scale to physics units (meters)
        AffineTransform viewMatrix = new AffineTransform();
        viewMatrix.translate(0f, dimensions.height/2);
        viewMatrix.scale(1f, -1f);
        viewMatrix.translate(0f, -dimensions.height/2);
        viewMatrix.scale(scale, scale);

        //read physics polygons
        parseGroup(physicsLayer, viewMatrix);

        //create metadata transform
        AffineTransform metaTransform = createTransform(metaLayer);
        metaTransform.preConcatenate(viewMatrix);

        //read metadata
        XmlReader.Element spawnRect = childByLabel(metaLayer, "rect", LABEL_SPAWN);
        if (spawnRect == null) {
            throw new IllegalArgumentException("Please create a rect with label 'spawn' on the Meta layer");
        }
        spawn = parseRect(spawnRect, metaTransform).getCenter(new Vector2());
        cameraBounds = parseRect(childByLabel(metaLayer, "rect", LABEL_SCREEN), metaTransform);
        processRectByPrefix(metaLayer, metaTransform, PREFIX_PARTICLE, (type, rect) -> {
            ParticleEffectPool.PooledEffect particle = Resource.GFX.particles.get(type).obtain();
            Vector2 center = rect.getCenter(new Vector2());
            particle.setPosition(center.x, center.y);
            particles.add(particle);
        });
        processRectByPrefix(metaLayer, metaTransform, "kill", (s, rect) -> {
            world.addActor(new KillboxActor(world, rect));
        });
        processCircleByPrefix(metaLayer, metaTransform, "light", (s, circle, color) -> {
            PointLight light = new PointLight(world.rayHandler, 10, color, circle.radius*2f, circle.x, circle.y);
            light.setXray(true);
            lights.add(light);
        });
    }

    private void parseGroup(XmlReader.Element group, AffineTransform currentTransform) {
        AffineTransform groupTransform = createTransform(group);
        groupTransform.preConcatenate(currentTransform);

        PathParser parser = new PathParser();
        for (XmlReader.Element path : group.getChildrenByName("path")) {
            AffineTransform pathTransform = createTransform(path);
            pathTransform.preConcatenate(groupTransform);
            BodyCreatingPathHandler handler = new BodyCreatingPathHandler(pathTransform);
            parser.setPathHandler(handler);

            parser.parse(path.getAttribute("d"));
            BodyBuilder builder = handler.getBodyBuilder();
            applyPhysicsProps(path, builder);
            bodies.add(builder.build(world));
        }
        for (XmlReader.Element rect : group.getChildrenByName("rect")) {
            Rectangle parsed = parseRect(rect, groupTransform);
            BodyBuilder builder = BodyBuilder.forStatic(parsed.getPosition(new Vector2()))
                    .fixShape(ShapeBuilder.boxAbs(parsed.getWidth(), parsed.getHeight()));
            applyPhysicsProps(rect, builder);
            bodies.add(builder.build(world));
        }
        for (XmlReader.Element subgroup : group.getChildrenByName("g")) {
            parseGroup(subgroup, groupTransform);
        }
    }

    private void applyPhysicsProps(XmlReader.Element rect, BodyBuilder builder) {
        try {
            for (String desc : rect.get("desc").split(";")) {
                if (desc.startsWith("fp:")) {
                    String[] fp = desc.substring("fp:".length()).split(",");
                    builder.fixProps(Float.parseFloat(fp[0]), Float.parseFloat(fp[1]), Float.parseFloat(fp[2]));
                }
            }
        } catch (GdxRuntimeException ignore) {
        }
    }

    private AffineTransform createTransform(XmlReader.Element element) {
        String t;
        try {
            t = element.getAttribute("transform");
        } catch (Exception e) {
            return new AffineTransform();
        }
        TransformListParser transformParser = new TransformListParser();
        AWTTransformProducer transformProducer = new AWTTransformProducer();
        transformParser.setTransformListHandler(transformProducer);

        transformParser.parse(t);
        return transformProducer.getAffineTransform();
    }

    private XmlReader.Element childByLabel(XmlReader.Element layer, String type, String label) {
        for (XmlReader.Element element : layer.getChildrenByName(type)) {
            if (label.equals(element.getAttribute(ATTR_LABEL))) {
                return element;
            }
        }
        return null;
    }

    private List<XmlReader.Element> childrenByPrefix(XmlReader.Element layer, String type, String prefix) {
        return StreamSupport.stream(layer.getChildrenByName(type).spliterator(), false)
                .filter(e -> hasAttribute(e, ATTR_LABEL) && e.getAttribute(ATTR_LABEL).startsWith(prefix))
                .collect(Collectors.toList());
    }

    private void processRectByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, BiConsumer<String, Rectangle> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "rect", prefix)) {
            Rectangle rect = parseRect(element, currentTransform);
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, rect);
        }
    }

    private void processCircleByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, TriConsumer<String, Circle, Color> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "circle", prefix)) {
            Circle circle = parseCircle(element, currentTransform);
            String[] style = element.getAttribute("style").split(";");
            Color color = new Color();
            for (String s : style) {
                if (s.startsWith("fill:#")) {
                    color.set(Color.valueOf(s.substring("fill:#".length())));
                } else if (s.startsWith("fill-opacity:")) {
                    color.set(color.r, color.g, color.b, Float.parseFloat(s.substring("fill-opacity:".length())));
                }
            }
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, circle, color);
        }
    }

    private Rectangle parseRect(XmlReader.Element rect, AffineTransform currentTransform) {
        AffineTransform rectTransform = createTransform(rect);
        Vector2 p1 = new Vector2(Float.parseFloat(rect.getAttribute("x")), Float.parseFloat(rect.getAttribute("y")));
        Vector2 p2 = new Vector2(p1.x + Float.parseFloat(rect.getAttribute("width")), p1.y + Float.parseFloat(rect.getAttribute("height")));
        applyTransform(rectTransform, p1);
        applyTransform(rectTransform, p2);
        applyTransform(currentTransform, p1);
        applyTransform(currentTransform, p2);
        float tmp;
        if (p1.x > p2.x) { tmp = p1.x; p1.x = p2.x; p2.x = tmp; }
        if (p1.y > p2.y) { tmp = p1.y; p1.y = p2.y; p2.y = tmp; }
        return new Rectangle(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
    }

    private Circle parseCircle(XmlReader.Element circle, AffineTransform currentTransform) {
        AffineTransform circleTransform = createTransform(circle);
        Vector2 c = new Vector2(Float.parseFloat(circle.getAttribute("cx")), Float.parseFloat(circle.getAttribute("cy")));
        float r = Float.parseFloat(circle.getAttribute("r"));
        applyTransform(circleTransform, c);
        applyTransform(currentTransform, c);
        r = applyDeltaTransform(circleTransform, r);
        r = applyDeltaTransform(currentTransform, r);
        return new Circle(c, r);
    }

    private void applyTransform(AffineTransform transform, Vector2 v) {
        Point2D tp = transform.transform(new Point2D.Float(v.x, v.y), new Point2D.Float());
        v.set((float)tp.getX(), (float)tp.getY());
    }

    private float applyDeltaTransform(AffineTransform transform, float l) {
        Point2D tp = transform.deltaTransform(new Point2D.Float(l, 0), new Point2D.Float());
        return (float) tp.getX();
    }

    private boolean hasAttribute(XmlReader.Element element, String attribute) {
        String val;
        try {
            val = element.getAttribute(attribute);
        } catch (GdxRuntimeException e) {
            return false;
        }
        return val != null && val.length() > 0;
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
