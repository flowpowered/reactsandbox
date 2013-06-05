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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;

public class OpenGL32Model {
	// Vertex info
	private static final byte POSITION_COMPONENT_COUNT = 3;
	private static final byte NORMAL_COMPONENT_COUNT = 3;
	// State
	private boolean created = false;
	// Vertex data
	private final TFloatList positions = new TFloatArrayList();
	private final TFloatList normals = new TFloatArrayList();
	private final TIntList indices = new TIntArrayList();
	private int renderingIndicesCount;
	// OpenGL pointers
	private int vertexArrayID = 0;
	private int positionsBufferID = 0;
	private int normalsBufferID = 0;
	private int vertexIndexBufferID = 0;
	// Properties
	private final Vector3 position = new Vector3(0, 0, 0);
	private final Quaternion rotation = Quaternion.identity();
	private final Matrix4x4 matrix = Matrix4x4.identity();
	private boolean updateMatrix = true;
	private Color modelColor = new Color(1, 0.1f, 0.1f, 1);

	/**
	 * Creates the model from it's mesh. It can now be rendered.
	 *
	 * @throws IllegalStateException If the display wasn't created first. If the model has already been
	 * created.
	 */
	public void create() {
		if (!OpenGL32Renderer.created()) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		if (created) {
			throw new IllegalStateException("OpenGL32Model has already been created.");
		}
		vertexIndexBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		renderingIndicesCount = indices.size();
		positionsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positionsBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		normalsBufferID = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normalsBuffer(), GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		vertexArrayID = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vertexArrayID);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, positionsBufferID);
		GL20.glVertexAttribPointer(0, POSITION_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normalsBufferID);
		GL20.glVertexAttribPointer(1, NORMAL_COMPONENT_COUNT, GL11.GL_FLOAT, false, 0, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		created = true;
		OpenGL32Renderer.checkForOpenGLError("createModel");
	}

	/**
	 * Destroys the model's resources. It can no longer be rendered.
	 */
	public void destroy() {
		if (!created) {
			return;
		}
		deleteMesh();
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(positionsBufferID);
		GL15.glDeleteBuffers(normalsBufferID);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL15.glDeleteBuffers(vertexIndexBufferID);
		GL30.glBindVertexArray(0);
		GL30.glDeleteVertexArrays(vertexArrayID);
		renderingIndicesCount = 0;
		created = false;
		OpenGL32Renderer.checkForOpenGLError("destroyModel");
	}

	/**
	 * Delete all the model mesh generated so far.
	 */
	public void deleteMesh() {
		positions.clear();
		normals.clear();
		indices.clear();
	}

	/**
	 * Returns the model's matrix; updating it if necessary.
	 *
	 * @return The model matrix.
	 */
	protected Matrix4x4 matrix() {
		if (updateMatrix) {
			final Matrix4x4 rotationMatrix = MathHelper.asRotationMatrix(rotation);
			final Matrix4x4 positionMatrix = MathHelper.asTranslationMatrix(position);
			matrix.set(Matrix4x4.multiply(rotationMatrix, positionMatrix));
			updateMatrix = false;
		}
		return matrix;
	}

	/**
	 * Displays the current model with the proper rotation and position to the render window.
	 */
	protected void render() {
		GL30.glBindVertexArray(vertexArrayID);
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vertexIndexBufferID);
		GL11.glDrawElements(GL11.GL_TRIANGLES, renderingIndicesCount, GL11.GL_UNSIGNED_INT, 0);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		OpenGL32Renderer.checkForOpenGLError("renderModel");
	}

	/**
	 * Returns the list of indices used by OpenGL to draw to pick the order of vertices to draw the
	 * object. Use it to add mesh data.
	 *
	 * @return The indices list.
	 */
	public TIntList indices() {
		return indices;
	}

	/**
	 * Returns the list of three component positions (x, y, z) for rendering. Use it to add mesh data.
	 *
	 * @return The position list.
	 */
	public TFloatList positions() {
		return positions;
	}

	/**
	 * Returns the list of three component normals (x, y, z) for lighting. Use it to add mesh data.
	 *
	 * @return The normal list.
	 */
	public TFloatList normals() {
		return normals;
	}

	/**
	 * Returns true if the display was created and is ready for rendering, false if otherwise.
	 *
	 * @return True if the model can be rendered, false if not.
	 */
	public boolean created() {
		return created;
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

	private FloatBuffer positionsBuffer() {
		final FloatBuffer positionsBuffer = BufferUtils.createFloatBuffer(positions.size());
		positionsBuffer.put(positions.toArray());
		positionsBuffer.flip();
		return positionsBuffer;
	}

	private FloatBuffer normalsBuffer() {
		final FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(normals.size());
		verticesBuffer.put(normals.toArray());
		verticesBuffer.flip();
		return verticesBuffer;
	}

	private IntBuffer indicesBuffer() {
		final IntBuffer indicesBuffer = BufferUtils.createIntBuffer(indices.size());
		indicesBuffer.put(indices.toArray());
		indicesBuffer.flip();
		return indicesBuffer;
	}
}
