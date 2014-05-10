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

import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;

/**
 * Various utility function for the sandbox.
 */
public class SandboxUtil {
    /**
     * Converts from Math to React Vector3.
     *
     * @param v The React Vector3
     * @return The equivalent Math Vector3
     */
    public static com.flowpowered.math.vector.Vector3f toMathVector3(Vector3 v) {
        return new com.flowpowered.math.vector.Vector3f(v.getX(), v.getY(), v.getZ());
    }

    /**
     * Converts from React to Math Vector3.
     *
     * @param v The Math Vector3
     * @return The equivalent React Vector3
     */
    public static Vector3 toReactVector3(com.flowpowered.math.vector.Vector3f v) {
        return new Vector3(v.getX(), v.getY(), v.getZ());
    }

    /**
     * Converts from React to Math Quaternion.
     *
     * @param q The React Quaternion
     * @return The equivalent Math Quaternion
     */
    public static com.flowpowered.math.imaginary.Quaternionf toMathQuaternion(Quaternion q) {
        return new com.flowpowered.math.imaginary.Quaternionf(q.getX(), q.getY(), q.getZ(), q.getW());
    }

    /**
     * Converts from Math to React Quaternion
     *
     * @param q The Math Quaternion
     * @return The equivalent React Quaternion
     */
    public static Quaternion toReactQuaternion(com.flowpowered.math.imaginary.Quaternionf q) {
        return new Quaternion(q.getX(), q.getY(), q.getZ(), q.getW());
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
}
