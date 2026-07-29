package tizio.dev.lle.api.data;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class SpotlightInstance {
    private Vector3f position;
    private Vector3f direction;
    private Vector4f color;
    private float size;
    private float minFadeDist;
    private float maxFadeDist;
    private float beamLength;
    private float beamRadius;
    private float visualBeamLength;
    private int goboIndex;

    public SpotlightInstance(Vector3f position, Vector3f direction, Vector4f color, float size, float minFadeDist, float maxFadeDist, float beamLength, float beamRadius) {
        this(position, direction, color, size, minFadeDist, maxFadeDist, beamLength, beamRadius, -1);
    }

    public SpotlightInstance(Vector3f position, Vector3f direction, Vector4f color, float size, float minFadeDist, float maxFadeDist, float beamLength, float beamRadius, int goboIndex) {
        this.position = position;
        this.direction = direction.normalize();
        this.color = color;
        this.size = size;
        this.minFadeDist = minFadeDist;
        this.maxFadeDist = maxFadeDist;
        this.beamLength = beamLength;
        this.beamRadius = beamRadius;
        this.visualBeamLength = beamLength;
        this.goboIndex = goboIndex;
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getDirection() { return direction; }
    public Vector4f getColor() { return color; }
    public float getSize() { return size; }
    public float getMinFadeDist() { return minFadeDist; }
    public float getMaxFadeDist() { return maxFadeDist; }
    public float getBeamLength() { return beamLength; }
    public float getBeamRadius() { return beamRadius; }
    public float getVisualBeamLength() { return visualBeamLength; }
    public int getGoboIndex() { return goboIndex; }

    public void setPosition(Vector3f position) { this.position = position; }
    public void setDirection(Vector3f direction) { this.direction = direction.normalize(); }
    public void setVisualBeamLength(float visualBeamLength) { this.visualBeamLength = visualBeamLength; }
    public void setGoboIndex(int goboIndex) { this.goboIndex = goboIndex; }
}