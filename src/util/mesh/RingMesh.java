package util.mesh;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

/**
 *
 * @author Volker Schuller
 */
public class RingMesh extends Mesh {

    public RingMesh(float radius1, float radius2, int precision, boolean close, float repeat) {
        boolean circle = (radius1 <= 0) && close;
        if (circle) {
            circle(radius2, precision, repeat);
            return;
        }
        final int vertNum = precision * 2 + (close ? 0 : 2);
        Vector3f[] vertices = new Vector3f[vertNum];
        Vector2f[] texCoord = new Vector2f[vertNum];
        int[] indexes = new int[precision * 6];
        float[] normals = new float[vertNum * 3];
        float factor = 2 * FastMath.PI / precision;
        Vector3f B = new Vector3f(FastMath.cos(factor * 0.5f) * radius2, 0, FastMath.sin(factor * 0.5f) * radius2);
        Vector3f D = new Vector3f(FastMath.cos(factor) * radius1, 0, FastMath.sin(factor) * radius1);
        texCoord[0] = new Vector2f(0, 0);
        texCoord[1] = new Vector2f(1, 0);
        vertices[0] = B;
        vertices[1] = D;
        int max = precision * 2 + (close ? -1 : 1);
        float rstep = repeat / max;
        for (int i = 2; i < max; i += 2) {
            float bAngle = factor * ((i >> 1) + 0.5f);
            B = new Vector3f(FastMath.cos(bAngle) * radius2, 0, FastMath.sin(bAngle) * radius2);
            vertices[i] = B;
            texCoord[i] = new Vector2f(0, i * rstep);
            float dAngle = factor * ((i >> 1) + 1);
            D = new Vector3f(FastMath.cos(dAngle) * radius1, 0, FastMath.sin(dAngle) * radius1);
            vertices[i + 1] = D;
            texCoord[i + 1] = new Vector2f(1, i * rstep);
            int index = i * 3 - 6;
            indexes[index] = i - 1;
            indexes[index + 1] = i;
            indexes[index + 2] = i - 2;
            indexes[index + 3] = i - 1;
            indexes[index + 4] = i + 1;
            indexes[index + 5] = i;
        }
        if (close) {
            int index = max * 3 - 3;
            indexes[index] = max;
            indexes[index + 1] = 1;
            indexes[index + 2] = 0;
            indexes[index + 3] = max - 1;
            indexes[index + 4] = max;
            indexes[index + 5] = 0;
        }
        generateNormals(vertNum, normals);
        setBuffers(vertices, texCoord, indexes, normals);
    }

    public RingMesh(float radius, int precision, float repeat) {
        circle(radius, precision, repeat);
    }

    private void circle(float radius, int precision, float repeat) {
        final int vertNum = precision + 1;
        Vector3f[] vertices = new Vector3f[vertNum];
        Vector2f[] texCoord = new Vector2f[vertNum];
        int[] indexes = new int[precision * 3];
        float[] normals = new float[vertNum * 3];
        float factor = 2 * FastMath.PI / precision;
        Vector3f B = new Vector3f(FastMath.cos(factor) * radius, 0, FastMath.sin(factor) * radius);
        Vector3f D = Vector3f.ZERO;
        texCoord[0] = new Vector2f(1, 0);
        texCoord[1] = new Vector2f(0, 0);
        vertices[0] = D;
        vertices[1] = B;
        int max = precision * 1 + 1;
        float rstep = repeat / max;
        for (int i = 2; i < max; i += 1) {
            float bAngle = factor * i;
            B = new Vector3f(FastMath.cos(bAngle) * radius, 0, FastMath.sin(bAngle) * radius);
            vertices[i] = B;
            texCoord[i] = new Vector2f(0, i * rstep);
            int index = i * 3 - 6;
            indexes[index] = 0;
            indexes[index + 1] = i;
            indexes[index + 2] = i - 1;

        }
        int index = max * 3 - 6;
        indexes[index] = 0;
        indexes[index + 1] = 1;
        indexes[index + 2] = max - 1;
        generateNormals(vertNum, normals);
        setBuffers(vertices, texCoord, indexes, normals);
    }

    private void generateNormals(final int vertNum, float[] normals) {
        for (int i = 0; i < vertNum * 3; i += 3) {
            normals[i] = 0;
            normals[i + 1] = 1;
            normals[i + 2] = 0;
        }
    }

    private void setBuffers(Vector3f[] vertices, Vector2f[] texCoord, int[] indexes, float[] normals) {
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        updateBound();
    }
}
