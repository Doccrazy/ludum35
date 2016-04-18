package de.doccrazy.ld35.game.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.GameWorld;
import de.doccrazy.shared.game.actor.ParticleActor;
import de.doccrazy.shared.game.actor.ParticleEvent;
import de.doccrazy.shared.game.actor.ShapeActor;
import de.doccrazy.shared.game.base.CollisionListener;
import de.doccrazy.shared.game.base.KeyboardMovementListener;
import de.doccrazy.shared.game.base.MovementInputListener;
import de.doccrazy.shared.game.world.BodyBuilder;
import de.doccrazy.shared.game.world.GameState;
import de.doccrazy.shared.game.world.ShapeBuilder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PlayerActor extends ShapeActor<GameWorld> implements CollisionListener {
    private static final float CONTACT_TTL = 0.2f;
    private static final float RADIUS = 0.5f;
    private static final float VELOCITY = 5f;
    private static final float TORQUE = 2f;
    private static final float JUMP_IMPULSE = 4f;
    private static final float AIR_CONTROL = 3f;
    public static final float V_MAX_AIRCONTROL = 2.5f;
    public static final float V_MAX_GLIDE_DROP = -2f;
    public static final float V_MAX_ROLL = 40f;
    public static final float GLIDE_V_SCALE = 0.01f;

    private MovementInputListener movement;
    private boolean moving;
    private float orientation = 1;
    private int shapeState;
    private float lastJump = 0;
    private boolean touchingFloor, touchingLeftWall, touchingRightWall;
    private float lastFloorContact, lastLeftWallContact, lastRightWallContact;

    public PlayerActor(GameWorld world, Vector2 spawn) {
        super(world, spawn, false);
        setzOrder(50);
        //setScaleX(Resource.GFX.mower.getWidth() / Resource.GFX.mower.getHeight());
    }

    @Override
    protected void init() {
        super.init();
        setShapeState(0);
    }

    @Override
    protected BodyBuilder createBody(Vector2 spawn) {
        return BodyBuilder.forDynamic(spawn)
                .fixShape(ShapeBuilder.circle(RADIUS)).fixProps(3f, 0.1f, 1f);
    }

    public void setupKeyboardControl() {
        movement = new KeyboardMovementListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (Input.Keys.NUM_1 == keycode) {
                    setShapeState(0);
                    return true;
                }
                if (Input.Keys.NUM_2 == keycode) {
                    setShapeState(1);
                    return true;
                }
                if (Input.Keys.NUM_3 == keycode) {
                    setShapeState(2);
                    return true;
                }
                return super.keyDown(event, keycode);
            }
        };
        addListener((InputListener)movement);
    }

    private void setShapeState(int state) {
        shapeState = state;
        switch (state) {
            case 0:
                body.getFixtureList().get(0).getShape().setRadius(RADIUS*0.9f);
                body.resetMassData();
                body.getFixtureList().get(0).setRestitution(0.1f);
                body.setLinearDamping(0.2f);
                body.setAngularDamping(0.8f);
                body.setFixedRotation(false);
                body.setGravityScale(1f);
                setUseRotation(false);
                setRotation(0);
                break;
            case 1:
                body.getFixtureList().get(0).getShape().setRadius(RADIUS);
                body.resetMassData();
                body.getFixtureList().get(0).setRestitution(0.1f);
                body.setLinearDamping(0.05f);
                body.setAngularDamping(0.05f);
                body.setFixedRotation(false);
                body.setGravityScale(1f);
                setUseRotation(true);
                break;
            case 2:
                body.getFixtureList().get(0).getShape().setRadius(RADIUS/5f);
                body.resetMassData();
                body.getFixtureList().get(0).setRestitution(0f);
                body.setLinearDamping(0.01f);
                body.setAngularDamping(0.8f);
                body.setFixedRotation(true);
                body.setGravityScale(0.1f);
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(v.x, MathUtils.clamp(v.y, -1f, 1f));
                setUseRotation(false);
                setRotation(0);
                break;
        }
        body.setAwake(true);
        setScaleY(Resource.GFX.player[shapeState].getHeight() / Resource.GFX.player[shapeState].getWidth());
    }

    public void setupController(MovementInputListener movement) {
        this.movement = movement;
    }

    @Override
    protected void doAct(float delta) {
        processContacts();
        if (movement != null && world.getGameState() == GameState.GAME) {
            move(delta);
        } else {
            body.setAngularVelocity(0);
        }
        if (body.getPosition().y + body.getFixtureList().get(0).getShape().getRadius() < world.getLevel().getBoundingBox().y) {
            kill();
        }
        super.doAct(delta);
    }

    private void processContacts() {
        for (Contact contact : world.box2dWorld.getContactList()) {
            Body a = contact.getFixtureA().getBody();
            Body b = contact.getFixtureB().getBody();
            Body other = a.getUserData() == this ? b : (b.getUserData() == this ? a : null);
            if (other == null) {
                continue;
            }

            Vector2 normal = contact.getWorldManifold().getNormal();
            if (other.getType() == BodyDef.BodyType.StaticBody && contact.isTouching() && normal.y > 0.707f) {  //45°
                touchingFloor = true;
                lastFloorContact = stateTime;
            } else if (other.getType() == BodyDef.BodyType.StaticBody && contact.isTouching() && normal.x > 0.866f) {  //60°
                touchingLeftWall = true;
                lastLeftWallContact = stateTime;
            } else if (other.getType() == BodyDef.BodyType.StaticBody && contact.isTouching() && normal.x < -0.866f) {  //60°
                touchingRightWall = true;
                lastRightWallContact = stateTime;
            }
        }
        if (stateTime - lastFloorContact > CONTACT_TTL) {
            touchingFloor = false;
        }
        if (stateTime - lastLeftWallContact > CONTACT_TTL) {
            touchingLeftWall = false;
        }
        if (stateTime - lastRightWallContact > CONTACT_TTL) {
            touchingRightWall = false;
        }
        //System.out.println("Floor: " + touchingFloor + ", wallLeft: " + touchingLeftWall + ", wallRight: " + touchingRightWall);
    }

    private void move(float delta) {
        Vector2 mv = movement.getMovement();
        moving = Math.abs(mv.x) > 0;
        if (moving) {
            orientation = Math.signum(mv.x);
        }

        Vector2 v = body.getLinearVelocity();
        if (shapeState == 0) {
            if (mv.x == 0 || Math.signum(mv.x) == Math.signum(orientation)) {
                //System.out.println(touchingFloor());
                if (touchingFloor) {
                    body.setAngularVelocity(-mv.x*VELOCITY);
                } else {
                    if (Math.abs(v.x) < V_MAX_AIRCONTROL) {
                        body.applyForceToCenter(mv.x * AIR_CONTROL, 0f, true);
                    }
                }
            }
            boolean jump = movement.pollJump();
            if (stateTime - lastJump > CONTACT_TTL && jump) {
                if (touchingFloor) {
                    addImpulse(0f, JUMP_IMPULSE);
                    lastJump = stateTime;
                    //Resource.jump.play();
                } else if (touchingLeftWall && mv.x > 0) {
                    body.applyLinearImpulse(JUMP_IMPULSE/3f, JUMP_IMPULSE, body.getPosition().x, body.getPosition().y, true);
                    lastJump = stateTime;
                } else if (touchingRightWall && mv.x < 0) {
                    body.applyLinearImpulse(-JUMP_IMPULSE/3f, JUMP_IMPULSE, body.getPosition().x, body.getPosition().y, true);
                    lastJump = stateTime;
                }
            }

        } else if (shapeState == 1) {
            if (Math.abs(body.getAngularVelocity()) < V_MAX_ROLL) {
                body.applyTorque(-mv.x * TORQUE, true);
            }
            if (v.len() < 0.1f) {
                body.setAngularVelocity(-mv.x*VELOCITY*0.5f);
            }
        } else if (shapeState == 2) {
            if (v.y < V_MAX_GLIDE_DROP) {
                body.setLinearVelocity(v.x, V_MAX_GLIDE_DROP);
            }
            body.applyForceToCenter(0, mv.y * Math.abs(v.x) * GLIDE_V_SCALE, true);
        }
    }

    private void addImpulse(float impulseX, float impulseY) {
        body.applyLinearImpulse(impulseX, impulseY, body.getPosition().x, body.getPosition().y, true);
        //floorContacts.clear();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        drawRegion(batch, Resource.GFX.player[shapeState]);
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
        return true;
    }

    @Override
    public void endContact(Body other) {
    }

    @Override
    public void hit(float force) {

    }

    public void damage(float amount) {
        world.postEvent(new ParticleEvent(body.getPosition().x, body.getPosition().y, Resource.GFX.particles.get("explosion")));
        kill();
    }
}
