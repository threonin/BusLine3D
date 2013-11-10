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

    public RingMesh(float radius1, float radius2, int precision, boolean close, float repeat, float height) {
        boolean circle = (radius1 <= 0) && close;
        MeshData meshData;
        if (circle) {
            MeshData meshData1 = circle(radius2, precision, repeat, true, 0);
            MeshData meshData2 = circle(radius2, precision, repeat, false, height);
            meshData = join(meshData1, meshData2, true, true);
        } else {
            MeshData meshData1 = ring(radius1, radius2, precision, close, repeat, true, 0);
            MeshData meshData2 = ring(radius1, radius2, precision, close, repeat, false, height);
            meshData = join(meshData1, meshData2, false, close);
        }
        setBuffers(meshData);
    }

    public RingMesh(float radius1, float radius2, int precision, boolean close, float repeat) {
        boolean circle = (radius1 <= 0) && close;
        MeshData meshData;
        if (circle) {
            meshData = circle(radius2, precision, repeat, false, 0);
        } else {
            meshData = ring(radius1, radius2, precision, close, repeat, false, 0);
        }
        setBuffers(meshData);
    }

    private MeshData ring(float radius1, float radius2, int precision, boolean close, float repeat, boolean reverse, float y) {
        final int vertNum = precision * 2 + (close ? 0 : 2);
        Vector3f[] vertices = new Vector3f[vertNum];
        Vector2f[] texCoord = new Vector2f[vertNum];
        int[] indexes = new int[precision * 6];
        float[] normals = new float[vertNum * 3];
        float factor = 2 * FastMath.PI / precision * (reverse ? -1 : 1);
        Vector3f B = new Vector3f(FastMath.cos(factor * 0.5f) * radius2, y, FastMath.sin(factor * 0.5f) * radius2);
        Vector3f D = new Vector3f(FastMath.cos(factor) * radius1, y, FastMath.sin(factor) * radius1);
        texCoord[0] = new Vector2f(0, 0);
        texCoord[1] = new Vector2f(1, 0);
        vertices[0] = B;
        vertices[1] = D;
        int max = precision * 2 + (close ? -1 : 1);
        float rstep = repeat / max;
        for (int i = 2; i < max; i += 2) {
            float bAngle = factor * ((i >> 1) + 0.5f);
            B = new Vector3f(FastMath.cos(bAngle) * radius2, y, FastMath.sin(bAngle) * radius2);
            vertices[i] = B;
            float tex = reverse ? (repeat - i * rstep) : (i * rstep);
            texCoord[i] = new Vector2f(0, tex);
            float dAngle = factor * ((i >> 1) + 1);
            D = new Vector3f(FastMath.cos(dAngle) * radius1, y, FastMath.sin(dAngle) * radius1);
            vertices[i + 1] = D;
            texCoord[i + 1] = new Vector2f(1, tex);
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
        generateNormals(vertNum, normals, reverse);
        return new MeshData(vertices, texCoord, indexes, normals);
    }

    public RingMesh(float radius, int precision, float repeat) {
        setBuffers(circle(radius, precision, repeat, false, 0));
    }

    private MeshData circle(float radius, int precision, float repeat, boolean reverse, float y) {
        final int vertNum = precision + 1;
        Vector3f[] vertices = new Vector3f[vertNum];
        Vector2f[] texCoord = new Vector2f[vertNum];
        int[] indexes = new int[precision * 3];
        float[] normals = new float[vertNum * 3];
        float factor = 2 * FastMath.PI / precision * (reverse ? -1 : 1);
        Vector3f B = new Vector3f(FastMath.cos(factor) * radius, y, FastMath.sin(factor) * radius);
        Vector3f D = new Vector3f(0, y, 0);
        texCoord[0] = new Vector2f(1, 0);
        texCoord[1] = new Vector2f(0, 0);
        vertices[0] = D;
        vertices[1] = B;
        int max = precision * 1 + 1;
        float rstep = repeat / max;
        for (int i = 2; i < max; i += 1) {
            float bAngle = factor * i;
            B = new Vector3f(FastMath.cos(bAngle) * radius, y, FastMath.sin(bAngle) * radius);
            vertices[i] = B;
            texCoord[i] = new Vector2f(0, reverse ? (repeat - i * rstep) : (i * rstep));
            int index = i * 3 - 6;
            indexes[index] = 0;
            indexes[index + 1] = i;
            indexes[index + 2] = i - 1;
        }
        int index = max * 3 - 6;
        indexes[index] = 0;
        indexes[index + 1] = 1;
        indexes[index + 2] = max - 1;
        generateNormals(vertNum, normals, reverse);
        return new MeshData(vertices, texCoord, indexes, normals);
    }

    private void generateNormals(final int vertNum, float[] normals, boolean reverse) {
        for (int i = 0; i < vertNum * 3; i += 3) {
            normals[i] = 0;
            normals[i + 1] = reverse ? -1 : 1;
            normals[i + 2] = 0;
        }
    }

    private void setBuffers(MeshData meshData) {
        setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(meshData.vertices));
        setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(meshData.texCoord));
        setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(meshData.indexes));
        setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(meshData.normals));
        updateBound();
    }

    private MeshData join(MeshData meshData1, MeshData meshData2, boolean circle, boolean close) {
        int numVert = meshData1.vertices.length;
        Vector3f[] vertices = new Vector3f[numVert * 2];
        System.arraycopy(meshData1.vertices, 0, vertices, 0, numVert);
        System.arraycopy(meshData2.vertices, 0, vertices, numVert, numVert);
        Vector2f[] texCoord = new Vector2f[numVert * 2];
        System.arraycopy(meshData1.texCoord, 0, texCoord, 0, numVert);
        System.arraycopy(meshData2.texCoord, 0, texCoord, numVert, numVert);
        int numIndex = meshData1.indexes.length;
        int[] indexes = new int[numIndex * 2 + numVert * 6 - (circle ? 6 : (close ? 0 : 12))];
        System.arraycopy(meshData1.indexes, 0, indexes, 0, numIndex);
        int i = numIndex;
        for (int index : meshData2.indexes) {
            indexes[i++] = index + numVert;
        }
        if (circle) {
            joinCircles(i, numIndex, indexes, numVert);
        } else {
            joinRings(i, numIndex, indexes, numVert, close);
        }
        float[] normals = new float[numVert * 2];
        System.arraycopy(meshData1.normals, 0, normals, 0, numVert);
        System.arraycopy(meshData2.normals, 0, normals, numVert, numVert);
        return new MeshData(vertices, texCoord, indexes, normals);
    }

    private void joinCircles(int i, int numIndex, int[] indexes, int numVert) {
        int join = 0;
        for (; i <= numIndex * 2 + numVert * 6 - 18; i += 6) {
            indexes[i] = join + 1;
            indexes[i + 1] = numVert * 2 - join - 2;
            indexes[i + 2] = numVert * 2 - join - 1;
            indexes[i + 3] = join + 1;
            indexes[i + 4] = join + 2;
            indexes[i + 5] = numVert * 2 - join - 2;
            join++;
        }
        indexes[i] = join + 1;
        indexes[i + 1] = numVert * 2 - 1;
        indexes[i + 2] = numVert * 2 - join - 1;
        indexes[i + 3] = join + 1;
        indexes[i + 4] = 1;
        indexes[i + 5] = numVert * 2 - 1;
    }

    private void joinRings(int i, int numIndex, int[] indexes, int numVert, boolean close) {
        int join = 0;
        for (; i <= numIndex * 2 + numVert * 3 - 18; i += 6) {
            indexes[i] = join + 1;
            indexes[i + 1] = numVert * 2 - join - 5;
            indexes[i + 2] = join + 3;
            indexes[i + 3] = join + 1;
            indexes[i + 4] = numVert * 2 - join - 3;
            indexes[i + 5] = numVert * 2 - join - 5;
            join += 2;
        }
        if (close) {
            indexes[i++] = join + 1;
            indexes[i++] = numVert * 2 - join - 3;
            indexes[i++] = join + 3;
            indexes[i++] = join + 3;
            indexes[i++] = numVert * 2 - join - 3;
            indexes[i++] = numVert * 2 - 1;
            indexes[i++] = join + 3;
            indexes[i++] = numVert * 2 - 3;
            indexes[i++] = 1;
            indexes[i++] = join + 3;
            indexes[i++] = numVert * 2 - 1;
            indexes[i++] = numVert * 2 - 3;
        } else {
            indexes[i++] = join + 1;
            indexes[i++] = numVert * 2 - 3;
            indexes[i++] = 1;
            indexes[i++] = join + 1;
            indexes[i++] = numVert * 2 - join - 3;
            indexes[i++] = numVert * 2 - 3;
        }
        join = 0;
        for (; i <= numIndex * 2 + numVert * 6 - (close ? 12 : 18); i += 6) {
            indexes[i] = join;
            indexes[i + 1] = join + 2;
            indexes[i + 2] = numVert * 2 - join - 4;
            indexes[i + 3] = join;
            indexes[i + 4] = numVert * 2 - join - 4;
            indexes[i + 5] = numVert * 2 - join - 2;
            join += 2;
        }
        if (close) {
            indexes[i++] = 0;
            indexes[i++] = 2 * numVert - 2;
            indexes[i++] = numVert;
            indexes[i++] = 0;
            indexes[i++] = numVert;
            indexes[i++] = join;
        }
    }

    private class MeshData {

        public Vector3f[] vertices;
        public Vector2f[] texCoord;
        public int[] indexes;
        public float[] normals;

        public MeshData(Vector3f[] vertices, Vector2f[] texCoord, int[] indexes, float[] normals) {
            this.vertices = vertices;
            this.texCoord = texCoord;
            this.indexes = indexes;
            this.normals = normals;
        }
    }
}
