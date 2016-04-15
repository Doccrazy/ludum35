package de.doccrazy.ld35.game.world;

import com.badlogic.gdx.math.MathUtils;

public class RandomEvent {
    private float minTime, maxTime;
    private float nextTime;

    public RandomEvent(float minTime, float maxTime) {
        this.minTime = minTime;
        this.maxTime = maxTime;
        nextTime = MathUtils.random(minTime, maxTime);
    }

    public boolean apply(float delta) {
        nextTime -= delta;
        if (nextTime <= 0) {
            nextTime = MathUtils.random(minTime, maxTime);
            return true;
        }
        return false;
    }

    public void setMaxTime(float maxTime) {
        nextTime += (maxTime - this.maxTime);
        this.maxTime = maxTime;
    }
}
