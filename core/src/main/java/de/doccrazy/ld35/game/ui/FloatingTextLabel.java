package de.doccrazy.ld35.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Align;
import de.doccrazy.ld35.core.Resource;
import de.doccrazy.ld35.game.world.FloatingTextEvent;

public class FloatingTextLabel extends Label {
    public static final float DURATION = 1.5f;
    private float stateTime = 0f;
    private int direction;

    public FloatingTextLabel(Stage stage, String text, Vector2 pos, int direction) {
        super(text, new LabelStyle(new BitmapFont(), new Color(1f, 0.7f, 0f, 0.75f)));
        this.direction = direction;
        stage.addActor(this);
        setAlignment(Align.center);
        setPosition(pos.x, pos.y);
    }

    public FloatingTextLabel(UiRoot uiRoot, FloatingTextEvent event) {
        this(uiRoot.getStage(), event.getText(), new Vector2(), 1);
        Vector2 pos = uiRoot.getWorld().stage.stageToScreenCoordinates(new Vector2(event.getX(), event.getY()));
        pos = getStage().screenToStageCoordinates(pos);
        setPosition(pos.x, pos.y);
        if (event.isImportant()) {
            setStyle(new LabelStyle(Resource.FONT.retro, new Color(1, 1, 1, 0.75f)));
        } else if (event.isNegative()) {
            setStyle(new LabelStyle(Resource.FONT.retro, new Color(1, 0.3f, 0.3f, 0.75f)));
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        setY(getY() + direction*delta*20f);
        stateTime += delta;
        setColor(1, 1, 1, Interpolation.exp10Out.apply(MathUtils.clamp((DURATION - stateTime)/ DURATION, 0, 1)));
        if (stateTime > DURATION) {
            remove();
        }
    }
}
