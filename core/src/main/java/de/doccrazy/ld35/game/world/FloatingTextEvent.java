package de.doccrazy.ld35.game.world;

import de.doccrazy.shared.game.event.Event;

public class FloatingTextEvent extends Event {
    private final String text;
    private final boolean important;
    private final boolean negative;

    public FloatingTextEvent(float x, float y, String text, boolean important, boolean negative) {
        super(x, y);
        this.text = text;
        this.important = important;
        this.negative = negative;
    }

    public String getText() {
        return text;
    }

    public boolean isImportant() {
        return important;
    }

    public boolean isNegative() {
        return negative;
    }
}
