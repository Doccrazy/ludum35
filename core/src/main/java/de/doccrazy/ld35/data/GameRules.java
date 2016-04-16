package de.doccrazy.ld35.data;

import com.badlogic.gdx.math.Vector2;

public class GameRules {
    public static final Vector2 GRAVITY = new Vector2(0, -9.81f);

    //width of the visible level area on screen
    public static final int LEVEL_WIDTH = 16;
    public static final int LEVEL_HEIGHT = (int)(LEVEL_WIDTH*9f/16f);
}
