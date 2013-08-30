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

import java.util.Random;

import org.spout.math.vector.Vector2;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.IntUniform;
import org.spout.renderer.data.Uniform.Vector2ArrayUniform;
import org.spout.renderer.data.UniformHolder;

public class ShadowMappingEffect {
	private final int kernelSize;
	private final Vector2[] kernel;
	private final float bias;
	private final float radius;

	public ShadowMappingEffect(int kernelSize, float bias, float radius) {
		this.kernelSize = kernelSize;
		this.kernel = new Vector2[kernelSize];
		this.bias = bias;
		this.radius = radius;
		// Generate the kernel
		final Random random = new Random();
		for (int i = 0; i < kernelSize; i++) {
			// Create a set of random unit vectors
			kernel[i] = new Vector2(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1).normalize();
		}
	}

	public void dispose() {
	}

	public void addUniforms(UniformHolder destination) {
		destination.add(new IntUniform("kernelSize", kernelSize));
		destination.add(new Vector2ArrayUniform("kernel", kernel));
		destination.add(new FloatUniform("bias", bias));
		destination.add(new FloatUniform("radius", radius));
	}
}
