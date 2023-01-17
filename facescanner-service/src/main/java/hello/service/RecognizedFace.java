package hello.service;

import java.util.UUID;

public class RecognizedFace {
    private int x;
    private int y;
    private int width;
    private int height;
    private UUID id;

    public RecognizedFace(int x, int y, int width, int height, UUID id) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public UUID getId() {
        return id;
    }

}
