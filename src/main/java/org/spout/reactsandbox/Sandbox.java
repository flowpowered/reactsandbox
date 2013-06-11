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
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.yaml.snakeyaml.Yaml;

import org.spout.physics.body.CollisionBody;
import org.spout.physics.body.RigidBody;
import org.spout.physics.collision.shape.AABB;
import org.spout.physics.collision.shape.BoxShape;
import org.spout.physics.collision.shape.CollisionShape;
import org.spout.physics.collision.shape.ConeShape;
import org.spout.physics.collision.shape.CylinderShape;
import org.spout.physics.collision.shape.SphereShape;
import org.spout.physics.engine.DynamicsWorld;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Transform;
import org.spout.physics.math.Vector3;

/**
 * The main class of the ReactSandbox.
 */
public class Sandbox {
	// Constants
	private static final String WINDOW_TITLE = "React Sandbox";
	private static final float TIMESTEP = 1f / 60;
	private static final int TIMESTEP_MILLISEC = Math.round(TIMESTEP * 1000);
	// Settings
	private static float mouseSensitivity = 0.08f;
	private static float cameraSpeed = 0.2f;
	private static int windowWidth = 1200;
	private static int windowHeight = 800;
	private static float fieldOfView = 75;
	private static Color defaultAABBColor;
	private static Color defaultShapeColor;
	// Physics objects
	private static DynamicsWorld world;
	private static final Vector3 gravity = new Vector3(0, -9.81f, 0);
	private static final Map<RigidBody, OpenGL32Solid> shapes = new HashMap<RigidBody, OpenGL32Solid>();
	private static final Map<RigidBody, OpenGL32Wireframe> aabbs = new HashMap<RigidBody, OpenGL32Wireframe>();
	// Input
	private static boolean mouseGrabbed = true;
	private static float cameraPitch = 0;
	private static float cameraYaw = 0;

	/**
	 * Entry point for the application.
	 *
	 * @param args Unused
	 */
	public static void main(String[] args) {
		try {
			deploy();
			loadConfiguration();
			System.out.println("Starting up");
			OpenGL32Renderer.create(WINDOW_TITLE, windowWidth, windowHeight, fieldOfView);
			world = new DynamicsWorld(gravity, TIMESTEP);
			addBody(new BoxShape(new Vector3(1, 1, 1)), 1, new Vector3(0, 6, 0), SandboxUtil.angleAxisToQuaternion(45, 1, 1, 1));
			addBody(new ConeShape(1, 2), 1, new Vector3(0, 9, 0), SandboxUtil.angleAxisToQuaternion(89, -1, -1, -1));
			addBody(new CylinderShape(1, 2), 1, new Vector3(0, 12, 0), SandboxUtil.angleAxisToQuaternion(-15, 1, -1, 1));
			addBody(new SphereShape(1), 1, new Vector3(0, 15, 0), SandboxUtil.angleAxisToQuaternion(32, -1, -1, 1));
			addBody(new BoxShape(new Vector3(50, 1, 50)), 100, new Vector3(0, 0, 0), Quaternion.identity()).setMotionEnabled(false);
			Mouse.setGrabbed(true);
			world.start();
			OpenGL32Renderer.cameraPosition().setAllValues(0, 5, 10);
			while (!Display.isCloseRequested()) {
				final long start = System.nanoTime();
				processInput();
				world.update();
				updateBodies();
				final CollisionBody targeted = world.findClosestIntersectingBody(
						OpenGL32Renderer.cameraPosition(),
						OpenGL32Renderer.cameraForward());
				if (targeted instanceof RigidBody) {
					aabbs.get(targeted).color(Color.BLUE);
				}
				OpenGL32Renderer.render();
				final long delta = Math.round((System.nanoTime() - start) / 1000000d);
				Thread.sleep(Math.max(TIMESTEP_MILLISEC - delta, 0));
			}
			System.out.println("Shutting down");
			world.stop();
			Mouse.setGrabbed(false);
			OpenGL32Renderer.destroy();
		} catch (Exception ex) {
			ex.printStackTrace();
			final String name = ex.getClass().getSimpleName();
			final String message = ex.getMessage();
			Sys.alert("Error: " + name, message == null || message.trim().equals("") ? name : message);
			System.exit(-1);
		}
	}

	private static RigidBody addBody(CollisionShape shape, float mass, Vector3 position, Quaternion orientation) {
		RigidBody body = world.createRigidBody(new Transform(position, orientation), mass, shape);
		body.setMotionEnabled(true);
		body.setRestitution(0.5f);
		final Transform bodyTransform = body.getTransform();
		final Vector3 bodyPosition = bodyTransform.getPosition();
		final Quaternion bodyOrientation = bodyTransform.getOrientation();
		final OpenGL32Wireframe aabbModel = new OpenGL32Wireframe();
		MeshGenerator.generateCuboid(aabbModel, new Vector3(1, 1, 1));
		final AABB aabb = body.getAABB();
		aabbModel.scale(Vector3.subtract(aabb.getMax(), aabb.getMin()));
		final OpenGL32Solid shapeModel = new OpenGL32Solid();
		switch (shape.getType()) {
			case BOX:
				final BoxShape box = (BoxShape) shape;
				MeshGenerator.generateCuboid(shapeModel, Vector3.multiply(box.getExtent(), 2));
				break;
			case CONE:
				final ConeShape cone = (ConeShape) shape;
				MeshGenerator.generateCone(shapeModel, cone.getRadius(), cone.getHeight());
				break;
			case CYLINDER:
				final CylinderShape cylinder = (CylinderShape) shape;
				MeshGenerator.generateCylinder(shapeModel, cylinder.getRadius(), cylinder.getHeight());
				break;
			case SPHERE:
				final SphereShape sphere = (SphereShape) shape;
				MeshGenerator.generateSphere(shapeModel, sphere.getRadius());
		}
		aabbModel.position(bodyPosition);
		aabbModel.color(defaultAABBColor);
		aabbModel.create();
		OpenGL32Renderer.addModel(aabbModel);
		aabbs.put(body, aabbModel);
		shapeModel.position(bodyPosition);
		shapeModel.rotation(bodyOrientation);
		shapeModel.color(defaultShapeColor);
		shapeModel.create();
		OpenGL32Renderer.addModel(shapeModel);
		shapes.put(body, shapeModel);
		return body;
	}

	private static void updateBodies() {
		for (Entry<RigidBody, OpenGL32Solid> entry : shapes.entrySet()) {
			final RigidBody body = entry.getKey();
			final OpenGL32Solid shape = entry.getValue();
			final OpenGL32Wireframe aabbModel = aabbs.get(body);
			final AABB aabb = body.getAABB();
			final Transform transform = body.getTransform();
			final Vector3 position = transform.getPosition();
			aabbModel.position(position);
			shape.position(position);
			aabbModel.scale(Vector3.subtract(aabb.getMax(), aabb.getMin()));
			shape.rotation(transform.getOrientation());
			aabbModel.color(defaultAABBColor);
		}
	}

	private static void processInput() {
		final boolean mouseGrabbedBefore = mouseGrabbed;
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				if (Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
					mouseGrabbed ^= true;
				}
			}
		}
		if (Display.isActive()) {
			if (mouseGrabbed != mouseGrabbedBefore) {
				Mouse.setGrabbed(mouseGrabbed);
			}
			if (mouseGrabbed) {
				cameraYaw -= Mouse.getDY() * mouseSensitivity;
				cameraYaw %= 360;
				cameraPitch += Mouse.getDX() * mouseSensitivity;
				cameraPitch %= 360;
				final Quaternion yaw = SandboxUtil.angleAxisToQuaternion(cameraYaw, 1, 0, 0);
				final Quaternion pitch = SandboxUtil.angleAxisToQuaternion(cameraPitch, 0, 1, 0);
				OpenGL32Renderer.cameraRotation(Quaternion.multiply(yaw, pitch));
			}
		}
		final Vector3 right = OpenGL32Renderer.cameraRight();
		final Vector3 up = OpenGL32Renderer.cameraUp();
		final Vector3 forward = OpenGL32Renderer.cameraForward();
		final Vector3 position = OpenGL32Renderer.cameraPosition();
		if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
			position.add(Vector3.multiply(forward, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			position.add(Vector3.multiply(forward, -cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
			position.add(Vector3.multiply(right, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
			position.add(Vector3.multiply(right, -cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
			position.add(Vector3.multiply(up, cameraSpeed));
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
			position.add(Vector3.multiply(up, -cameraSpeed));
		}
		OpenGL32Renderer.lightPosition(position);
	}

	private static void deploy() throws Exception {
		final File configFile = new File("config.yml");
		if (!configFile.exists()) {
			FileUtils.copyInputStreamToFile(Sandbox.class.getResourceAsStream("/config.yml"), configFile);
		}
		final String osPath;
		final String[] nativeLibs;
		if (SystemUtils.IS_OS_WINDOWS) {
			nativeLibs = new String[]{
					"jinput-dx8_64.dll", "jinput-dx8.dll", "jinput-raw_64.dll", "jinput-raw.dll",
					"jinput-wintab.dll", "lwjgl.dll", "lwjgl64.dll", "OpenAL32.dll", "OpenAL64.dll"
			};
			osPath = "windows/";
		} else if (SystemUtils.IS_OS_MAC) {
			nativeLibs = new String[]{
					"libjinput-osx.jnilib", "liblwjgl.jnilib", "openal.dylib"
			};
			osPath = "mac/";
		} else if (SystemUtils.IS_OS_LINUX) {
			nativeLibs = new String[]{
					"liblwjgl.so", "liblwjgl64.so", "libopenal.so", "libopenal64.so", "libjinput-linux.so",
					"libjinput-linux64.so"
			};
			osPath = "linux/";
		} else {
			throw new IllegalStateException("Could not get lwjgl natives for OS \"" + SystemUtils.OS_NAME + "\".");
		}
		final File nativesDir = new File("natives" + File.separator + osPath);
		nativesDir.mkdirs();
		for (String nativeLib : nativeLibs) {
			final File nativeFile = new File(nativesDir, nativeLib);
			if (!nativeFile.exists()) {
				FileUtils.copyInputStreamToFile(Sandbox.class.getResourceAsStream("/" + nativeLib), nativeFile);
			}
		}
		final String nativesPath = nativesDir.getAbsolutePath();
		System.setProperty("org.lwjgl.librarypath", nativesPath);
		System.setProperty("net.java.games.input.librarypath", nativesPath);
	}

	@SuppressWarnings("unchecked")
	private static void loadConfiguration() throws Exception {
		try {
			final Map<String, Object> config =
					(Map<String, Object>) new Yaml().load(new FileInputStream("config.yml"));
			final Map<String, Object> inputConfig = (Map<String, Object>) config.get("Input");
			final Map<String, Object> appearanceConfig = (Map<String, Object>) config.get("Appearance");
			mouseSensitivity = ((Number) inputConfig.get("MouseSensitivity")).floatValue();
			cameraSpeed = ((Number) inputConfig.get("CameraSpeed")).floatValue();
			final String[] windowSize = ((String) appearanceConfig.get("WindowSize")).split(",");
			windowWidth = Integer.parseInt(windowSize[0].trim());
			windowHeight = Integer.parseInt(windowSize[1].trim());
			fieldOfView = ((Number) appearanceConfig.get("FieldOfView")).floatValue();
			OpenGL32Renderer.backgroundColor(parseColor(((String) appearanceConfig.get("BackgroundColor")), 0));
			defaultAABBColor = (parseColor(((String) appearanceConfig.get("AABBColor")), 1));
			defaultShapeColor = (parseColor(((String) appearanceConfig.get("ShapeColor")), 1));
			OpenGL32Renderer.diffuseIntensity(((Number) appearanceConfig.get("DiffuseIntensity")).floatValue());
			OpenGL32Renderer.specularIntensity(((Number) appearanceConfig.get("SpecularIntensity")).floatValue());
			OpenGL32Renderer.ambientIntensity(((Number) appearanceConfig.get("AmbientIntensity")).floatValue());
			OpenGL32Renderer.lightAttenuation(((Number) appearanceConfig.get("LightAttenuation")).floatValue());
		} catch (Exception ex) {
			throw new IllegalStateException("Malformed config.yml: \"" + ex.getMessage() + "\".");
		}
	}

	private static Color parseColor(String s, float alpha) {
		final String[] ss = s.split(",");
		return new Color(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()),
				alpha);
	}
}
