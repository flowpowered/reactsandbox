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

import java.awt.Color;

import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;

public abstract class OpenGL32Model {
	// State
	protected boolean created = false;
	// Properties
	protected final Vector3 position = new Vector3(0, 0, 0);
	protected final Quaternion rotation = Quaternion.identity();
	protected final Matrix4x4 matrix = Matrix4x4.identity();
	protected boolean updateMatrix = true;
	protected Color modelColor = new Color(0.8f, 0.1f, 0.1f, 1);

	public abstract void create();

	public abstract void destroy();

	protected abstract void render();

	/**
	 * Returns true if the display was created and is ready for rendering, false if otherwise.
	 *
	 * @return True if the model can be rendered, false if not.
	 */
	public boolean isCreated() {
		return created;
	}

	/**
	 * Returns the model's matrix; updating it if necessary.
	 *
	 * @return The model matrix.
	 */
	protected Matrix4x4 matrix() {
		if (updateMatrix) {
			final Matrix4x4 rotationMatrix = SandboxUtil.asRotationMatrix(rotation);
			final Matrix4x4 positionMatrix = SandboxUtil.asTranslationMatrix(position);
			matrix.set(Matrix4x4.multiply(rotationMatrix, positionMatrix));
			updateMatrix = false;
		}
		return matrix;
	}

	/**
	 * Gets the model color.
	 *
	 * @return The model color.
	 */
	public Color color() {
		return modelColor;
	}

	/**
	 * Sets the model color.
	 *
	 * @param color The model color.
	 */
	public void color(Color color) {
		modelColor = color;
	}

	/**
	 * Gets the model position.
	 *
	 * @return The model position.
	 */
	public Vector3 position() {
		updateMatrix = true;
		return position;
	}

	/**
	 * Sets the model position.
	 *
	 * @param position The model position.
	 */
	public void position(Vector3 position) {
		this.position.set(position);
		updateMatrix = true;
	}

	/**
	 * Gets the model rotation.
	 *
	 * @return The model rotation.
	 */
	public Quaternion rotation() {
		updateMatrix = true;
		return rotation;
	}

	/**
	 * Sets the model rotation.
	 *
	 * @param rotation The model rotation.
	 */
	public void rotation(Quaternion rotation) {
		this.rotation.set(rotation);
		updateMatrix = true;
	}
}
