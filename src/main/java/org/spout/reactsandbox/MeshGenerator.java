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

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;

import org.spout.physics.math.Vector3;

public class MeshGenerator {
	public static void generateCuboidMesh(OpenGL32Model dest, Vector3 size) {
		/* ^
		 * | y
		 * |
		 * |     x
		 * ------->
		 * \
		 *  \
		 *   \ z
		 *    V
		 * 4------5
		 * |\     |\
		 * | 7------6
		 * | |    | |
		 * 0-|----1 |
		 *  \|     \|
		 *   3------2
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
		final TFloatList positions = dest.positions();
		final TFloatList normals = dest.normals();
		final TIntList indices = dest.indices();
		// face x
		addVector(positions, p2);
		addVector(normals, nx);
		addVector(positions, p6);
		addVector(normals, nx);
		addVector(positions, p5);
		addVector(normals, nx);
		addVector(positions, p1);
		addVector(normals, nx);
		addAll(indices, 0, 1, 2, 0, 2, 3);
		// face y
		addVector(positions, p4);
		addVector(normals, ny);
		addVector(positions, p5);
		addVector(normals, ny);
		addVector(positions, p6);
		addVector(normals, ny);
		addVector(positions, p7);
		addVector(normals, ny);
		addAll(indices, 4, 5, 6, 4, 6, 7);
		// face z
		addVector(positions, p3);
		addVector(normals, nz);
		addVector(positions, p7);
		addVector(normals, nz);
		addVector(positions, p6);
		addVector(normals, nz);
		addVector(positions, p2);
		addVector(normals, nz);
		addAll(indices, 8, 9, 10, 8, 10, 11);
		// face -x
		addVector(positions, p0);
		addVector(normals, nxN);
		addVector(positions, p4);
		addVector(normals, nxN);
		addVector(positions, p7);
		addVector(normals, nxN);
		addVector(positions, p3);
		addVector(normals, nxN);
		addAll(indices, 12, 13, 14, 12, 14, 15);
		// face -y
		addVector(positions, p0);
		addVector(normals, nyN);
		addVector(positions, p3);
		addVector(normals, nyN);
		addVector(positions, p2);
		addVector(normals, nyN);
		addVector(positions, p1);
		addVector(normals, nyN);
		addAll(indices, 16, 17, 18, 16, 18, 19);
		// face -z
		addVector(positions, p1);
		addVector(normals, nzN);
		addVector(positions, p5);
		addVector(normals, nzN);
		addVector(positions, p4);
		addVector(normals, nzN);
		addVector(positions, p0);
		addVector(normals, nzN);
		addAll(indices, 20, 21, 22, 20, 22, 23);
	}

	private static void addVector(TFloatList to, Vector3 v) {
		to.add(v.getX());
		to.add(v.getY());
		to.add(v.getZ());
	}

	private static void addAll(TIntList to, int... f) {
		to.add(f);
	}
}
