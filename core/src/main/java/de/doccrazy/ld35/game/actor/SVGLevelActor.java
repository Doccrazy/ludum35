package de.doccrazy.ld35.game.actor;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.PolygonShape;
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
        spawn = parseRectGetCenter(spawnRect, metaTransform);
        Vector2[] boundsPoly = parseRectAsPoly(childByLabel(metaLayer, "rect", LABEL_SCREEN), metaTransform);
        cameraBounds = new Rectangle(boundsPoly[0].x, boundsPoly[0].y, boundsPoly[2].x - boundsPoly[0].x, boundsPoly[2].y - boundsPoly[0].y);

        processRectCenterByPrefix(metaLayer, metaTransform, PREFIX_PARTICLE, (type, center) -> {
            ParticleEffectPool.PooledEffect particle = Resource.GFX.particles.get(type).obtain();
            particle.setPosition(center.x, center.y);
            particles.add(particle);
        });
        processRectAsPolyByPrefix(metaLayer, metaTransform, "kill", (s, rect) -> {
            world.addActor(new KillboxActor(world, rect));
        });
        processRectAsPolyByPrefix(metaLayer, metaTransform, "win", (s, rect) -> {
            world.addActor(new WinboxActor(world, rect));
        });
        processCircleByPrefix(metaLayer, metaTransform, "light", (s, circle, color) -> {
            PointLight light = new PointLight(world.rayHandler, 10, color, circle.radius*2f, circle.x, circle.y);
            light.setXray(true);
            lights.add(light);
        });
        processArcByPrefix(metaLayer, metaTransform, "conelight", (s, arc, color) -> {
            ConeLight light = new ConeLight(world.rayHandler, 10, color, arc.r*7f, arc.x, arc.y, MathUtils.radDeg * (arc.a2 + arc.a1)/2f, MathUtils.radDeg * Math.abs(arc.a2 - arc.a1)/2f);
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
            Vector2[] parsed = parseRectAsPoly(rect, groupTransform);
            BodyBuilder builder = BodyBuilder.forStatic(parsed[0])
                    .fixShape(ShapeBuilder.polyRel(parsed));
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

    private void processRectCenterByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, BiConsumer<String, Vector2> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "rect", prefix)) {
            Vector2 center = parseRectGetCenter(element, currentTransform);
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, center);
        }
    }

    private void processRectAsPolyByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, BiConsumer<String, Vector2[]> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "rect", prefix)) {
            Vector2[] rect = parseRectAsPoly(element, currentTransform);
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, rect);
        }
    }

    private void processCircleByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, TriConsumer<String, Circle, Color> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "circle", prefix)) {
            Circle circle = parseCircle(element, currentTransform);
            Color color = colorFromStyle(element);
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, circle, color);
        }
    }

    private void processArcByPrefix(XmlReader.Element layer, AffineTransform currentTransform, String prefix, TriConsumer<String, Arc, Color> consumer) {
        for (XmlReader.Element element : childrenByPrefix(layer, "path", prefix)) {
            Arc arc = parseArc(element, currentTransform);
            Color color = colorFromStyle(element);
            String objectName = element.getAttribute(ATTR_LABEL).substring(prefix.length());
            consumer.accept(objectName, arc, color);
        }
    }

    private Color colorFromStyle(XmlReader.Element element) {
        String[] style = element.getAttribute("style").split(";");
        Color color = new Color();
        for (String s : style) {
            if (s.startsWith("fill:#")) {
                color.set(Color.valueOf(s.substring("fill:#".length())));
            } else if (s.startsWith("fill-opacity:")) {
                color.set(color.r, color.g, color.b, Float.parseFloat(s.substring("fill-opacity:".length())));
            }
        }
        return color;
    }

    private Vector2 parseRectGetCenter(XmlReader.Element rect, AffineTransform currentTransform) {
        Vector2[] r = parseRectAsPoly(rect, currentTransform);
        return r[0].lerp(r[2], 0.5f);
    }

    /**
     * @return [bottom left, top left, top right, bottom right]
     */
    private Vector2[] parseRectAsPoly(XmlReader.Element rect, AffineTransform currentTransform) {
        AffineTransform rectTransform = createTransform(rect);
        rectTransform.preConcatenate(currentTransform);
        Vector2[] p = new Vector2[4];
        p[0] = new Vector2(Float.parseFloat(rect.getAttribute("x")), Float.parseFloat(rect.getAttribute("y")));
        p[2] = new Vector2(p[0].x + Float.parseFloat(rect.getAttribute("width")), p[0].y + Float.parseFloat(rect.getAttribute("height")));
        p[1] = new Vector2(p[0].x, p[2].y);
        p[3] = new Vector2(p[2].x, p[0].y);
        applyTransform(rectTransform, p[0]);
        applyTransform(rectTransform, p[1]);
        applyTransform(rectTransform, p[2]);
        applyTransform(rectTransform, p[3]);
        return p;
    }

    private Circle parseCircle(XmlReader.Element circle, AffineTransform currentTransform) {
        AffineTransform circleTransform = createTransform(circle);
        circleTransform.preConcatenate(currentTransform);
        Vector2 c = new Vector2(Float.parseFloat(circle.getAttribute("cx")), Float.parseFloat(circle.getAttribute("cy")));
        float r = Float.parseFloat(circle.getAttribute("r"));
        applyTransform(circleTransform, c);
        r = applyDeltaTransform(circleTransform, r);
        return new Circle(c, Math.abs(r));
    }

    private Arc parseArc(XmlReader.Element arc, AffineTransform currentTransform) {
        AffineTransform arcTransform = createTransform(arc);
        arcTransform.preConcatenate(currentTransform);
        Vector2 c = new Vector2(Float.parseFloat(arc.getAttribute("sodipodi:cx")), Float.parseFloat(arc.getAttribute("sodipodi:cy")));
        float r = Float.parseFloat(arc.getAttribute("sodipodi:rx"));
        float a1 = Float.parseFloat(arc.getAttribute("sodipodi:start"));
        float a2 = Float.parseFloat(arc.getAttribute("sodipodi:end"));
        applyTransform(arcTransform, c);
        r = applyDeltaTransform(arcTransform, r);
        Vector2 vMirror = applyDeltaTransform(arcTransform, new Vector2(1, 1));
        if (vMirror.x < 0) {
            a1 = (float) (Math.PI - a1);
            a2 = (float) (Math.PI - a2);
        }
        if (vMirror.y < 0) {
            a1 = -a1;
            a2 = -a2;
        }
        return new Arc(c.x, c.y, Math.abs(r), a1, a2);
    }

    private void applyTransform(AffineTransform transform, Vector2 v) {
        Point2D tp = transform.transform(new Point2D.Float(v.x, v.y), new Point2D.Float());
        v.set((float)tp.getX(), (float)tp.getY());
    }

    private float applyDeltaTransform(AffineTransform transform, float l) {
        Point2D tp = transform.deltaTransform(new Point2D.Float(l, 0), new Point2D.Float());
        return (float) tp.getX();
    }

    private Vector2 applyDeltaTransform(AffineTransform transform, Vector2 v) {
        Point2D tp = transform.deltaTransform(new Point2D.Float(v.x, v.y), new Point2D.Float());
        return new Vector2((float) tp.getX(), (float) tp.getY());
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

class Arc {
    public float x, y;
    public float r;
    public float a1, a2;

    public Arc(float x, float y, float r, float a1, float a2) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.a1 = a1;
        this.a2 = a2;
    }
}