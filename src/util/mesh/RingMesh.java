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
        final int vertNum = precision * (circle ? 1 : 2) + (circle ? 1 : (close ? 0 : 2));
        Vector3f[] vertices = new Vector3f[vertNum];
        Vector2f[] texCoord = new Vector2f[vertNum];
        int[] indexes = new int[precision * (circle ? 3 : 6)];
        float[] normals = new float[vertNum * 3];
        float factor = 2 * FastMath.PI / precision;
        Vector3f B = new Vector3f(FastMath.cos(circle ? factor : (factor * 0.5f)) * radius2, 0, FastMath.sin(circle ? factor : (factor) * 0.5f) * radius2);
        Vector3f D = circle ? Vector3f.ZERO : new Vector3f(FastMath.cos(factor) * radius1, 0, FastMath.sin(factor) * radius1);
        if (circle) {
            texCoord[0] = new Vector2f(1, 0);
            texCoord[1] = new Vector2f(0, 0);
            vertices[0] = D;
            vertices[1] = B;
        } else {
            texCoord[0] = new Vector2f(0, 0);
            texCoord[1] = new Vector2f(1, 0);
            vertices[0] = B;
            vertices[1] = D;
        }
        int max = precision * (circle ? 1 : 2) + (((!circle) && close) ? -1 : 1);
        float rstep = repeat / max;
        for (int i = 2; i < max; i += (circle ? 1 : 2)) {
            float bAngle = factor * (circle ? i : ((i >> 1) + 0.5f));
            B = new Vector3f(FastMath.cos(bAngle) * radius2, 0, FastMath.sin(bAngle) * radius2);
            vertices[i] = B;
            texCoord[i] = new Vector2f(0, i * rstep);
            if (!circle) {
                float dAngle = factor * ((i >> 1) + 1);
                D = new Vector3f(FastMath.cos(dAngle) * radius1, 0, FastMath.sin(dAngle) * radius1);
                vertices[i + 1] = D;
                texCoord[i + 1] = new Vector2f(1, i * rstep);
            }
            int index = i * 3 - 6;
            if (circle) {
                indexes[index] = 0;
                indexes[index + 1] = i;
                indexes[index + 2] = i - 1;
            } else {
                indexes[index] = i - 1;
                indexes[index + 1] = i;
                indexes[index + 2] = i - 2;
                indexes[index + 3] = i - 1;
                indexes[index + 4] = i + 1;
                indexes[index + 5] = i;
            }
        }
        if (circle) {
            int index = max * 3 - 6;
            indexes[index] = 0;
            indexes[index + 1] = 1;
            indexes[index + 2] = max - 1;
        } else if (close) {
            int index = max * 3 - 3;
            indexes[index] = max;
            indexes[index + 1] = 1;
            indexes[index + 2] = 0;
            indexes[index + 3] = max - 1;
            indexes[index + 4] = max;
            indexes[index + 5] = 0;
        }
        for (int i = 0; i < vertNum * 3; i += 3) {
            normals[i] = 0;
            normals[i + 1] = 1;
            normals[i + 2] = 0;
        }
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
        setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        updateBound();
    }
}
