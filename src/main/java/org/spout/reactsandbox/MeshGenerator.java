/*
 * This file is part of ReactSandbox.
 *
 * Copyright (c) 2013 Spout LLC <http://www.spout.org/>
 * ReactSandbox is licensed under the Spout License Version 1.
 *
 * ReactSandbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * ReactSandbox is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://spout.in/licensev1> for the full license, including
 * the MIT license.
 */
package org.spout.reactsandbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.spout.physics.math.Vector3;
import org.spout.renderer.Model;
import org.spout.renderer.data.VertexAttribute.FloatVertexAttribute;
import org.spout.renderer.data.VertexData;

/**
 * Generates various shape meshes of the desired size and stores them to the models.
 */
public class MeshGenerator {
	/*
	^
	| y
	|
	|     x
	------->
	\
	 \
	  \ z
	   V
	The axis system
	*/
	public static void generateCrosshairs(Model destination, float length) {
		/*
		  \ |
		   \|
		----O-----
		    |\
		    | \
		 */
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		length /= 2;
		// Add the x axis line
		addAll(positions, -length, 0, 0, length, 0, 0);
		addAll(indices, 0, 1);
		// Add the y axis line
		addAll(positions, 0, -length, 0, 0, length, 0);
		addAll(indices, 2, 3);
		// Add the z axis line
		addAll(positions, 0, 0, -length, 0, 0, length);
		addAll(indices, 4, 5);
	}

	/**
	 * Generate a cuboid shaped wireframe (the outline of the cuboid). The center is at the middle of
	 * the cuboid.
	 *
	 * @param destination Where to save the mesh
	 * @param size The size of the cuboid to generate, on x, y and z
	 */
	public static void generateWireCuboid(Model destination, Vector3 size) {
		/*
		4------5
		|\     |\
		| 7------6
		| |    | |
		0-|----1 |
		 \|     \|
		  3------2
		 */
		// Corner positions
		final Vector3 p = Vector3.divide(size, 2);
		final Vector3 p6 = new Vector3(p.getX(), p.getY(), p.getZ());
		final Vector3 p0 = Vector3.negate(p6);
		final Vector3 p7 = new Vector3(-p.getX(), p.getY(), p.getZ());
		final Vector3 p1 = Vector3.negate(p7);
		final Vector3 p4 = new Vector3(-p.getX(), p.getY(), -p.getZ());
		final Vector3 p2 = Vector3.negate(p4);
		final Vector3 p5 = new Vector3(p.getX(), p.getY(), -p.getZ());
		final Vector3 p3 = Vector3.negate(p5);
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		// Add all of the corners
		addVector(positions, p0);
		addVector(positions, p1);
		addVector(positions, p2);
		addVector(positions, p3);
		addVector(positions, p4);
		addVector(positions, p5);
		addVector(positions, p6);
		addVector(positions, p7);
		// Face x
		addAll(indices, 1, 2, 2, 6, 6, 5, 5, 1);
		// Face y
		addAll(indices, 4, 5, 5, 6, 6, 7, 7, 4);
		// Face z
		addAll(indices, 2, 3, 3, 7, 7, 6, 6, 2);
		// Face -x
		addAll(indices, 0, 3, 3, 7, 7, 4, 4, 0);
		// Face -y
		addAll(indices, 0, 1, 1, 2, 2, 3, 3, 0);
		// Face -z
		addAll(indices, 0, 1, 1, 5, 5, 4, 4, 0);
	}

	/**
	 * Generates a solid cuboid mesh. The center is at the middle of the cuboid.
	 *
	 * @param destination Where to save the mesh
	 * @param size The size of the cuboid to generate, on x, y and z
	 */
	public static void generateCuboid(Model destination, Vector3 size) {
		/*
		4------5
		|\     |\
		| 7------6
		| |    | |
		0-|----1 |
		 \|     \|
		  3------2
		 */
		// Corner positions
		final Vector3 p = Vector3.divide(size, 2);
		final Vector3 p6 = new Vector3(p.getX(), p.getY(), p.getZ());
		final Vector3 p0 = Vector3.negate(p6);
		final Vector3 p7 = new Vector3(-p.getX(), p.getY(), p.getZ());
		final Vector3 p1 = Vector3.negate(p7);
		final Vector3 p4 = new Vector3(-p.getX(), p.getY(), -p.getZ());
		final Vector3 p2 = Vector3.negate(p4);
		final Vector3 p5 = new Vector3(p.getX(), p.getY(), -p.getZ());
		final Vector3 p3 = Vector3.negate(p5);
		// Face normals
		final Vector3 nx = new Vector3(1, 0, 0);
		final Vector3 ny = new Vector3(0, 1, 0);
		final Vector3 nz = new Vector3(0, 0, 1);
		final Vector3 nxN = new Vector3(-1, 0, 0);
		final Vector3 nyN = new Vector3(0, -1, 0);
		final Vector3 nzN = new Vector3(0, 0, -1);
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final FloatVertexAttribute normalsAttribute = new FloatVertexAttribute("normals", 3);
		vertices.addAttribute(1, normalsAttribute);
		final TFloatList normals = normalsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		// Face x
		addVector(positions, p2);
		addVector(normals, nx);
		addVector(positions, p6);
		addVector(normals, nx);
		addVector(positions, p5);
		addVector(normals, nx);
		addVector(positions, p1);
		addVector(normals, nx);
		addAll(indices, 0, 2, 1, 0, 3, 2);
		// Face y
		addVector(positions, p4);
		addVector(normals, ny);
		addVector(positions, p5);
		addVector(normals, ny);
		addVector(positions, p6);
		addVector(normals, ny);
		addVector(positions, p7);
		addVector(normals, ny);
		addAll(indices, 4, 6, 5, 4, 7, 6);
		// Face z
		addVector(positions, p3);
		addVector(normals, nz);
		addVector(positions, p7);
		addVector(normals, nz);
		addVector(positions, p6);
		addVector(normals, nz);
		addVector(positions, p2);
		addVector(normals, nz);
		addAll(indices, 8, 10, 9, 8, 11, 10);
		// Face -x
		addVector(positions, p0);
		addVector(normals, nxN);
		addVector(positions, p4);
		addVector(normals, nxN);
		addVector(positions, p7);
		addVector(normals, nxN);
		addVector(positions, p3);
		addVector(normals, nxN);
		addAll(indices, 12, 14, 13, 12, 15, 14);
		// Face -y
		addVector(positions, p0);
		addVector(normals, nyN);
		addVector(positions, p3);
		addVector(normals, nyN);
		addVector(positions, p2);
		addVector(normals, nyN);
		addVector(positions, p1);
		addVector(normals, nyN);
		addAll(indices, 16, 18, 17, 16, 19, 18);
		// Face -z
		addVector(positions, p1);
		addVector(normals, nzN);
		addVector(positions, p5);
		addVector(normals, nzN);
		addVector(positions, p4);
		addVector(normals, nzN);
		addVector(positions, p0);
		addVector(normals, nzN);
		addAll(indices, 20, 22, 21, 20, 23, 22);
	}

	/**
	 * Generates a solid cuboid mesh with texture UV. The center is at the middle of the cuboid.
	 *
	 * @param destination Where to save the mesh
	 * @param size The size of the cuboid to generate, on x, y and z
	 */
	public static void generateTexturedCuboid(Model destination, Vector3 size) {
		generateCuboid(destination, size);
		final FloatVertexAttribute textureAttribute = new FloatVertexAttribute("textureCoords", 2);
		destination.getVertexData().addAttribute(2, textureAttribute);
		final TFloatList texture = textureAttribute.getData();
		final float max = size.get(size.getMaxAxis());
		final float xRatio = size.getX() / max;
		final float yRatio = size.getY() / max;
		final float zRatio = size.getZ() / max;
		// Face x
		addAll(texture, 0, 0, yRatio, 0, 0, zRatio, yRatio, zRatio);
		// Face y
		addAll(texture, 0, 0, xRatio, 0, 0, zRatio, xRatio, zRatio);
		// Face z
		addAll(texture, 0, 0, yRatio, 0, 0, xRatio, yRatio, xRatio);
		// Face -x
		addAll(texture, 0, 0, yRatio, 0, 0, zRatio, yRatio, zRatio);
		// Face -y
		addAll(texture, 0, 0, zRatio, 0, 0, xRatio, zRatio, xRatio);
		// Face -z
		addAll(texture, 0, 0, yRatio, 0, 0, xRatio, yRatio, xRatio);
	}

	/**
	 * Generates a solid spherical mesh. The center is at the middle of the sphere.
	 *
	 * @param destination Where to save the mesh
	 * @param radius The radius of the sphere
	 */
	public static void generateSphere(Model destination, float radius) {
		// Octahedron positions
		final Vector3 v0 = new Vector3(0.0f, -1.0f, 0.0f);
		final Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
		final Vector3 v2 = new Vector3(0.0f, 0.0f, 1.0f);
		final Vector3 v3 = new Vector3(-1.0f, 0.0f, 0.0f);
		final Vector3 v4 = new Vector3(0.0f, 0.0f, -1.0f);
		final Vector3 v5 = new Vector3(0.0f, 1.0f, 0.0f);
		// Build a list of triangles
		final List<Triangle> triangles = new ArrayList<>();
		triangles.addAll(Arrays.asList(
				new Triangle(v0, v1, v2),
				new Triangle(v0, v2, v3),
				new Triangle(v0, v3, v4),
				new Triangle(v0, v4, v1),
				new Triangle(v1, v5, v2),
				new Triangle(v2, v5, v3),
				new Triangle(v3, v5, v4),
				new Triangle(v4, v5, v1)));
		// List to store the subdivided triangles
		final List<Triangle> newTriangles = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			// Subdivide all of the triangles by splitting the edges
			for (Triangle triangle : triangles) {
				newTriangles.addAll(Arrays.asList(subdivide(triangle)));
			}
			// Store the new triangles in the main list
			triangles.clear();
			triangles.addAll(newTriangles);
			// Clear the new triangles for the next run
			newTriangles.clear();
		}
		// Normalize the positions so they are all the same distance from the center
		// then scale them to the appropriate radius
		for (Triangle triangle : triangles) {
			triangle.getV0().normalize().multiply(radius);
			triangle.getV1().normalize().multiply(radius);
			triangle.getV2().normalize().multiply(radius);
		}
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final FloatVertexAttribute normalsAttribute = new FloatVertexAttribute("normals", 3);
		vertices.addAttribute(1, normalsAttribute);
		final TFloatList normals = normalsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		// Add the triangle faces to the data buffers
		int index = 0;
		// Keep track of already added vertices, so we can reuse them for a smaller mesh
		final TObjectIntMap<Vector3> addedVertices = new TObjectIntHashMap<>();
		for (Triangle triangle : triangles) {
			final Vector3 vt0 = triangle.getV0();
			final Vector3 vt1 = triangle.getV1();
			final Vector3 vt2 = triangle.getV2();
			if (addedVertices.containsKey(vt0)) {
				addAll(indices, addedVertices.get(vt0));
			} else {
				addVector(positions, vt0);
				addVector(normals, vt0.getUnit());
				addedVertices.put(vt0, index);
				addAll(indices, index++);
			}
			if (addedVertices.containsKey(vt1)) {
				addAll(indices, addedVertices.get(vt1));
			} else {
				addVector(positions, vt1);
				addVector(normals, vt1.getUnit());
				addedVertices.put(vt1, index);
				addAll(indices, index++);
			}
			if (addedVertices.containsKey(vt2)) {
				addAll(indices, addedVertices.get(vt2));
			} else {
				addVector(positions, vt2);
				addVector(normals, vt2.getUnit());
				addedVertices.put(vt2, index);
				addAll(indices, index++);
			}
		}
	}

	/**
	 * Generates a cylindrical solid mesh. The center is at middle of the of the cylinder.
	 *
	 * @param destination Where to save the mesh
	 * @param radius The radius of the base and top
	 * @param height The height (distance from the base to the top)
	 */
	public static void generateCylinder(Model destination, float radius, float height) {
		// 0,0,0 will be halfway up the cylinder in the middle
		final float halfHeight = height / 2;
		// The positions at the rims of the cylinders
		final List<Vector3> rims = new ArrayList<>();
		for (int angle = 0; angle < 360; angle += 15) {
			final double angleRads = Math.toRadians(angle);
			rims.add(new Vector3(
					radius * (float) Math.cos(angleRads),
					halfHeight,
					radius * (float) -Math.sin(angleRads)));
		}
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final FloatVertexAttribute normalsAttribute = new FloatVertexAttribute("normals", 3);
		vertices.addAttribute(1, normalsAttribute);
		final TFloatList normals = normalsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		// The normals for the triangles of the top and bottom faces
		final Vector3 topNormal = new Vector3(0, 1, 0);
		final Vector3 bottomNormal = new Vector3(0, -1, 0);
		// Add the top and bottom face center vertices
		addVector(positions, new Vector3(0, halfHeight, 0));// 0
		addVector(normals, topNormal);
		addVector(positions, new Vector3(0, -halfHeight, 0));// 1
		addVector(normals, bottomNormal);
		// Add all the faces section by section, turning around the y axis
		final int rimsSize = rims.size();
		for (int i = 0; i < rimsSize; i++) {
			// Get the top and bottom vertex positions and the side normal
			final Vector3 t = rims.get(i);
			final Vector3 b = new Vector3(t.getX(), -t.getY(), t.getZ());
			final Vector3 n = new Vector3(t.getX(), 0, t.getZ()).normalize();
			// Top face vertex
			addVector(positions, t);// index
			addVector(normals, topNormal);
			// Bottom face vertex
			addVector(positions, b);// index + 1
			addVector(normals, bottomNormal);
			// Side top vertex
			addVector(positions, t);// index + 2
			addVector(normals, n);
			// Side bottom vertex
			addVector(positions, b);// index + 3
			addVector(normals, n);
			// Get the current index for our vertices
			final int currentIndex = i * 4 + 2;
			// Get the index for the next iteration, wrapping around at the end
			final int nextIndex = (i == rimsSize - 1 ? 0 : i + 1) * 4 + 2;
			// Add the 4 triangles (1 top, 1 bottom, 2 for the side)
			addAll(indices, 0, currentIndex, nextIndex);
			addAll(indices, 1, nextIndex + 1, currentIndex + 1);
			addAll(indices, currentIndex + 2, currentIndex + 3, nextIndex + 2);
			addAll(indices, currentIndex + 3, nextIndex + 3, nextIndex + 2);
		}
	}

	/**
	 * Generates a conical solid mesh. The center is at the middle of the cone.
	 *
	 * @param destination Where to save the mesh
	 * @param radius The radius of the base
	 * @param height The height (distance from the base to the apex)
	 */
	public static void generateCone(Model destination, float radius, float height) {
		// 0,0,0 will be halfway up the cone in the middle
		final float halfHeight = height / 2;
		// The positions at the bottom rim of the cone
		final List<Vector3> rim = new ArrayList<>();
		for (int angle = 0; angle < 360; angle += 15) {
			final double angleRads = Math.toRadians(angle);
			rim.add(new Vector3(
					radius * (float) Math.cos(angleRads),
					-halfHeight,
					radius * (float) -Math.sin(angleRads)));
		}
		// Model data buffers
		final VertexData vertices = destination.getVertexData();
		final FloatVertexAttribute positionsAttribute = new FloatVertexAttribute("positions", 3);
		vertices.addAttribute(0, positionsAttribute);
		final TFloatList positions = positionsAttribute.getData();
		final FloatVertexAttribute normalsAttribute = new FloatVertexAttribute("normals", 3);
		vertices.addAttribute(1, normalsAttribute);
		final TFloatList normals = normalsAttribute.getData();
		final TIntList indices = vertices.getIndices();
		// Apex of the cone
		final Vector3 top = new Vector3(0, halfHeight, 0);
		// The normal for the triangle of the bottom face
		final Vector3 bottomNormal = new Vector3(0, -1, 0);
		// Add the bottom face center vertex
		addVector(positions, new Vector3(0, -halfHeight, 0));// 0
		addVector(normals, bottomNormal);
		// Add all the faces section by section, turning around the y axis
		final int rimSize = rim.size();
		for (int i = 0; i < rimSize; i++) {
			// Get the bottom vertex position and the side normal
			final Vector3 b = rim.get(i);
			final Vector3 bn = new Vector3(b.getX(), 0, b.getZ()).normalize();
			// Average the current and next normal to get the top normal
			final int nextI = i == rimSize - 1 ? 0 : i + 1;
			final Vector3 nextB = rim.get(nextI);
			final Vector3 tn = mean(bn, new Vector3(nextB.getX(), 0, nextB.getZ()).normalize());
			// Top side vertex
			addVector(positions, top);// index
			addVector(normals, tn);
			// Bottom side vertex
			addVector(positions, b);// index + 1
			addVector(normals, bn);
			// Bottom face vertex
			addVector(positions, b);// index + 2
			addVector(normals, bottomNormal);
			// Get the current index for our vertices
			final int currentIndex = i * 3 + 1;
			// Get the index for the next iteration, wrapping around at the end
			final int nextIndex = nextI * 3 + 1;
			// Add the 2 triangles (1 side, 1 bottom)
			addAll(indices, currentIndex, currentIndex + 1, nextIndex + 1);
			addAll(indices, currentIndex + 2, 0, nextIndex + 2);
		}
	}

	private static Vector3 mean(Vector3 v0, Vector3 v1) {
		return new Vector3(
				(v0.getX() + v1.getX()) / 2,
				(v0.getY() + v1.getY()) / 2,
				(v0.getZ() + v1.getZ()) / 2);
	}

	private static void addVector(TFloatList to, Vector3 v) {
		to.add(v.getX());
		to.add(v.getY());
		to.add(v.getZ());
	}

	private static void addAll(TIntList to, int... f) {
		to.add(f);
	}

	private static void addAll(TFloatList to, float... f) {
		to.add(f);
	}

	private static Triangle[] subdivide(Triangle triangle) {
		final Vector3 e0 = Vector3.subtract(triangle.v1, triangle.v0).divide(2);
		final Vector3 va = Vector3.add(triangle.v0, e0);
		final Vector3 e1 = Vector3.subtract(triangle.v2, triangle.v1).divide(2);
		final Vector3 vb = Vector3.add(triangle.v1, e1);
		final Vector3 e2 = Vector3.subtract(triangle.v0, triangle.v2).divide(2);
		final Vector3 vc = Vector3.add(triangle.v2, e2);
		return new Triangle[]{
				new Triangle(triangle.v0, va, vc),
				new Triangle(va, triangle.v1, vb),
				new Triangle(vc, vb, triangle.v2),
				new Triangle(va, vb, vc)
		};
	}

	private static class Triangle {
		private final Vector3 v0 = new Vector3();
		private final Vector3 v1 = new Vector3();
		private final Vector3 v2 = new Vector3();

		private Triangle(Vector3 v0, Vector3 v1, Vector3 v2) {
			this.v0.set(v0);
			this.v1.set(v1);
			this.v2.set(v2);
		}

		private Vector3 getV0() {
			return v0;
		}

		private Vector3 getV1() {
			return v1;
		}

		private Vector3 getV2() {
			return v2;
		}
	}
}
