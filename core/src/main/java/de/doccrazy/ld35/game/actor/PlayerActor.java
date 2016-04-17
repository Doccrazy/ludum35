package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.base.CollisionListener;
import de.doccrazy.shared.game.base.KeyboardMovementListener;
import de.doccrazy.shared.game.base.MovementInputListener;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.ShapeBuilder;

public class PlayerActor extends ShapeActor<GameWorld> implements CollisionListener {
    private static final float RADIUS = 0.5f;
    private static final float VELOCITY = 3f;
    private static final float JUMP_IMPULSE = 50f;

    private MovementInputListener movement;
    private boolean moving;
    private float orientation = 1;

    public PlayerActor(GameWorld world, Vector2 spawn) {
        super(world, spawn, false);
        setzOrder(50);
        //setScaleX(Resource.GFX.mower.getWidth() / Resource.GFX.mower.getHeight());
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        return BodyBuilder.forDynamic(spawn).damping(0.05f, 0.05f)
                .fixShape(ShapeBuilder.circle(RADIUS)).fixProps(3f, 0.1f, 1f);
    }

    public void setupKeyboardControl() {
        movement = new KeyboardMovementListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (Input.Keys.NUM_1 == keycode) {
                    body.getFixtureList().get(0).getShape().setRadius(RADIUS/5f);
                } else if (Input.Keys.NUM_2 == keycode) {
                    body.getFixtureList().get(0).getShape().setRadius(RADIUS);
                }
                return super.keyDown(event, keycode);
            }
        };
        addListener((InputListener)movement);
    }

    public void setupController(MovementInputListener movement) {
        this.movement = movement;
    }

    @Override
    protected void doAct(float delta) {
        super.doAct(delta);
        if (movement != null) {
            move(delta);
        }
    }

    private void move(float delta) {
        Vector2 mv = movement.getMovement();
        moving = Math.abs(mv.x) > 0;
        if (moving) {
            orientation = Math.signum(mv.x);
        }

        body.applyTorque(-mv.x*VELOCITY, true);
        if (moving) {
            //if (touchingFloor()) {
                //body.setAngularVelocity(-mv.x*VELOCITY);
            /*} else {
                body.applyForceToCenter(mv.x * AIR_CONTROL, 0f, true);
            }*/
        }
        if (movement.pollJump()) {
            addImpulse(0f, JUMP_IMPULSE);
            //Resource.jump.play();
        }
    }

    private void addImpulse(float impulseX, float impulseY) {
        body.applyLinearImpulse(impulseX, impulseY, body.getPosition().x, body.getPosition().y, true);
        //floorContacts.clear();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {

    }

    private void drawParticle(Batch batch, ParticleEffectPool.PooledEffect effect, Vector2 attach, float rotation) {
        Vector2 center = new Vector2(getX() + getOriginX(), getY() + getOriginY());
        float r = getRotation() + rotation;
        Vector2 p = attach.rotate(r).add(center);
        effect.setPosition(p.x, p.y);
        effect.getEmitters().first().getAngle().setHigh(190 + r, 170 + r);
        effect.update(Gdx.graphics.getDeltaTime());
        effect.draw(batch);
    }

    @Override
    public boolean beginContact(Body me, Body other, Vector2 normal, Vector2 contactPoint) {
        return false;
    }

    @Override
    public void endContact(Body other) {

    }

    @Override
    public void hit(float force) {

    }
}
