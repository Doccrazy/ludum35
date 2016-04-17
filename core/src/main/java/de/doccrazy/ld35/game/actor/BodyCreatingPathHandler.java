package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import de.doccrazy.shared.game.base.PolyRenderer;
import de.doccrazy.shared.game.world.BodyBuilder;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class BodyCreatingPathHandler extends DefaultPathHandler {
    private static final float SUBDIVS = 20f;

    private final AffineTransform transform;
    private Vector2 location, startPoint;
    private List<Vector2> polyPoints;
    private BodyBuilder bodyBuilder;

    public BodyCreatingPathHandler(AffineTransform transform) {
        this.transform = transform;
        location = new Vector2();
        polyPoints = new ArrayList<>();
    }

    public BodyBuilder getBodyBuilder() {
        return bodyBuilder;
    }

    @Override
    public void startPath() throws ParseException {
        System.out.println("start");
        polyPoints.clear();
        location.set(0, 0);
        startPoint = null;
        bodyBuilder = null;
    }

    @Override
    public void endPath() throws ParseException {
        System.out.println("end");
        Point2D.Float pOut = new Point2D.Float();
        for (Vector2 p : polyPoints) {
            transform.transform(new Point2D.Float(p.x, p.y), pOut);
            p.x = pOut.x;
            p.y = pOut.y;
        }
        Vector2 pos = polyPoints.get(0).cpy();
        System.out.println("Position: " + pos);
        for (Vector2 p : polyPoints) {
            p.x = p.x - pos.x;
            p.y = p.y - pos.y;
        }
        bodyBuilder = BodyBuilder.forStatic(pos);
        List<PolygonShape> shapes = PolyRenderer.createPolyShape(polyPoints);
        for (int i = 0; i < shapes.size(); i++) {
            if (i > 0) {
                bodyBuilder.newFixture();
            }
            bodyBuilder.fixShape(shapes.get(i));
        }
    }

    @Override
    public void closePath() throws ParseException {
        System.out.println("close");
        if (Math.abs(polyPoints.get(polyPoints.size()-1).x - startPoint.x) > 0.001f
                && Math.abs(polyPoints.get(polyPoints.size()-1).y - startPoint.y) > 0.001f) {
            polyPoints.add(startPoint);
        }
    }

    @Override
    public void movetoRel(float x, float y) throws ParseException {
        if (startPoint == null) {
            startPoint = new Vector2(x, y);
        }
        location.set(location.x + x, location.y + y);
        //polyPoints.add(new Vector2(x, y));
        System.out.println("movetoRel(" + x + ", " + y + ")");
    }

    @Override
    public void movetoAbs(float x, float y) throws ParseException {
        location.set(x, y);
        System.out.println("movetoAbs(" + x + ", " + y + ")");
    }

    @Override
    public void linetoRel(float x, float y) throws ParseException {
        System.out.println("linetoRel(" + x + ", " + y + ")");
        location.set(location.x + x, location.y + y);
        polyPoints.add(new Vector2(location.x, location.y));
    }

    @Override
    public void linetoAbs(float x, float y) throws ParseException {
        System.out.println("linetoAbs(" + x + ", " + y + ")");
    }

    @Override
    public void curvetoCubicRel(float x1, float y1, float x2, float y2, float x, float y) throws ParseException {
        System.out.println(String.format("curvetoCubicRel(%f, %f, %f, %f, %f, %f)", x1, y1, x2, y2, x, y));
        cubicCurve(location, x1, y1, x2, y2, x, y);
    }

    @Override
    public void curvetoCubicAbs(float x1, float y1, float x2, float y2, float x, float y) throws ParseException {
        System.out.println(String.format("curvetoCubicAbs(%f, %f, %f, %f, %f, %f)", x1, y1, x2, y2, x, y));
        cubicCurve(Vector2.Zero, x1, y1, x2, y2, x, y);
    }

    private void cubicCurve(Vector2 rel, float x1, float y1, float x2, float y2, float x, float y) {
        Vector2 p1 = new Vector2(rel.x + x1, rel.y + y1);
        Vector2 p2 = new Vector2(rel.x + x2, rel.y + y2);
        Vector2 pEnd = new Vector2(rel.x + x, rel.y + y);
        Vector2 out = new Vector2();
        Vector2 tmp = new Vector2();
        for (int i = 1; i <= SUBDIVS; i++) {
            Bezier.cubic(out, i/SUBDIVS, location, p1, p2, pEnd, tmp);
            polyPoints.add(out.cpy());
        }
        location.set(pEnd.x, pEnd.y);
    }
}
