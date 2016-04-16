package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.XmlReader;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.data.GameRules;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;
import org.apache.batik.parser.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

public class SVGLevelActor extends Level {
    private final Rectangle dimensions, cameraBounds;
    private final Vector2 spawn;
    private final TextureRegion levelTexture;

    public SVGLevelActor(GameWorld world, XmlReader.Element levelElement, TextureRegion levelTexture) {
        super(world);
        this.levelTexture = levelTexture;

        XmlReader.Element physicsLayer = childByLabel(levelElement, "g", "Physics");
        XmlReader.Element metaLayer = childByLabel(levelElement, "g", "Meta");

        //read "screen" meta rect to adjust level scale accordingly
        XmlReader.Element cameraBoundsForScale = childByLabel(metaLayer, "rect", "screen");
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
        XmlReader.Element spawnRect = childByLabel(metaLayer, "rect", "spawn");
        if (spawnRect == null) {
            throw new IllegalArgumentException("Please create a rect with label 'spawn' on the Meta layer");
        }
        spawn = parseRect(spawnRect, metaTransform).getCenter(new Vector2());
        cameraBounds = parseRect(childByLabel(metaLayer, "rect", "screen"), metaTransform);
    }

    private void parseGroup(XmlReader.Element group, AffineTransform currentTransform) {
        AffineTransform groupTransform = createTransform(group);
        groupTransform.preConcatenate(currentTransform);

        PathParser parser = new PathParser();
        parser.setPathHandler(new BodyCreatingPathHandler(world, groupTransform));
        for (XmlReader.Element path : group.getChildrenByName("path")) {
            parser.parse(path.getAttribute("d"));
        }
        for (XmlReader.Element rect : group.getChildrenByName("rect")) {
            Rectangle parsed = parseRect(rect, groupTransform);
            BodyBuilder.forStatic(parsed.getPosition(new Vector2()))
                    .fixShape(ShapeBuilder.boxAbs(parsed.getWidth(), parsed.getHeight()))
                    .build(world);
        }
        for (XmlReader.Element subgroup : group.getChildrenByName("g")) {
            parseGroup(subgroup, groupTransform);
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
            if (label.equals(element.getAttribute("inkscape:label"))) {
                return element;
            }
        }
        return null;
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

    private void applyTransform(AffineTransform transform, Vector2 v) {
        Point2D tp = transform.transform(new Point2D.Float(v.x, v.y), new Point2D.Float());
        v.set((float)tp.getX(), (float)tp.getY());
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
    }
}
