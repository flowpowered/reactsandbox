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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.OpenGLException;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.glu.GLU;

import org.spout.physics.math.Matrix4x4;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Vector3;

public class OpenGL32Renderer {
	// States
	private static boolean created = false;
	// Renderer data
	private static int windowWidth;
	private static int windowHeight;
	private static int vertexShaderID = 0;
	private static int fragmentShaderID = 0;
	private static int programID = 0;
	// OpenGL32Model data
	private static final List<OpenGL32Model> models = new ArrayList<OpenGL32Model>();
	// Shader data
	private static int modelMatrixLocation;
	private static int cameraMatrixLocation;
	private static int projectionMatrixLocation;
	private static int modelColorLocation;
	private static int diffuseIntensityLocation;
	private static int specularIntensityLocation;
	private static int ambientIntensityLocation;
	private static int lightPositionLocation;
	private static int lightAttenuationLocation;
	// Camera
	private static final Matrix4x4 projectionMatrix = Matrix4x4.identity();
	private static final Vector3 cameraPosition = new Vector3(0, 0, 0);
	private static final Quaternion cameraRotation = Quaternion.identity();
	private static final Matrix4x4 cameraRotationMatrix = Matrix4x4.identity();
	private static final Matrix4x4 cameraMatrix = Matrix4x4.identity();
	private static boolean updateCameraMatrix = true;
	// Lighting
	private static final Vector3 lightPosition = new Vector3(0, 0, 0);
	private static float diffuseIntensity = 0.9f;
	private static float specularIntensity = 1;
	private static float ambientIntensity = 0.1f;
	private static Color backgroundColor = new Color(0.2f, 0.2f, 0.2f, 0);
	private static float lightAttenuation = 0.9f;

	private OpenGL32Renderer() {
	}

	/**
	 * Create the render window and basic resources. This excludes the model.
	 *
	 * @param title The title of the render window.
	 * @param windowWidth The width of the render window.
	 * @param windowHeight The height of the render window.
	 * @param fieldOfView The field of view in degrees. 75 is suggested.
	 */
	public static void create(String title, int windowWidth, int windowHeight, float fieldOfView)
			throws LWJGLException {
		if (created) {
			throw new IllegalStateException("Rendered has already been created.");
		}
		createDisplay(title, windowWidth, windowHeight);
		createProjection(fieldOfView);
		createShaders();
		created = true;
	}

	/**
	 * Destroys the render window and the models.
	 */
	public static void destroy() {
		if (!created) {
			throw new IllegalStateException("Rendered has not been created yet.");
		}
		destroyModels();
		destroyShaders();
		destroyDisplay();
		created = false;
	}

	private static void createDisplay(String title, int width, int height) throws LWJGLException {
		windowWidth = width;
		windowHeight = height;
		final PixelFormat pixelFormat = new PixelFormat();
		final ContextAttribs contextAttributes = new ContextAttribs(3, 2).withProfileCore(true);
		Display.setDisplayMode(new DisplayMode(width, height));
		Display.setTitle(title);
		Display.create(pixelFormat, contextAttributes);
		GL11.glViewport(0, 0, width, height);
		GL11.glClearColor(backgroundColor.getRed() / 255f, backgroundColor.getGreen() / 255f,
				backgroundColor.getBlue() / 255f, backgroundColor.getAlpha() / 255f);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL32.GL_DEPTH_CLAMP);
		GL11.glDepthMask(true);
		checkForOpenGLError("createDisplay");
	}

	private static void createProjection(float fieldOfView) {
		final float aspectRatio = windowWidth / windowHeight;
		final float near_plane = 0;
		final float far_plane = 100;
		final float y_scale = (float) (1 / Math.tan(Math.toRadians(fieldOfView / 2)));
		final float x_scale = y_scale / aspectRatio;
		final float frustum_length = far_plane - near_plane;
		projectionMatrix.set(0, 0, x_scale);
		projectionMatrix.set(1, 1, y_scale);
		projectionMatrix.set(2, 2, -(far_plane + near_plane) / frustum_length);
		projectionMatrix.set(3, 2, -1);
		projectionMatrix.set(2, 3, -(2 * near_plane * far_plane) / frustum_length);
	}

	private static void createShaders() {
		vertexShaderID = loadShader("Vertex Shader", OpenGL32Renderer.class.getResourceAsStream("/render.vert"),
				GL20.GL_VERTEX_SHADER);
		fragmentShaderID = loadShader("Fragment Shader", OpenGL32Renderer.class.getResourceAsStream("/render.frag"),
				GL20.GL_FRAGMENT_SHADER);
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID, fragmentShaderID);
		GL20.glLinkProgram(programID);
		modelMatrixLocation = GL20.glGetUniformLocation(programID, "modelMatrix");
		cameraMatrixLocation = GL20.glGetUniformLocation(programID, "cameraMatrix");
		projectionMatrixLocation = GL20.glGetUniformLocation(programID, "projectionMatrix");
		modelColorLocation = GL20.glGetUniformLocation(programID, "modelColor");
		diffuseIntensityLocation = GL20.glGetUniformLocation(programID, "diffuseIntensity");
		specularIntensityLocation = GL20.glGetUniformLocation(programID, "specularIntensity");
		ambientIntensityLocation = GL20.glGetUniformLocation(programID, "ambientIntensity");
		lightPositionLocation = GL20.glGetUniformLocation(programID, "lightPosition");
		lightAttenuationLocation = GL20.glGetUniformLocation(programID, "lightAttenuation");
		GL20.glValidateProgram(programID);
		checkForOpenGLError("createShaders");
	}

	private static void destroyDisplay() {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthMask(false);
		checkForOpenGLError("destroyDisplay");
		Display.destroy();
	}

	private static void destroyShaders() {
		GL20.glUseProgram(0);
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragmentShaderID);
		GL20.glDeleteShader(vertexShaderID);
		GL20.glDeleteShader(fragmentShaderID);
		GL20.glDeleteProgram(programID);
		checkForOpenGLError("destroyShaders");
	}

	private static void destroyModels() {
		for (OpenGL32Model model : models) {
			model.destroy();
		}
		models.clear();
	}

	private static Matrix4x4 cameraMatrix() {
		if (updateCameraMatrix) {
			cameraRotationMatrix.set(MathHelper.asRotationMatrix(cameraRotation));
			final Matrix4x4 cameraPositionMatrix = MathHelper.asTranslationMatrix(cameraPosition);
			cameraMatrix.set(Matrix4x4.multiply(cameraRotationMatrix, cameraPositionMatrix));
			updateCameraMatrix = false;
		}
		return cameraMatrix;
	}

	private static void shaderData() {
		final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
		matrixBuffer.put(MathHelper.asArray(cameraMatrix()));
		matrixBuffer.flip();
		GL20.glUniformMatrix4(cameraMatrixLocation, false, matrixBuffer);
		matrixBuffer.clear();
		matrixBuffer.put(MathHelper.asArray(projectionMatrix));
		matrixBuffer.flip();
		GL20.glUniformMatrix4(projectionMatrixLocation, false, matrixBuffer);
		GL20.glUniform1f(diffuseIntensityLocation, diffuseIntensity);
		GL20.glUniform1f(specularIntensityLocation, specularIntensity);
		GL20.glUniform1f(ambientIntensityLocation, ambientIntensity);
		GL20.glUniform3f(lightPositionLocation, lightPosition.getX(), lightPosition.getY(), lightPosition.getZ());
		GL20.glUniform1f(lightAttenuationLocation, lightAttenuation);
		checkForOpenGLError("preRender");
	}

	private static void shaderData(OpenGL32Model model) {
		final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
		matrixBuffer.put(MathHelper.asArray(model.matrix()));
		matrixBuffer.flip();
		GL20.glUniformMatrix4(modelMatrixLocation, false, matrixBuffer);
		GL20.glUniform4f(modelColorLocation,
				model.color().getRed() / 255f, model.color().getGreen() / 255f,
				model.color().getBlue() / 255f, model.color().getAlpha() / 255f);
		checkForOpenGLError("preRenderModel");
	}

	/**
	 * Displays the models with to the render window.
	 *
	 * @throws IllegalStateException If the display wasn't created first or if no models were added.
	 */
	protected static void render() {
		if (!created) {
			throw new IllegalStateException("Display needs to be created first.");
		}
		if (models.isEmpty()) {
			throw new IllegalStateException("At least one model needs to be created first.");
		}
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL20.glUseProgram(programID);
		shaderData();
		for (OpenGL32Model model : models) {
			if (!model.created()) {
				continue;
			}
			shaderData(model);
			model.render();
		}
		GL20.glUseProgram(0);
		Display.sync(60);
		Display.update();
		checkForOpenGLError("render");
	}

	/**
	 * Returns true if the render display has been created.
	 *
	 * @return True if the display and rendering resources have been creates, false if other wise.
	 */
	public static boolean created() {
		return created;
	}

	/**
	 * Adds a model to the list. If a non-created model is added to the list, it will not be rendered
	 * until it is created.
	 *
	 * @param model The model to add
	 */
	public static void addModel(OpenGL32Model model) {
		if (!models.contains(model)) {
			models.add(model);
		}
	}

	/**
	 * Removes a model from the list
	 *
	 * @param model The model to remove
	 */
	public static void removeModel(OpenGL32Model model) {
		models.remove(model);
	}

	/**
	 * Gets the camera position.
	 *
	 * @return The camera position.
	 */
	public static Vector3 cameraPosition() {
		updateCameraMatrix = true;
		return cameraPosition;
	}

	/**
	 * Sets the camera position.
	 *
	 * @param position The camera position.
	 */
	public static void cameraPosition(Vector3 position) {
		cameraPosition.set(position);
		updateCameraMatrix = true;
	}

	/**
	 * Gets the camera rotation.
	 *
	 * @return The camera rotation.
	 */
	public static Quaternion cameraRotation() {
		updateCameraMatrix = true;
		return cameraRotation;
	}

	/**
	 * Sets the camera rotation.
	 *
	 * @param rotation The camera rotation.
	 */
	public static void cameraRotation(Quaternion rotation) {
		cameraRotation.set(rotation);
		updateCameraMatrix = true;
	}

	/**
	 * Gets the vector representing the right direction for the camera.
	 *
	 * @return The camera's right direction vector.
	 */
	public static Vector3 cameraRight() {
		return toCamera(new Vector3(1, 0, 0));
	}

	/**
	 * Gets the vector representing the up direction for the camera.
	 *
	 * @return The camera's up direction vector.
	 */
	public static Vector3 cameraUp() {
		return toCamera(new Vector3(0, 1, 0));
	}

	/**
	 * Gets the vector representing the forward direction for the camera.
	 *
	 * @return The camera's forward direction vector.
	 */
	public static Vector3 cameraForward() {
		return toCamera(new Vector3(0, 0, 1));
	}

	/**
	 * Gets the background color.
	 *
	 * @return The background color.
	 */
	public static Color backgroundColor() {
		return backgroundColor;
	}

	/**
	 * Sets the background color.
	 *
	 * @param color The background color.
	 */
	public static void backgroundColor(Color color) {
		backgroundColor = color;
	}

	/**
	 * Gets the light position.
	 *
	 * @return The light position.
	 */
	public static Vector3 lightPosition() {
		return lightPosition;
	}

	/**
	 * Sets the light position.
	 *
	 * @param position The light position.
	 */
	public static void lightPosition(Vector3 position) {
		lightPosition.set(position);
	}

	/**
	 * Sets the diffuse intensity.
	 *
	 * @param intensity The diffuse intensity.
	 */
	public static void diffuseIntensity(float intensity) {
		diffuseIntensity = intensity;
	}

	/**
	 * Gets the diffuse intensity.
	 *
	 * @return The diffuse intensity.
	 */
	public static float diffuseIntensity() {
		return diffuseIntensity;
	}

	/**
	 * Sets the specular intensity.
	 *
	 * @param intensity specular The intensity.
	 */
	public static void specularIntensity(float intensity) {
		specularIntensity = intensity;
	}

	/**
	 * Gets specular intensity.
	 *
	 * @return The specular intensity.
	 */
	public static float specularIntensity() {
		return specularIntensity;
	}

	/**
	 * Sets the ambient intensity.
	 *
	 * @param intensity The ambient intensity.
	 */
	public static void ambientIntensity(float intensity) {
		ambientIntensity = intensity;
	}

	/**
	 * Gets the ambient intensity.
	 *
	 * @return The ambient intensity.
	 */
	public static float ambientIntensity() {
		return ambientIntensity;
	}

	/**
	 * Gets the light distance attenuation factor. In other terms, how much distance affects light
	 * intensity. Larger values affect it more. 0.9 is the default value.
	 *
	 * @return The light distance attenuation factor.
	 */
	public static float lightAttenuation() {
		return lightAttenuation;
	}

	/**
	 * Sets the light distance attenuation factor. In other terms, how much distance affects light
	 * intensity. Larger values affect it more. 0.12 is the default value.
	 *
	 * @param attenuation The light distance attenuation factor.
	 */
	public static void lightAttenuation(float attenuation) {
		lightAttenuation = attenuation;
	}

	/**
	 * Checks for an OpenGL exception. If one is found, this method will throw a {@link
	 * org.lwjgl.opengl.OpenGLException} which can be caught and handled.
	 *
	 * @param stage The rendering stage at which this method is being called.
	 */
	protected static void checkForOpenGLError(String stage) {
		final int errorValue = GL11.glGetError();
		if (errorValue != GL11.GL_NO_ERROR) {
			throw new OpenGLException("OPEN GL ERROR: " + stage + ": " + GLU.gluErrorString(errorValue));
		}
	}

	private static int loadShader(String name, InputStream shaderRessource, int type) {
		final StringBuilder shaderSource = new StringBuilder();
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(shaderRessource));
			String line;
			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
			shaderRessource.close();
		} catch (IOException e) {
			System.out.println("IO exception: " + e.getMessage());
		}
		final int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			throw new OpenGLException("OPEN GL ERROR: Could not compile shader \"" + name + "\"\n"
					+ GL20.glGetShaderInfoLog(shaderID, 1000));
		}
		checkForOpenGLError("loadShader");
		return shaderID;
	}

	private static Vector3 toCamera(Vector3 v) {
		final Matrix4x4 inverted = cameraRotationMatrix.getInverse();
		if (inverted != null) {
			return MathHelper.transform(inverted, v);
		}
		return v;
	}
}
