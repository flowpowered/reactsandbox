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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;

import org.lwjgl.BufferUtils;

import org.spout.physics.math.Matrix3x3;
import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;
import org.spout.physics.math.Vector4;

/**
 * Various utility function for the sandbox.
 */
public class SandboxUtil {
	/**
	 * Creates a new rotation 4x4 matrix from the provided quaternion.
	 *
	 * @param q The quaternion
	 * @return The rotation matrix
	 */
	public static Matrix4x4 asRotationMatrix(Quaternion q) {
		final Matrix3x3 m3 = q.getMatrix();
		return new Matrix4x4(
				m3.get(0, 0), m3.get(0, 1), m3.get(0, 2), 0,
				m3.get(1, 0), m3.get(1, 1), m3.get(1, 2), 0,
				m3.get(2, 0), m3.get(2, 1), m3.get(2, 2), 0,
				0, 0, 0, 1);
	}

	/**
	 * Creates a new translation 4x4 matrix from the provided vector3.
	 *
	 * @param v The vector3
	 * @return The translation matrix
	 */
	public static Matrix4x4 asTranslationMatrix(Vector3 v) {
		return new Matrix4x4(
				1, 0, 0, v.getX(),
				0, 1, 0, v.getY(),
				0, 0, 1, v.getZ(),
				0, 0, 0, 1);
	}

	/**
	 * Converts the 4x4 matrix to an array. Can be used for OpenGL.
	 *
	 * @param m The matrix to convert
	 * @return The array for the matrix
	 */
	public static float[] asArray(Matrix4x4 m) {
		final float[] a = new float[16];
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				a[i + j * 4] = m.get(i, j);
			}
		}
		return a;
	}

	/**
	 * Transforms the vector3 with a 4x4 matrix and returns is as a new vector3.
	 *
	 * @param m The transformation matrix
	 * @param v The vector to transform
	 * @return The transformed vector
	 */
	public static Vector3 transform(Matrix4x4 m, Vector3 v) {
		final Vector4 v4 = new Vector4(v.getX(), v.getY(), v.getZ(), 1);
		final Vector4 tv4 = Matrix4x4.multiply(m, v4);
		return new Vector3(tv4.getX(), tv4.getY(), tv4.getZ());
	}

	/**
	 * Creates a new quaternion from the rotation around the axis.
	 *
	 * @param angle The angle of the rotation
	 * @param x The x component of the axis
	 * @param y The y component of the axis
	 * @param z The z component of the axis
	 * @return The quaternion
	 */
	public static Quaternion angleAxisToQuaternion(float angle, float x, float y, float z) {
		final float halfAngle = (float) (Math.toRadians(angle) / 2);
		final float q = (float) (Math.sin(halfAngle) / Math.sqrt(x * x + y * y + z * z));
		return new Quaternion(x * q, y * q, z * q, (float) Math.cos(halfAngle));
	}

	/**
	 * Converts a float list to a float buffer.
	 *
	 * @param floats The float list to convert
	 * @return The float buffer for the list
	 */
	public static FloatBuffer toBuffer(TFloatList floats) {
		final FloatBuffer floatsBuffer = BufferUtils.createFloatBuffer(floats.size());
		floatsBuffer.put(floats.toArray());
		floatsBuffer.flip();
		return floatsBuffer;
	}

	/**
	 * Converts an integer list to an integer buffer.
	 *
	 * @param ints The integer list to convert
	 * @return The integer buffer for the list
	 */
	public static IntBuffer toBuffer(TIntList ints) {
		final IntBuffer intsBuffer = BufferUtils.createIntBuffer(ints.size());
		intsBuffer.put(ints.toArray());
		intsBuffer.flip();
		return intsBuffer;
	}
}
