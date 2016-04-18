package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.GeometryUtils;
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
    private static final float SUBDIVS = 4f;

    private final AffineTransform transform;
    private Vector2 location, origin, startPoint;
    private List<Vector2> polyPoints;
    private BodyBuilder bodyBuilder;
    private boolean initial;

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
    }

    @Override
    public void closePath() throws ParseException {
        System.out.println("close");
        if (Math.abs(polyPoints.get(polyPoints.size()-1).x - startPoint.x) > 0.001f
                || Math.abs(polyPoints.get(polyPoints.size()-1).y - startPoint.y) > 0.001f) {
            polyPoints.add(startPoint);
        }
        System.out.println("Position: " + origin);
        for (Vector2 p : polyPoints) {
            p.x = p.x - origin.x;
            p.y = p.y - origin.y;
        }
        List<PolygonShape> shapes = PolyRenderer.createPolyShape(polyPoints);
        for (int i = 0; i < shapes.size(); i++) {
            if (!initial) {
                bodyBuilder.newFixture();
            }
            initial = false;
            bodyBuilder.fixShape(shapes.get(i));
        }
        polyPoints.clear();
        startPoint = null;
    }

    @Override
    public void movetoRel(float x, float y) throws ParseException {
        location.set(location.x + x, location.y + y);
        if (startPoint == null) {
            startPoint = transform(location);
        }
        if (bodyBuilder == null) {
            origin = transform(location);
            bodyBuilder = BodyBuilder.forStatic(origin);
            initial = true;
        }
        //polyPoints.add(new Vector2(x, y));
        System.out.println("movetoRel(" + x + ", " + y + ")");
    }

    @Override
    public void movetoAbs(float x, float y) throws ParseException {
        location.set(x, y);
        if (startPoint == null) {
            startPoint = transform(location);
        }
        if (bodyBuilder == null) {
            origin = transform(location);
            bodyBuilder = BodyBuilder.forStatic(origin);
            initial = true;
        }
        System.out.println("movetoAbs(" + x + ", " + y + ")");
    }

    @Override
    public void linetoRel(float x, float y) throws ParseException {
        System.out.println("linetoRel(" + x + ", " + y + ")");
        location.set(location.x + x, location.y + y);
        polyPoints.add(transform(location));
    }

    @Override
    public void linetoAbs(float x, float y) throws ParseException {
        System.out.println("linetoAbs(" + x + ", " + y + ")");
        location.set(x, y);
        polyPoints.add(transform(location));
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
        Vector2 pStart = transform(location);
        Vector2 p1 = transform(new Vector2(rel.x + x1, rel.y + y1));
        Vector2 p2 = transform(new Vector2(rel.x + x2, rel.y + y2));
        Vector2 pEnd = transform(new Vector2(rel.x + x, rel.y + y));
        location.set(rel.x + x, rel.y + y);

        float len = pEnd.dst(pStart) + pStart.dst(p1) + pEnd.dst(p2);
        Vector2 tmp = new Vector2();
        float sd = (int) (SUBDIVS * len);
        //System.out.println("subdivs " + sd);
        for (int i = 1; i <= sd; i++) {
            Vector2 out = new Vector2();
            Bezier.cubic(out, i/sd, pStart, p1, p2, pEnd, tmp);
            polyPoints.add(out);
        }
    }

    @Override
    public void arcRel(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y) throws ParseException {
        System.out.println(String.format("arcRel(%f, %f, %f, %b, %b, %f, %f)", rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y));
        arcAbs(rx, ry, xAxisRotation, largeArcFlag, sweepFlag, location.x + x, location.y + y);
    }

    @Override
    public void arcAbs(float rx, float ry, float xAxisRotation, boolean largeArcFlag, boolean sweepFlag, float x, float y) throws ParseException {
        System.out.println(String.format("arcAbs(%f, %f, %f, %b, %b, %f, %f)", rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y));
        location.set(x, y);
        polyPoints.add(transform(location));
    }

    private Vector2 transform(Vector2 org) {
        Point2D.Float pOut = new Point2D.Float();
        transform.transform(new Point2D.Float(org.x, org.y), pOut);
        return new Vector2(pOut.x, pOut.y);
    }
}
