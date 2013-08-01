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
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.yaml.snakeyaml.Yaml;

import org.spout.math.vector.Vector2;
import org.spout.physics.body.CollisionBody;
import org.spout.physics.body.ImmobileRigidBody;
import org.spout.physics.body.MobileRigidBody;
import org.spout.physics.body.RigidBody;
import org.spout.physics.body.RigidBodyMaterial;
import org.spout.physics.collision.RayCaster.IntersectedBody;
import org.spout.physics.collision.shape.AABB;
import org.spout.physics.collision.shape.BoxShape;
import org.spout.physics.collision.shape.CollisionShape;
import org.spout.physics.collision.shape.CollisionShape.CollisionShapeType;
import org.spout.physics.collision.shape.ConeShape;
import org.spout.physics.collision.shape.CylinderShape;
import org.spout.physics.collision.shape.SphereShape;
import org.spout.physics.engine.DynamicsWorld;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Transform;
import org.spout.physics.math.Vector3;
import org.spout.renderer.Camera;
import org.spout.renderer.FrameBuffer;
import org.spout.renderer.FrameBuffer.AttachmentPoint;
import org.spout.renderer.GLVersion;
import org.spout.renderer.InstancedModel;
import org.spout.renderer.Material;
import org.spout.renderer.Model;
import org.spout.renderer.Program;
import org.spout.renderer.RenderBuffer;
import org.spout.renderer.Renderer;
import org.spout.renderer.Shader.ShaderType;
import org.spout.renderer.Texture;
import org.spout.renderer.Texture.FilterMode;
import org.spout.renderer.Texture.ImageFormat;
import org.spout.renderer.VertexArray.DrawingMode;
import org.spout.renderer.data.RenderList;
import org.spout.renderer.data.Uniform.ColorUniform;
import org.spout.renderer.data.Uniform.FloatUniform;
import org.spout.renderer.data.Uniform.Vector3Uniform;
import org.spout.renderer.data.UniformHolder;
import org.spout.renderer.data.VertexData;
import org.spout.renderer.loader.ObjFileLoader;
import org.spout.renderer.util.StringModel;

/**
 * The main class of the ReactSandbox.
 */
public class Sandbox {
	// Constants
	private static final String WINDOW_TITLE = "Sandbox";
	private static final int TARGET_FPS = 60;
	private static final float TIMESTEP = 1f / TARGET_FPS;
	private static final RigidBodyMaterial PHYSICS_MATERIAL = RigidBodyMaterial.asUnmodifiableMaterial(new RigidBodyMaterial(0.2f, 0.8f));
	// Settings
	private static boolean cullingEnabled = true;
	private static float mouseSensitivity = 0.08f;
	private static float cameraSpeed = 0.2f;
	private static int windowWidth = 1200;
	private static int windowHeight = 800;
	private static float fieldOfView = 75;
	private static Color aabbColor;
	private static Color coneShapeColor;
	private static Color cylinderShapeColor;
	private static Color sphereShapeColor;
	// Physics objects
	private static DynamicsWorld world;
	private static Vector3 gravity = new Vector3(0, -9.81f, 0);
	private static final Map<CollisionBody, Model> shapes = new HashMap<>();
	private static final Map<CollisionBody, Model> aabbs = new HashMap<>();
	// Input
	private static boolean mouseGrabbed = true;
	private static float cameraPitch = 0;
	private static float cameraYaw = 0;
	// Performance monitoring
	private static final FPSMonitor fpsMonitor = new FPSMonitor();
	// Selection
	private static CollisionBody selected = null;
	// Rendering
	private static GLVersion glVersion;
	private static Renderer renderer;
	private static Color backgroundColor;
	private static Camera modelCamera;
	private static RenderList modelList;
	private static RenderList guiList;
	private static RenderList screenList;
	private static final VertexData diamondModel = ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/diamond.obj"));
	private static StringModel fpsMonitorModel;
	// Rendering material for objects
	private static Material solidMaterial;
	private static Material texturedMaterial;
	private static Material wireframeMaterial;
	private static Material mobMaterial;
	private static Material screenMaterial;
	// Lighting
	private static org.spout.math.vector.Vector3 lightPosition = new org.spout.math.vector.Vector3(0, 0, 0);
	private static float diffuseIntensity = 0.8f;
	private static float specularIntensity = 0.2f;
	private static float ambientIntensity = 0.3f;
	private static float lightAttenuation = 0.03f;

	/**
	 * Entry point for the application.
	 *
	 * @param args Unused
	 */
	public static void main(String[] args) {
		try {
			deploy();
			loadConfiguration();
			setupRenderer();
			addCrosshairs();
			addFPSMonitor();
			addScreen();
			addMob();
			setupPhysics();
			startupLog();
			modelCamera.setPosition(new org.spout.math.vector.Vector3(0, 5, 10));
			fpsMonitor.start();
			Mouse.setGrabbed(true);
			while (!Display.isCloseRequested()) {
				processInput();
				world.update();
				handleSelection();
				updateBodies();
				updateFPSMonitor();
				renderer.render();
				Display.sync(TARGET_FPS);
			}
			shutdownLog();
			world.stop();
			renderer.destroy();
		} catch (Exception ex) {
			ex.printStackTrace();
			final String name = ex.getClass().getSimpleName();
			final String message = ex.getMessage();
			Sys.alert("Error: " + name, message == null || message.trim().equals("") ? name : message);
			System.exit(-1);
		}
	}

	private static ImmobileRigidBody addImmobileBody(CollisionShape shape, float mass, Vector3 position, Quaternion orientation) {
		final ImmobileRigidBody body = world.createImmobileRigidBody(new Transform(position, orientation), mass, shape);
		addBody(body);
		return body;
	}

	private static MobileRigidBody addMobileBody(CollisionShape shape, float mass, Vector3 position, Quaternion orientation) {
		final MobileRigidBody body = world.createMobileRigidBody(new Transform(position, orientation), mass, shape);
		addBody(body);
		return body;
	}

	private static CollisionBody addBody(CollisionBody body) {
		final Transform bodyTransform = body.getTransform();
		final Vector3 bodyPosition = bodyTransform.getPosition();
		final Quaternion bodyOrientation = bodyTransform.getOrientation();
		final AABB aabb = body.getAABB();
		final Model aabbModel = glVersion.createModel();
		aabbModel.getVertexArray().setVertexData(MeshGenerator.generateWireCuboid(null, new Vector3(1, 1, 1)));
		aabbModel.setMaterial(wireframeMaterial);
		aabbModel.getVertexArray().setDrawingMode(DrawingMode.LINES);
		aabbModel.setScale(SandboxUtil.toMathVector3(Vector3.subtract(aabb.getMax(), aabb.getMin())));
		aabbModel.setPosition(SandboxUtil.toMathVector3(bodyPosition));
		aabbModel.getUniforms().add(new ColorUniform("modelColor", aabbColor));
		aabbModel.create();
		modelList.add(aabbModel);
		aabbs.put(body, aabbModel);
		final CollisionShape shape = body.getCollisionShape();
		final Model shapeModel = glVersion.createModel();
		final VertexData data;
		switch (shape.getType()) {
			case BOX:
				final BoxShape box = (BoxShape) shape;
				data = MeshGenerator.generateTexturedCuboid(null, Vector3.multiply(box.getExtent(), 2));
				break;
			case CONE:
				data = diamondModel;
				shapeModel.getUniforms().add(new ColorUniform("modelColor", coneShapeColor));
				break;
			case CYLINDER:
				final CylinderShape cylinder = (CylinderShape) shape;
				data = MeshGenerator.generateCylinder(null, cylinder.getRadius(), cylinder.getHeight());
				shapeModel.getUniforms().add(new ColorUniform("modelColor", cylinderShapeColor));
				break;
			case SPHERE:
				final SphereShape sphere = (SphereShape) shape;
				data = MeshGenerator.generateSphere(null, sphere.getRadius());
				shapeModel.getUniforms().add(new ColorUniform("modelColor", sphereShapeColor));
				break;
			default:
				throw new IllegalArgumentException("Unsupported collision shape: " + shape.getType());
		}
		shapeModel.getVertexArray().setVertexData(data);
		if (shape.getType() == CollisionShapeType.BOX) {
			shapeModel.setMaterial(texturedMaterial);
		} else {
			shapeModel.setMaterial(solidMaterial);
		}
		shapeModel.setPosition(SandboxUtil.toMathVector3(bodyPosition));
		shapeModel.setRotation(SandboxUtil.toMathQuaternion(bodyOrientation));
		shapeModel.create();
		modelList.add(shapeModel);
		shapes.put(body, shapeModel);
		return body;
	}

	private static void removeBody(final CollisionBody body) {
		if (body == null) {
			return;
		}
		final Model shapeModel = shapes.remove(body);
		modelList.remove(shapeModel);
		shapeModel.destroy();
		final Model aabbModel = aabbs.remove(body);
		modelList.remove(aabbModel);
		aabbModel.destroy();
		if (body instanceof RigidBody) {
			world.destroyRigidBody((RigidBody) body);
		}
	}

	private static void spawnBody(final CollisionShapeType type) {
		CollisionShape shape = null;
		switch (type) {
			case BOX:
				shape = new BoxShape(1, 1, 1);
				break;
			case CONE:
				shape = new ConeShape(1, 1);
				break;
			case CYLINDER:
				shape = new CylinderShape(1, 1);
				break;
			case SPHERE:
				shape = new SphereShape(1);
				break;
		}
		addMobileBody(shape, 10,
				SandboxUtil.toReactVector3(modelCamera.getPosition().add(modelCamera.getForward().mul(5))),
				SandboxUtil.toReactQuaternion(modelCamera.getRotation()));
	}

	private static void updateBodies() {
		for (Entry<CollisionBody, Model> entry : shapes.entrySet()) {
			final CollisionBody body = entry.getKey();
			final Model shape = entry.getValue();
			final Model aabbModel = aabbs.get(body);
			final AABB aabb = body.getAABB();
			final Transform transform = body.getInterpolatedTransform();
			final Vector3 position = transform.getPosition();
			aabbModel.setPosition(SandboxUtil.toMathVector3(position));
			aabbModel.setScale(SandboxUtil.toMathVector3(Vector3.subtract(aabb.getMax(), aabb.getMin())));
			shape.setPosition(SandboxUtil.toMathVector3(position));
			shape.setRotation(SandboxUtil.toMathQuaternion(transform.getOrientation()));
		}
	}

	private static void updateFPSMonitor() {
		fpsMonitor.update();
		fpsMonitorModel.setString("FPS: " + fpsMonitor.getFPS());
	}

	private static void processInput() {
		final boolean mouseGrabbedBefore = mouseGrabbed;
		while (Keyboard.next()) {
			if (Keyboard.getEventKeyState()) {
				switch (Keyboard.getEventKey()) {
					case Keyboard.KEY_ESCAPE:
						mouseGrabbed ^= true;
				}
			}
		}
		while (Mouse.next()) {
			if (Mouse.getEventButtonState() && mouseGrabbed) {
				switch (Mouse.getEventButton()) {
					case 0: // Left Button
						spawnBody(CollisionShapeType.CONE);
						break;
					case 1: // Right Button
						removeBody(selected);
						selected = null;
				}
			}
		}
		if (Display.isActive()) {
			if (mouseGrabbed != mouseGrabbedBefore) {
				Mouse.setGrabbed(!mouseGrabbedBefore);
			}
			if (mouseGrabbed) {
				cameraPitch -= Mouse.getDX() * mouseSensitivity;
				cameraPitch %= 360;
				final Quaternion pitch = SandboxUtil.angleAxisToQuaternion(cameraPitch, 0, 1, 0);
				cameraYaw += Mouse.getDY() * mouseSensitivity;
				cameraYaw %= 360;
				final Quaternion yaw = SandboxUtil.angleAxisToQuaternion(cameraYaw, 1, 0, 0);
				modelCamera.setRotation(SandboxUtil.toMathQuaternion(Quaternion.multiply(pitch, yaw)));
			}
		}
		final Vector3 right = SandboxUtil.toReactVector3(modelCamera.getRight());
		final Vector3 up = SandboxUtil.toReactVector3(modelCamera.getUp());
		final Vector3 forward = SandboxUtil.toReactVector3(modelCamera.getForward());
		final Vector3 position = SandboxUtil.toReactVector3(modelCamera.getPosition());
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
		modelCamera.setPosition(SandboxUtil.toMathVector3(position));
		solidMaterial.getUniforms().getVector3("lightPosition").set(SandboxUtil.toMathVector3(position));
		texturedMaterial.getUniforms().getVector3("lightPosition").set(SandboxUtil.toMathVector3(position));
		mobMaterial.getUniforms().getVector3("lightPosition").set(SandboxUtil.toMathVector3(position));
	}

	private static void handleSelection() {
		if (selected != null) {
			aabbs.get(selected).getUniforms().getColor("modelColor").set(aabbColor);
			selected = null;
		}
		final IntersectedBody targeted = world.findClosestIntersectingBody(
				SandboxUtil.toReactVector3(modelCamera.getPosition()),
				SandboxUtil.toReactVector3(modelCamera.getForward()));
		if (targeted != null && targeted.getBody() instanceof RigidBody) {
			selected = targeted.getBody();
			aabbs.get(selected).getUniforms().getColor("modelColor").set(Color.BLUE);
		}
	}

	private static void startupLog() {
		System.out.println("Starting up");
		System.out.println("Render Mode: " + glVersion);
		System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
	}

	private static void shutdownLog() {
		System.out.println("Shutting down");
	}

	private static void setupPhysics() {
		world = new DynamicsWorld(gravity, TIMESTEP);
		addMobileBody(new BoxShape(new Vector3(1, 1, 1)), 1, new Vector3(0, 6, 0), SandboxUtil.angleAxisToQuaternion(45, 1, 1, 1)).setMaterial(PHYSICS_MATERIAL);
		addMobileBody(new BoxShape(new Vector3(0.28f, 0.28f, 0.28f)), 1, new Vector3(0, 6, 0), SandboxUtil.angleAxisToQuaternion(45, 1, 1, 1)).setMaterial(PHYSICS_MATERIAL);
		addMobileBody(new ConeShape(1, 2), 1, new Vector3(0, 9, 0), SandboxUtil.angleAxisToQuaternion(89, -1, -1, -1)).setMaterial(PHYSICS_MATERIAL);
		addMobileBody(new CylinderShape(1, 2), 1, new Vector3(0, 12, 0), SandboxUtil.angleAxisToQuaternion(-15, 1, -1, 1)).setMaterial(PHYSICS_MATERIAL);
		addMobileBody(new SphereShape(1), 1, new Vector3(0, 15, 0), SandboxUtil.angleAxisToQuaternion(32, -1, -1, 1)).setMaterial(PHYSICS_MATERIAL);
		addImmobileBody(new BoxShape(new Vector3(25, 1, 25)), 100, new Vector3(0, 1.8f, 0), Quaternion.identity()).setMaterial(PHYSICS_MATERIAL);
		addImmobileBody(new BoxShape(new Vector3(50, 1, 50)), 100, new Vector3(0, 0, 0), Quaternion.identity()).setMaterial(PHYSICS_MATERIAL);
		world.start();
	}

	private static void addCrosshairs() {
		final Model model = glVersion.createModel();
		model.getVertexArray().setVertexData(MeshGenerator.generateCrosshairs(null, 0.04f));
		model.setMaterial(wireframeMaterial);
		model.getVertexArray().setDrawingMode(DrawingMode.LINES);
		model.getUniforms().add(new ColorUniform("modelColor", Color.WHITE));
		// Necessary for GL20 because there's no depth clamping. The GUI models must be just in front of the camera
		model.setPosition(new org.spout.math.vector.Vector3(0, 0, -0.001f));
		model.create();
		guiList.add(model);
	}

	private static void addFPSMonitor() {
		final StringModel model = new StringModel();
		model.setGLVersion(glVersion);
		model.setGlyphs("FPS: 0123456789");
		model.setFont(new Font("Arial", Font.PLAIN, 128));
		model.create();
		model.setPosition(new org.spout.math.vector.Vector3(-0.97, 0.6, -0.001f));
		model.setScale(new org.spout.math.vector.Vector3(0.35, 0.35, 0.35));
		model.setString("FPS: " + fpsMonitor.getFPS());
		guiList.add(model);
		fpsMonitorModel = model;
	}

	private static void addMob() {
		final Model model = glVersion.createModel();
		model.getVertexArray().setVertexData(ObjFileLoader.load(Sandbox.class.getResourceAsStream("/models/creeper.obj")));
		model.setMaterial(mobMaterial);
		model.setPosition(new org.spout.math.vector.Vector3(10, 10, 0));
		model.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(-90, 0, 1, 0));
		model.create();
		modelList.add(model);
		// Add a second mob, instanced from the first one
		final Model instancedMobModel = new InstancedModel(model);
		instancedMobModel.create();
		instancedMobModel.setPosition(new org.spout.math.vector.Vector3(-10, 10, 0));
		instancedMobModel.setRotation(org.spout.math.imaginary.Quaternion.fromAngleDegAxis(90, 0, 1, 0));
		modelList.add(instancedMobModel);
	}

	private static void addScreen() {
		final Model model = glVersion.createModel();
		final float aspect = (float) windowHeight / windowWidth;
		model.getVertexArray().setVertexData(MeshGenerator.generateScreenPlane(null, new Vector2(2, 2 * aspect)));
		model.setMaterial(screenMaterial);
		model.setPosition(new org.spout.math.vector.Vector3(0, 0, -0.001f));
		model.create();
		screenList.add(model);
	}

	private static void setupRenderer() throws Exception {
		// Create the renderer
		renderer = glVersion.createRenderer();
		renderer.setBackgroundColor(backgroundColor);
		renderer.setWindowTitle(WINDOW_TITLE);
		renderer.setWindowSize(windowWidth, windowHeight);
		renderer.setCullingEnabled(cullingEnabled);
		renderer.create();
		// Rendering cameras
		modelCamera = Camera.createPerspective(fieldOfView, windowWidth, windowHeight, 0.001f, 100);
		final float aspect = (float) windowHeight / windowWidth;
		final Camera guiCamera = Camera.createOrthographic(1, -1, 1 * aspect, -1 * aspect, 0.001f, 100);
		// Create and add the render lists
		modelList = new RenderList("models", modelCamera, 0);
		guiList = new RenderList("gui", guiCamera, 1);
		screenList = new RenderList("screen", guiCamera, 2);
		renderer.addRenderList(modelList);
		renderer.addRenderList(guiList);
		renderer.addRenderList(screenList);
		// Path to the shaders of the correct version
		final String shaderPath = "/shaders/" + glVersion.toString().toLowerCase() + "/";

		// Solid material
		solidMaterial = glVersion.createMaterial();
		final Program solidProgram = solidMaterial.getProgram();
		solidProgram.addShaderSource(ShaderType.VERTEX, Sandbox.class.getResourceAsStream(shaderPath + "solid.vert"));
		solidProgram.addShaderSource(ShaderType.FRAGMENT, Sandbox.class.getResourceAsStream(shaderPath + "solid.frag"));
		if (glVersion == GLVersion.GL20) {
			solidProgram.addAttributeLayout("position", 0);
			solidProgram.addAttributeLayout("normal", 1);
		}
		final UniformHolder solidUniforms = solidMaterial.getUniforms();
		solidUniforms.add(new Vector3Uniform("lightPosition", lightPosition));
		solidUniforms.add(new FloatUniform("diffuseIntensity", diffuseIntensity));
		solidUniforms.add(new FloatUniform("specularIntensity", specularIntensity));
		solidUniforms.add(new FloatUniform("ambientIntensity", ambientIntensity));
		solidUniforms.add(new FloatUniform("lightAttenuation", lightAttenuation));
		solidMaterial.create();

		// Textured material
		texturedMaterial = glVersion.createMaterial();
		final Program texturedProgram = texturedMaterial.getProgram();
		texturedProgram.addShaderSource(ShaderType.VERTEX, Sandbox.class.getResourceAsStream(shaderPath + "textured.vert"));
		texturedProgram.addShaderSource(ShaderType.FRAGMENT, Sandbox.class.getResourceAsStream(shaderPath + "textured.frag"));
		if (glVersion == GLVersion.GL20) {
			texturedProgram.addAttributeLayout("position", 0);
			texturedProgram.addAttributeLayout("normal", 1);
			texturedProgram.addAttributeLayout("textureCoords", 2);
		}
		texturedProgram.addTextureLayout("diffuse", 0);
		texturedProgram.addTextureLayout("specular", 1);
		final UniformHolder texturedUniforms = texturedMaterial.getUniforms();
		texturedUniforms.add(new Vector3Uniform("lightPosition", lightPosition));
		texturedUniforms.add(new FloatUniform("diffuseIntensity", diffuseIntensity));
		texturedUniforms.add(new FloatUniform("specularIntensity", specularIntensity));
		texturedUniforms.add(new FloatUniform("ambientIntensity", ambientIntensity));
		texturedUniforms.add(new FloatUniform("lightAttenuation", lightAttenuation));
		texturedMaterial.create();
		// Wood diffuse texture
		final Texture diffuseTexture = glVersion.createTexture();
		diffuseTexture.setFormat(ImageFormat.RGB);
		diffuseTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/wood_diffuse.jpg"));
		diffuseTexture.setMagFilter(FilterMode.LINEAR);
		diffuseTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		diffuseTexture.setUnit(0);
		diffuseTexture.create();
		texturedMaterial.addTexture(diffuseTexture);
		// Wood specular texture
		final Texture specularTexture = glVersion.createTexture();
		specularTexture.setFormat(ImageFormat.RED);
		specularTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/wood_specular.png"));
		specularTexture.setMagFilter(FilterMode.LINEAR);
		specularTexture.setMinFilter(FilterMode.LINEAR_MIPMAP_LINEAR);
		specularTexture.setUnit(1);
		specularTexture.create();
		texturedMaterial.addTexture(specularTexture);

		// Wireframe material
		wireframeMaterial = glVersion.createMaterial();
		final Program wireframeProgram = wireframeMaterial.getProgram();
		wireframeProgram.addShaderSource(ShaderType.VERTEX, Sandbox.class.getResourceAsStream(shaderPath + "wireframe.vert"));
		wireframeProgram.addShaderSource(ShaderType.FRAGMENT, Sandbox.class.getResourceAsStream(shaderPath + "wireframe.frag"));
		if (glVersion == GLVersion.GL20) {
			wireframeProgram.addAttributeLayout("position", 0);
		}
		wireframeMaterial.create();

		// Mob material
		mobMaterial = glVersion.createMaterial();
		final Program mobProgram = mobMaterial.getProgram();
		mobProgram.addShaderSource(ShaderType.VERTEX, Sandbox.class.getResourceAsStream(shaderPath + "textured.vert"));
		mobProgram.addShaderSource(ShaderType.FRAGMENT, Sandbox.class.getResourceAsStream(shaderPath + "textured.frag"));
		if (glVersion == GLVersion.GL20) {
			mobProgram.addAttributeLayout("position", 0);
			mobProgram.addAttributeLayout("normal", 1);
			mobProgram.addAttributeLayout("textureCoords", 2);
		}
		mobProgram.addTextureLayout("diffuse", 0);
		final UniformHolder mobUniforms = mobMaterial.getUniforms();
		mobUniforms.add(new Vector3Uniform("lightPosition", lightPosition));
		mobUniforms.add(new FloatUniform("diffuseIntensity", diffuseIntensity));
		mobUniforms.add(new FloatUniform("specularIntensity", specularIntensity));
		mobUniforms.add(new FloatUniform("ambientIntensity", ambientIntensity));
		mobUniforms.add(new FloatUniform("lightAttenuation", lightAttenuation));
		mobMaterial.create();
		final Texture mobTexture = glVersion.createTexture();
		mobTexture.setFormat(ImageFormat.RGB);
		mobTexture.setImageData(Sandbox.class.getResourceAsStream("/textures/creeper.png"));
		// For low resolution textures, always use NEAREST
		mobTexture.setMagFilter(FilterMode.NEAREST);
		mobTexture.setMinFilter(FilterMode.NEAREST);
		mobTexture.setUnit(0);
		mobTexture.create();
		mobMaterial.addTexture(mobTexture);

		// Screen material
		screenMaterial = glVersion.createMaterial();
		final Program screenProgram = screenMaterial.getProgram();
		screenProgram.addShaderSource(ShaderType.VERTEX, Sandbox.class.getResourceAsStream(shaderPath + "screen.vert"));
		screenProgram.addShaderSource(ShaderType.FRAGMENT, Sandbox.class.getResourceAsStream(shaderPath + "screen.frag"));
		if (glVersion == GLVersion.GL20) {
			screenProgram.addAttributeLayout("position", 0);
			screenProgram.addAttributeLayout("textureCoords", 1);
		}
		screenProgram.addTextureLayout("diffuse", 0);
		screenMaterial.create();
		final Texture screenTexture = glVersion.createTexture();
		screenTexture.setFormat(ImageFormat.RGBA);
		// Image data will be filled by the frame buffer
		screenTexture.setImageData((ByteBuffer) null, windowWidth, windowHeight);
		screenTexture.setMagFilter(FilterMode.LINEAR);
		screenTexture.setMinFilter(FilterMode.LINEAR);
		screenTexture.setUnit(0);
		screenTexture.create();
		screenMaterial.addTexture(screenTexture);
		// Create the frame buffer
		final FrameBuffer frameBuffer = glVersion.createFrameBuffer();
		// Attach the texture for the color
		frameBuffer.attach(AttachmentPoint.COLOR0, screenTexture);
		// And a render buffer for the depth
		final RenderBuffer renderBuffer = glVersion.createRenderBuffer();
		renderBuffer.setFormat(ImageFormat.DEPTH);
		renderBuffer.setSize(windowWidth, windowHeight);
		renderBuffer.create();
		frameBuffer.attach(AttachmentPoint.DEPTH, renderBuffer);
		// Add the frame buffer to the model list so it will render to it
		frameBuffer.create();
		modelList.setFrameBuffer(frameBuffer);
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
			glVersion = GLVersion.valueOf(((String) appearanceConfig.get("GLVersion")).toUpperCase());
			final String[] windowSize = ((String) appearanceConfig.get("WindowSize")).split(",");
			windowWidth = Integer.parseInt(windowSize[0].trim());
			windowHeight = Integer.parseInt(windowSize[1].trim());
			fieldOfView = ((Number) appearanceConfig.get("FieldOfView")).floatValue();
			backgroundColor = parseColor(((String) appearanceConfig.get("BackgroundColor")), 0);
			aabbColor = parseColor(((String) appearanceConfig.get("AABBColor")), 1);
			coneShapeColor = parseColor(((String) appearanceConfig.get("ConeShapeColor")), 1);
			sphereShapeColor = parseColor(((String) appearanceConfig.get("SphereShapeColor")), 1);
			cylinderShapeColor = parseColor(((String) appearanceConfig.get("CylinderShapeColor")), 1);
			diffuseIntensity = ((Number) appearanceConfig.get("DiffuseIntensity")).floatValue();
			specularIntensity = ((Number) appearanceConfig.get("SpecularIntensity")).floatValue();
			ambientIntensity = ((Number) appearanceConfig.get("AmbientIntensity")).floatValue();
			lightAttenuation = ((Number) appearanceConfig.get("LightAttenuation")).floatValue();
			cullingEnabled = (Boolean) appearanceConfig.get("CullingEnabled");
		} catch (Exception ex) {
			throw new IllegalStateException("Malformed config.yml: \"" + ex.getMessage() + "\".", ex);
		}
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

	private static Color parseColor(String s, float alpha) {
		final String[] ss = s.split(",");
		return new Color(
				Float.parseFloat(ss[0].trim()),
				Float.parseFloat(ss[1].trim()),
				Float.parseFloat(ss[2].trim()),
				alpha);
	}
}
