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

import org.spout.physics.math.Vector3;

public class MeshGenerator {
	public static void generateCuboid(OpenGL32Solid destination, Vector3 size) {
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
		4------5
		|\     |\
		| 7------6
		| |    | |
		0-|----1 |
		 \|     \|
		  3------2
		 */
		// corner positions
		final Vector3 p = Vector3.divide(size, 2);
		final Vector3 p6 = new Vector3(p.getX(), p.getY(), p.getZ());
		final Vector3 p0 = Vector3.negate(p6);
		final Vector3 p7 = new Vector3(-p.getX(), p.getY(), p.getZ());
		final Vector3 p1 = Vector3.negate(p7);
		final Vector3 p4 = new Vector3(-p.getX(), p.getY(), -p.getZ());
		final Vector3 p2 = Vector3.negate(p4);
		final Vector3 p5 = new Vector3(p.getX(), p.getY(), -p.getZ());
		final Vector3 p3 = Vector3.negate(p5);
		// face normals
		final Vector3 nx = new Vector3(1, 0, 0);
		final Vector3 ny = new Vector3(0, 1, 0);
		final Vector3 nz = new Vector3(0, 0, 1);
		final Vector3 nxN = new Vector3(-1, 0, 0);
		final Vector3 nyN = new Vector3(0, -1, 0);
		final Vector3 nzN = new Vector3(0, 0, -1);
		// model data buffers
		final TFloatList positions = destination.positions();
		final TFloatList normals = destination.normals();
		final TIntList indices = destination.indices();
		// face x
		addVector(positions, p2);
		addVector(normals, nx);
		addVector(positions, p6);
		addVector(normals, nx);
		addVector(positions, p5);
		addVector(normals, nx);
		addVector(positions, p1);
		addVector(normals, nx);
		addAll(indices, 0, 2, 1, 0, 3, 2);
		// face y
		addVector(positions, p4);
		addVector(normals, ny);
		addVector(positions, p5);
		addVector(normals, ny);
		addVector(positions, p6);
		addVector(normals, ny);
		addVector(positions, p7);
		addVector(normals, ny);
		addAll(indices, 4, 6, 5, 4, 7, 6);
		// face z
		addVector(positions, p3);
		addVector(normals, nz);
		addVector(positions, p7);
		addVector(normals, nz);
		addVector(positions, p6);
		addVector(normals, nz);
		addVector(positions, p2);
		addVector(normals, nz);
		addAll(indices, 8, 10, 9, 8, 11, 10);
		// face -x
		addVector(positions, p0);
		addVector(normals, nxN);
		addVector(positions, p4);
		addVector(normals, nxN);
		addVector(positions, p7);
		addVector(normals, nxN);
		addVector(positions, p3);
		addVector(normals, nxN);
		addAll(indices, 12, 14, 13, 12, 15, 14);
		// face -y
		addVector(positions, p0);
		addVector(normals, nyN);
		addVector(positions, p3);
		addVector(normals, nyN);
		addVector(positions, p2);
		addVector(normals, nyN);
		addVector(positions, p1);
		addVector(normals, nyN);
		addAll(indices, 16, 18, 17, 16, 19, 18);
		// face -z
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

	public static void generateSphere(OpenGL32Solid destination, float radius) {
		// Octahedron positions
		final Vector3 v0 = new Vector3(0.0f, -1.0f, 0.0f);
		final Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
		final Vector3 v2 = new Vector3(0.0f, 0.0f, 1.0f);
		final Vector3 v3 = new Vector3(-1.0f, 0.0f, 0.0f);
		final Vector3 v4 = new Vector3(0.0f, 0.0f, -1.0f);
		final Vector3 v5 = new Vector3(0.0f, 1.0f, 0.0f);
		// Build a list of triangles
		final List<Triangle> triangles = new ArrayList<Triangle>();
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
		final List<Triangle> newTriangles = new ArrayList<Triangle>();
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
			triangle.v0.normalize().multiply(radius);
			triangle.v1.normalize().multiply(radius);
			triangle.v2.normalize().multiply(radius);
		}
		// Model data buffers
		final TFloatList positions = destination.positions();
		final TFloatList normals = destination.normals();
		final TIntList indices = destination.indices();
		// Add the triangle faces to the data buffers
		int index = 0;
		for (Triangle triangle : triangles) {
			addVector(positions, triangle.v0);
			addVector(positions, triangle.v1);
			addVector(positions, triangle.v2);
			final Vector3 normal = triangle.getNormal();
			addVector(normals, normal);
			addVector(normals, normal);
			addVector(normals, normal);
			addAll(indices, index++, index++, index++);
		}
	}

	public static void generateCylinder(OpenGL32Solid destination, float radius, float height) {
		// 0,0,0 will be halfway up the cylinder in the middle
		final float halfHeight = height / 2;
		// Center positions in the top and bottom faces
		final Vector3 top = new Vector3(0, halfHeight, 0);
		final Vector3 bottom = new Vector3(0, -halfHeight, 0);
		// The normals for the triangles of the top and bottom faces
		final Vector3 topNormal = new Vector3(0, 1, 0);
		final Vector3 bottomNormal = new Vector3(0, -1, 0);
		// The positions at the rims of the cylinders
		final List<Vector3> rims = new ArrayList<Vector3>();
		for (int angle = 0; angle < 360; angle += 15) {
			final double angleRads = Math.toRadians(angle);
			rims.add(new Vector3(
					radius * (float) Math.cos(angleRads),
					halfHeight,
					radius * (float) -Math.sin(angleRads)));
		}
		// Model data buffers
		final TFloatList positions = destination.positions();
		final TFloatList normals = destination.normals();
		final TIntList indices = destination.indices();
		// Add all the faces section by section, turning around the y axis
		int index = 0;
		final int rimsSize = rims.size();
		for (int i = 0; i < rimsSize; i++) {
			// Two adjacent rim vertices for the top
			final Vector3 t0 = rims.get(i);
			final Vector3 t1 = rims.get(i == rimsSize - 1 ? 0 : i + 1);
			// Same for the bottom
			final Vector3 b0 = new Vector3(t0.getX(), -t0.getY(), t0.getZ());
			final Vector3 b1 = new Vector3(t1.getX(), -t1.getY(), t1.getZ());
			// Side normal
			final Vector3 n = Vector3.subtract(t1, t0).cross(Vector3.subtract(b0, t0)).normalize();
			// Top triangle
			addVector(positions, t0);
			addVector(normals, topNormal);
			addVector(positions, t1);
			addVector(normals, topNormal);
			addVector(positions, top);
			addVector(normals, topNormal);
			addAll(indices, index++, index++, index++);
			// Bottom triangle
			addVector(positions, b0);
			addVector(normals, bottomNormal);
			addVector(positions, bottom);
			addVector(normals, bottomNormal);
			addVector(positions, b1);
			addVector(normals, bottomNormal);
			addAll(indices, index++, index++, index++);
			// Side triangle 1
			addVector(positions, t1);
			addVector(normals, n);
			addVector(positions, t0);
			addVector(normals, n);
			addVector(positions, b0);
			addVector(normals, n);
			addAll(indices, index++, index++, index++);
			// Side triangle 2
			addVector(positions, t1);
			addVector(normals, n);
			addVector(positions, b0);
			addVector(normals, n);
			addVector(positions, b1);
			addVector(normals, n);
			addAll(indices, index++, index++, index++);
		}
	}

	public static void generateCone(OpenGL32Solid destination, float radius, float height) {
		// 0,0,0 will be halfway up the cone in the middle
		final float halfHeight = height / 2;
		// Apex of the cone
		final Vector3 top = new Vector3(0, halfHeight, 0);
		// Center position in the bottom face
		final Vector3 bottom = new Vector3(0, -halfHeight, 0);
		// The normals for the triangles for the bottom face
		final Vector3 bottomNormal = new Vector3(0, -1, 0);
		// The positions at the bottom rim of the cone
		final List<Vector3> rim = new ArrayList<Vector3>();
		for (int angle = 0; angle < 360; angle += 15) {
			final double angleRads = Math.toRadians(angle);
			rim.add(new Vector3(
					radius * (float) Math.cos(angleRads),
					-halfHeight,
					radius * (float) -Math.sin(angleRads)));
		}
		// Model data buffers
		final TFloatList positions = destination.positions();
		final TFloatList normals = destination.normals();
		final TIntList indices = destination.indices();
		// Add all the faces section by section, turning around the y axis
		int index = 0;
		final int rimSize = rim.size();
		for (int i = 0; i < rimSize; i++) {
			// Two adjacent rim vertices
			final Vector3 b0 = rim.get(i);
			final Vector3 b1 = rim.get(i == rimSize - 1 ? 0 : i + 1);
			// Side normal
			final Vector3 n = Vector3.subtract(top, b0).cross(Vector3.subtract(b1, b0)).normalize();
			// Bottom triangle
			addVector(positions, b0);
			addVector(normals, bottomNormal);
			addVector(positions, bottom);
			addVector(normals, bottomNormal);
			addVector(positions, b1);
			addVector(normals, bottomNormal);
			addAll(indices, index++, index++, index++);
			// Side triangle
			addVector(positions, top);
			addVector(normals, n);
			addVector(positions, b0);
			addVector(normals, n);
			addVector(positions, b1);
			addVector(normals, n);
			addAll(indices, index++, index++, index++);
		}
	}

	private static void addVector(TFloatList to, Vector3 v) {
		to.add(v.getX());
		to.add(v.getY());
		to.add(v.getZ());
	}

	private static void addAll(TIntList to, int... f) {
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
		final Vector3 v0 = new Vector3();
		final Vector3 v1 = new Vector3();
		final Vector3 v2 = new Vector3();

		private Triangle(Vector3 v0, Vector3 v1, Vector3 v2) {
			this.v0.set(v0);
			this.v1.set(v1);
			this.v2.set(v2);
		}

		private Vector3 getNormal() {
			return Vector3.subtract(v1, v0).cross(Vector3.subtract(v2, v0)).normalize();
		}
	}
}
