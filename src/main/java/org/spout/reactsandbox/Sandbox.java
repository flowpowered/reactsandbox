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

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.flowpowered.math.TrigMath;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.yaml.snakeyaml.Yaml;

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
import org.spout.physics.constraint.SliderJoint.SliderJointInfo;
import org.spout.physics.engine.DynamicsWorld;
import org.spout.physics.math.Quaternion;
import org.spout.physics.math.Transform;
import org.spout.physics.math.Vector3;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.util.CausticUtil;

/**
 * The main class of the ReactSandbox.
 */
public class Sandbox {
    // Constants
    public static final int TARGET_FPS = 60;
    private static final float TIMESTEP = 1f / TARGET_FPS;
    private static final RigidBodyMaterial PHYSICS_MATERIAL = RigidBodyMaterial.asUnmodifiableMaterial(new RigidBodyMaterial(0.2f, 0.8f));
    public static final float SPOT_CUTOFF = (float) (TrigMath.atan(100 / 50) / 2);
    // Settings
    private static float mouseSensitivity = 0.08f;
    private static float cameraSpeed = 0.2f;
    // Physics objects
    private static DynamicsWorld world;
    private static final Vector3 gravity = new Vector3(0, -9.81f, 0);
    private static final Map<CollisionBody, Model> shapes = new HashMap<>();
    private static final Map<CollisionBody, Model> aabbs = new HashMap<>();
    // Input
    private static boolean mouseGrabbed = true;
    private static float cameraPitch = 0;
    private static float cameraYaw = 0;
    // Selection
    private static CollisionBody selected = null;
    // Rendering
    private static GLVersion glVersion;

    /**
     * Entry point for the application.
     *
     * @param args Unused
     */
    public static void main(String[] args) {
        try {
            deploy();
            loadConfiguration();
            SandboxRenderer.init();
            SandboxRenderer.addDefaultObjects();
            setupPhysics();
            startupLog();
            SandboxRenderer.getCamera().setPosition(new Vector3f(0, 5, 10));
            SandboxRenderer.setLightPosition(new Vector3f(0, 50, 50));
            SandboxRenderer.setLightDirection(new Vector3f(0, -TrigMath.cos(SPOT_CUTOFF), -TrigMath.sin(SPOT_CUTOFF)));
            Mouse.setGrabbed(true);
            SandboxRenderer.startFPSMonitor();
            long lastTime = System.currentTimeMillis();
            while (!Display.isCloseRequested()) {
                final long currentTime = System.currentTimeMillis();
                processInput((currentTime - lastTime) / 1000f);
                lastTime = currentTime;
                world.update();
                handleSelection();
                updateBodies();
                SandboxRenderer.render();
                Display.sync(TARGET_FPS);
            }
            shutdownLog();
            world.stop();
            SandboxRenderer.dispose();
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
        final Model aabbModel = SandboxRenderer.addAABB(SandboxUtil.toMathVector3(bodyPosition), SandboxUtil.toMathVector3(Vector3.subtract(aabb.getMax(), aabb.getMin())));
        aabbs.put(body, aabbModel);
        final CollisionShape shape = body.getCollisionShape();
        final Model shapeModel;
        switch (shape.getType()) {
            case BOX:
                final BoxShape box = (BoxShape) shape;
                shapeModel = SandboxRenderer.addBox(SandboxUtil.toMathVector3(bodyPosition), SandboxUtil.toMathQuaternion(bodyOrientation), SandboxUtil.toMathVector3(box.getExtent()));
                break;
            case CONE:
                shapeModel = SandboxRenderer.addDiamond(SandboxUtil.toMathVector3(bodyPosition), SandboxUtil.toMathQuaternion(bodyOrientation));
                break;
            case CYLINDER:
                final CylinderShape cylinder = (CylinderShape) shape;
                shapeModel = SandboxRenderer.addCylinder(SandboxUtil.toMathVector3(bodyPosition), SandboxUtil.toMathQuaternion(bodyOrientation), cylinder.getRadius(), cylinder.getHeight());
                break;
            case SPHERE:
                final SphereShape sphere = (SphereShape) shape;
                shapeModel = SandboxRenderer.addSphere(SandboxUtil.toMathVector3(bodyPosition), SandboxUtil.toMathQuaternion(bodyOrientation), sphere.getRadius());
                break;
            default:
                throw new IllegalArgumentException("Unsupported collision shape: " + shape.getType());
        }
        shapes.put(body, shapeModel);
        return body;
    }

    private static void removeBody(final CollisionBody body) {
        if (body == null) {
            return;
        }
        final Model shapeModel = shapes.remove(body);
        SandboxRenderer.removeModel(shapeModel);
        final Model aabbModel = aabbs.remove(body);
        SandboxRenderer.removeModel(aabbModel);
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
                shape = new ConeShape(1, 2);
                break;
            case CYLINDER:
                shape = new CylinderShape(1, 2);
                break;
            case SPHERE:
                shape = new SphereShape(1);
                break;
        }
        final Camera camera = SandboxRenderer.getCamera();
        addMobileBody(shape, 10,
                SandboxUtil.toReactVector3(camera.getPosition().add(camera.getForward().mul(5))),
                SandboxUtil.toReactQuaternion(camera.getRotation()));
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

    private static void processInput(float dt) {
        dt /= TIMESTEP;
        final boolean mouseGrabbedBefore = mouseGrabbed;
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState()) {
                switch (Keyboard.getEventKey()) {
                    case Keyboard.KEY_ESCAPE:
                        mouseGrabbed ^= true;
                        break;
                    case Keyboard.KEY_F2:
                        SandboxRenderer.saveScreenshot();
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
        final Camera camera = SandboxRenderer.getCamera();
        if (Display.isActive()) {
            if (mouseGrabbed != mouseGrabbedBefore) {
                Mouse.setGrabbed(!mouseGrabbedBefore);
            }
            if (mouseGrabbed) {
                final float sensitivity = mouseSensitivity * dt;
                cameraPitch -= Mouse.getDX() * sensitivity;
                cameraPitch %= 360;
                final Quaternion pitch = SandboxUtil.angleAxisToQuaternion(cameraPitch, 0, 1, 0);
                cameraYaw += Mouse.getDY() * sensitivity;
                cameraYaw %= 360;
                final Quaternion yaw = SandboxUtil.angleAxisToQuaternion(cameraYaw, 1, 0, 0);
                camera.setRotation(SandboxUtil.toMathQuaternion(Quaternion.multiply(pitch, yaw)));
            }
        }
        final Vector3 right = SandboxUtil.toReactVector3(camera.getRight());
        final Vector3 up = SandboxUtil.toReactVector3(camera.getUp());
        final Vector3 forward = SandboxUtil.toReactVector3(camera.getForward());
        final Vector3 position = SandboxUtil.toReactVector3(camera.getPosition());
        final float speed = cameraSpeed * dt;
        if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
            position.add(Vector3.multiply(forward, speed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
            position.add(Vector3.multiply(forward, -speed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            position.add(Vector3.multiply(right, -speed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            position.add(Vector3.multiply(right, speed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            position.add(Vector3.multiply(up, speed));
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
            position.add(Vector3.multiply(up, -speed));
        }
        camera.setPosition(SandboxUtil.toMathVector3(position));
    }

    private static void handleSelection() {
        if (selected != null) {
            aabbs.get(selected).getUniforms().getVector4("modelColor").set(SandboxRenderer.getAABBColor());
            selected = null;
        }
        final Camera camera = SandboxRenderer.getCamera();
        final IntersectedBody targeted = world.findClosestIntersectingBody(
                SandboxUtil.toReactVector3(camera.getPosition()),
                SandboxUtil.toReactVector3(camera.getForward()));
        if (targeted != null && targeted.getBody() instanceof RigidBody) {
            selected = targeted.getBody();
            aabbs.get(selected).getUniforms().getVector4("modelColor").set(CausticUtil.BLUE);
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
        final MobileRigidBody box = addMobileBody(new BoxShape(new Vector3(1, 1, 1)), 1, new Vector3(0, 6, 0), SandboxUtil.angleAxisToQuaternion(45, 1, 1, 1));
        box.setMaterial(PHYSICS_MATERIAL);
        addMobileBody(new BoxShape(new Vector3(0.28f, 0.28f, 0.28f)), 1, new Vector3(0, 6, 0), SandboxUtil.angleAxisToQuaternion(45, 1, 1, 1)).setMaterial(PHYSICS_MATERIAL);
        addMobileBody(new ConeShape(1, 2), 1, new Vector3(0, 9, 0), SandboxUtil.angleAxisToQuaternion(89, -1, -1, -1)).setMaterial(PHYSICS_MATERIAL);
        addMobileBody(new CylinderShape(1, 2), 1, new Vector3(0, 12, 0), SandboxUtil.angleAxisToQuaternion(-15, 1, -1, 1)).setMaterial(PHYSICS_MATERIAL);
        final MobileRigidBody sphere = addMobileBody(new SphereShape(1), 1, new Vector3(0, 6, 7), SandboxUtil.angleAxisToQuaternion(32, -1, -1, 1));
        sphere.setMaterial(PHYSICS_MATERIAL);
        addImmobileBody(new BoxShape(new Vector3(25, 1, 25)), 100, new Vector3(0, 1.8f, 0), Quaternion.identity()).setMaterial(PHYSICS_MATERIAL);
        addImmobileBody(new BoxShape(new Vector3(50, 1, 50)), 100, new Vector3(0, 0, 0), Quaternion.identity()).setMaterial(PHYSICS_MATERIAL);

        final Vector3 boxPosition = box.getTransform().getPosition();
        final Vector3 spherePosition = sphere.getTransform().getPosition();
        //final BallAndSocketJointInfo info = new BallAndSocketJointInfo(box, sphere, Vector3.add(boxPosition, spherePosition).divide(2));
        final SliderJointInfo info = new SliderJointInfo(box, sphere, Vector3.add(boxPosition, spherePosition).divide(2), Vector3.subtract(spherePosition, boxPosition), 0, 10, 1, 1);
        //final HingeJointInfo info = new HingeJointInfo(box, sphere, Vector3.add(boxPosition, spherePosition).divide(2), new Vector3(0, 1, 0));
        //final FixedJointInfo info = new FixedJointInfo(box, sphere, Vector3.add(boxPosition, spherePosition).divide(2));
        world.createJoint(info);
        box.setMotionEnabled(false);

        world.start();

        /*
         Broken:
            Ball and socket joint
            Hinge joint
            Fixed joint warm starting
         */

    }

    @SuppressWarnings("unchecked")
    private static void loadConfiguration() throws Exception {
        try {
            final Map<String, Object> config =
                    (Map<String, Object>) new Yaml().load(new FileInputStream("config.yml"));
            final Map<String, Object> inputConfig = (Map<String, Object>) config.get("Input");
            mouseSensitivity = ((Number) inputConfig.get("MouseSensitivity")).floatValue();
            cameraSpeed = ((Number) inputConfig.get("CameraSpeed")).floatValue();
            final Map<String, Object> appearanceConfig = (Map<String, Object>) config.get("Appearance");
            CausticUtil.setDebugEnabled((Boolean) appearanceConfig.get("Debug"));
            glVersion = GLVersion.valueOf(((String) appearanceConfig.get("GLVersion")).toUpperCase());
            SandboxRenderer.setGLVersion(glVersion);
            SandboxRenderer.setBackgroundColor(parseVector4f(((String) appearanceConfig.get("BackgroundColor")), 0));
            SandboxRenderer.setAABBColor(parseVector4f(((String) appearanceConfig.get("AABBColor")), 1));
            SandboxRenderer.setDiamondColor(parseVector4f(((String) appearanceConfig.get("ConeShapeColor")), 1));
            SandboxRenderer.setSphereColor(parseVector4f(((String) appearanceConfig.get("SphereShapeColor")), 1));
            SandboxRenderer.setCylinderColor(parseVector4f(((String) appearanceConfig.get("CylinderShapeColor")), 1));
            SandboxRenderer.setLightAttenuation(((Number) appearanceConfig.get("LightAttenuation")).floatValue());
            SandboxRenderer.setCullBackFaces((Boolean) appearanceConfig.get("CullingEnabled"));
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
        final File screenshotDir = new File("screenshots");
        if (!screenshotDir.exists()) {
            screenshotDir.mkdir();
        }
    }

    private static Vector4f parseVector4f(String s, float alpha) {
        final String[] ss = s.split(",");
        return new Vector4f(
                Float.parseFloat(ss[0].trim()),
                Float.parseFloat(ss[1].trim()),
                Float.parseFloat(ss[2].trim()),
                alpha);
    }
}
