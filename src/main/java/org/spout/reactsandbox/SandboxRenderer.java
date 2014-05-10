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

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.TrigMath;
import com.flowpowered.math.imaginary.Quaternionf;
import com.flowpowered.math.matrix.Matrix4f;
import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector2i;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector4f;

import gnu.trove.list.TFloatList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import org.lwjgl.opengl.GLContext;

import org.spout.renderer.api.Action.RenderModelsAction;
import org.spout.renderer.api.Camera;
import org.spout.renderer.api.GLImplementation;
import org.spout.renderer.api.GLVersioned.GLVersion;
import org.spout.renderer.api.Material;
import org.spout.renderer.api.Pipeline;
import org.spout.renderer.api.Pipeline.PipelineBuilder;
import org.spout.renderer.api.data.ShaderSource;
import org.spout.renderer.api.data.Uniform.FloatUniform;
import org.spout.renderer.api.data.Uniform.IntUniform;
import org.spout.renderer.api.data.Uniform.Matrix4Uniform;
import org.spout.renderer.api.data.Uniform.Vector2Uniform;
import org.spout.renderer.api.data.Uniform.Vector3Uniform;
import org.spout.renderer.api.data.Uniform.Vector4Uniform;
import org.spout.renderer.api.data.UniformHolder;
import org.spout.renderer.api.data.VertexAttribute;
import org.spout.renderer.api.data.VertexAttribute.DataType;
import org.spout.renderer.api.data.VertexData;
import org.spout.renderer.api.gl.Context;
import org.spout.renderer.api.gl.Context.Capability;
import org.spout.renderer.api.gl.FrameBuffer;
import org.spout.renderer.api.gl.FrameBuffer.AttachmentPoint;
import org.spout.renderer.api.gl.Program;
import org.spout.renderer.api.gl.Shader;
import org.spout.renderer.api.gl.Texture;
import org.spout.renderer.api.gl.Texture.CompareMode;
import org.spout.renderer.api.gl.Texture.FilterMode;
import org.spout.renderer.api.gl.Texture.Format;
import org.spout.renderer.api.gl.Texture.InternalFormat;
import org.spout.renderer.api.gl.Texture.WrapMode;
import org.spout.renderer.api.gl.VertexArray;
import org.spout.renderer.api.gl.VertexArray.DrawingMode;
import org.spout.renderer.api.model.Model;
import org.spout.renderer.api.model.StringModel;
import org.spout.renderer.api.util.CausticUtil;
import org.spout.renderer.api.util.ColladaFileLoader;
import org.spout.renderer.api.util.MeshGenerator;
import org.spout.renderer.api.util.ObjFileLoader;
import org.spout.renderer.api.util.Rectangle;
import org.spout.renderer.lwjgl.LWJGLUtil;

public class SandboxRenderer {
    // CONSTANTS
    private static final String WINDOW_TITLE = "Sandbox";
    private static final Vector2i WINDOW_SIZE = new Vector2i(1200, 800);
    private static final Vector2i SHADOW_SIZE = new Vector2i(2048, 2048);
    private static final float ASPECT_RATIO = WINDOW_SIZE.getX() / (float) WINDOW_SIZE.getY();
    private static final float FIELD_OF_VIEW = 60;
    private static final float TAN_HALF_FOV = (float) Math.tan(Math.toRadians(FIELD_OF_VIEW) / 2);
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 1000;
    private static final Vector2f PROJECTION = new Vector2f(FAR_PLANE / (FAR_PLANE - NEAR_PLANE), (-FAR_PLANE * NEAR_PLANE) / (FAR_PLANE - NEAR_PLANE));
    private static final DateFormat SCREENSHOT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
    // SETTINGS
    private static Vector4f backgroundColor = CausticUtil.DARK_GRAY;
    private static boolean cullBackFaces = true;
    // EFFECT UNIFORMS
    private static final Vector3Uniform lightPositionUniform = new Vector3Uniform("lightPosition", Vector3f.ZERO);
    private static final Vector3Uniform spotDirectionUniform = new Vector3Uniform("spotDirection", new Vector3f(0, 0, -1));
    private static final FloatUniform lightAttenuationUniform = new FloatUniform("lightAttenuation", 0.03f);
    private static final Matrix4Uniform inverseViewMatrixUniform = new Matrix4Uniform("inverseViewMatrix", new Matrix4f());
    private static final Matrix4Uniform lightViewMatrixUniform = new Matrix4Uniform("lightViewMatrix", new Matrix4f());
    private static final Matrix4Uniform lightProjectionMatrixUniform = new Matrix4Uniform("lightProjectionMatrix", new Matrix4f());
    private static final Matrix4Uniform previousViewMatrixUniform = new Matrix4Uniform("previousViewMatrix", new Matrix4f());
    private static final Matrix4Uniform previousProjectionMatrixUniform = new Matrix4Uniform("previousProjectionMatrix", new Matrix4f());
    private static final FloatUniform blurStrengthUniform = new FloatUniform("blurStrength", 1);
    // CAMERAS
    private static final Camera modelCamera = Camera.createPerspective(FIELD_OF_VIEW, WINDOW_SIZE.getX(), WINDOW_SIZE.getY(), NEAR_PLANE, FAR_PLANE);
    private static final Camera lightCamera = Camera.createPerspective((float) TrigMath.RAD_TO_DEG * Sandbox.SPOT_CUTOFF * 2, 1, 1, 0.1f, (float) GenericMath.length(50d, 100d));
    private static final Camera guiCamera = Camera.createOrthographic(1, 0, 1 / ASPECT_RATIO, 0, NEAR_PLANE, FAR_PLANE);
    // CONTEXT
    private static Context context;
    // RENDER LISTS
    private static final List<Model> modelRenderList = new ArrayList<>();
    private static final List<Model> guiRenderList = new ArrayList<>();
    // PIPELINE
    private static Pipeline pipeline;
    // SHADERS
    private static final Map<String, Program> programs = new HashMap<>();
    // TEXTURES
    private static Texture creeperDiffuseTexture;
    private static Texture creeperNormalsTexture;
    private static Texture creeperSpecularTexture;
    private static Texture woodDiffuseTexture;
    private static Texture woodNormalsTexture;
    private static Texture woodSpecularTexture;
    private static Texture colorsTexture;
    private static Texture normalsTexture;
    private static Texture vertexNormals;
    private static Texture materialsTexture;
    private static Texture velocitiesTexture;
    private static Texture depthsTexture;
    private static Texture lightDepthsTexture;
    private static Texture ssaoTexture;
    private static Texture shadowTexture;
    private static Texture auxRTexture;
    private static Texture auxRGBATexture;
    // MATERIALS
    private static Material solidMaterial;
    private static Material wireframeMaterial;
    private static Material creeperMaterial;
    private static Material woodMaterial;
    private static Material ssaoMaterial;
    private static Material blurMaterial;
    private static Material shadowMaterial;
    private static Material lightingMaterial;
    private static Material motionBlurMaterial;
    private static Material antiAliasingMaterial;
    private static Material screenMaterial;
    // FRAME BUFFERS
    private static FrameBuffer modelFrameBuffer;
    private static FrameBuffer lightModelFrameBuffer;
    private static FrameBuffer ssaoFrameBuffer;
    private static FrameBuffer blurFrameBuffer;
    private static FrameBuffer shadowFrameBuffer;
    private static FrameBuffer lightingFrameBuffer;
    private static FrameBuffer motionBlurFrameBuffer;
    private static FrameBuffer antiAliasingFrameBuffer;
    // VERTEX ARRAYS
    private static VertexArray unitCubeWireVertexArray;
    private static VertexArray diamondModelVertexArray;
    private static VertexArray deferredStageScreenVertexArray;
    // EFFECTS
    private static SSAOEffect ssaoEffect;
    private static ShadowMappingEffect shadowMappingEffect;
    private static BlurEffect blurEffect;
    // MODEL PROPERTIES
    private static Vector4f aabbModelColor;
    private static Vector4f diamondModelColor;
    private static Vector4f cylinderModelColor;
    private static Vector4f sphereModelColor;
    // MODELS
    private static Model movingMobModel;
    // FPS MONITOR
    private static final FPSMonitor fpsMonitor = new FPSMonitor();
    private static StringModel fpsMonitorModel;

    public static void init() {
        initContext();
        initEffects();
        initPrograms();
        initTextures();
        initMaterials();
        initFrameBuffers();
        initVertexArrays();
        initPipeline();
    }

    private static void initContext() {
        // CONTEXT
        context.setWindowTitle(WINDOW_TITLE);
        context.setWindowSize(WINDOW_SIZE);
        context.create();
        context.setClearColor(backgroundColor);
        context.setCamera(modelCamera);
        if (cullBackFaces) {
            context.enableCapability(Capability.CULL_FACE);
        }
        context.enableCapability(Capability.DEPTH_TEST);
        if (context.getGLVersion() == GLVersion.GL32 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            context.enableCapability(Capability.DEPTH_CLAMP);
        }
        final UniformHolder uniforms = context.getUniforms();
        uniforms.add(previousViewMatrixUniform);
        uniforms.add(previousProjectionMatrixUniform);
    }

    private static void initEffects() {
        final int blurSize = 2;
        // SSAO
        ssaoEffect = new SSAOEffect(context, WINDOW_SIZE, 8, blurSize, 0.5f, 0.15f, 2);
        // SHADOW MAPPING
        shadowMappingEffect = new ShadowMappingEffect(context, WINDOW_SIZE, 8, blurSize, 0.000006f, 0.0004f);
        // BLUR
        blurEffect = new BlurEffect(WINDOW_SIZE, blurSize);
    }

    private static void initPipeline() {
        final GLVersion glVersion = context.getGLVersion();
        PipelineBuilder pipelineBuilder = new PipelineBuilder();
        // MODEL
        pipelineBuilder = pipelineBuilder.bindFrameBuffer(modelFrameBuffer).clearBuffer().renderModels(modelRenderList).unbindFrameBuffer(modelFrameBuffer);
        // LIGHT MODEL
        pipelineBuilder = pipelineBuilder.useViewPort(new Rectangle(Vector2i.ZERO, SHADOW_SIZE)).useCamera(lightCamera).bindFrameBuffer(lightModelFrameBuffer).clearBuffer()
                .renderModels(modelRenderList).unbindFrameBuffer(lightModelFrameBuffer).useViewPort(new Rectangle(Vector2i.ZERO, WINDOW_SIZE)).useCamera(modelCamera);
        // SSAO
        if (glVersion == GLVersion.GL32 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            pipelineBuilder = pipelineBuilder.disableCapabilities(Capability.DEPTH_CLAMP);
        }
        pipelineBuilder = pipelineBuilder.disableCapabilities(Capability.DEPTH_TEST).doAction(new DoDeferredStageAction(ssaoFrameBuffer, deferredStageScreenVertexArray, ssaoMaterial));
        // SHADOW
        pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(shadowFrameBuffer, deferredStageScreenVertexArray, shadowMaterial));
        // BLUR
        pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(blurFrameBuffer, deferredStageScreenVertexArray, blurMaterial));
        // LIGHTING
        pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(lightingFrameBuffer, deferredStageScreenVertexArray, lightingMaterial));
        // MOTION BLUR
        pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(motionBlurFrameBuffer, deferredStageScreenVertexArray, motionBlurMaterial));
        // ANTI ALIASING
        pipelineBuilder = pipelineBuilder.doAction(new DoDeferredStageAction(antiAliasingFrameBuffer, deferredStageScreenVertexArray, antiAliasingMaterial))
                .unbindFrameBuffer(antiAliasingFrameBuffer).enableCapabilities(Capability.DEPTH_TEST);
        if (glVersion == GLVersion.GL32 || GLContext.getCapabilities().GL_ARB_depth_clamp) {
            pipelineBuilder = pipelineBuilder.enableCapabilities(Capability.DEPTH_CLAMP);
        }
        // GUI
        pipelineBuilder = pipelineBuilder.useCamera(guiCamera).clearBuffer().renderModels(guiRenderList).useCamera(modelCamera).updateDisplay();
        pipeline = pipelineBuilder.build();
    }

    private static void initPrograms() {
        // SOLID
        loadProgram("solid");
        // TEXTURED
        loadProgram("textured");
        /// FONT
        loadProgram("font");
        // SSAO
        loadProgram("ssao");
        // SHADOW
        loadProgram("shadow");
        // BLUR
        loadProgram("blur");
        // LIGHTING
        loadProgram("lighting");
        // MOTION BLUR
        loadProgram("motionBlur");
        // ANTI ALIASING
        loadProgram("edaa");
        // SCREEN
        loadProgram("screen");
    }

    private static void loadProgram(String name) {
        final String shaderPath = "/shaders/glsl" + (context.getGLVersion().getMajor() == 3 ? GLVersion.GL33.getGLSLFull() : GLVersion.GL21.getGLSLFull()) + "/" + name;
        // SHADERS
        final Shader vert = context.newShader();
        vert.create();
        vert.setSource(new ShaderSource(Sandbox.class.getResourceAsStream(shaderPath + ".vert")));
        vert.compile();
        final Shader frag = context.newShader();
        frag.create();
        frag.setSource(new ShaderSource(Sandbox.class.getResourceAsStream(shaderPath + ".frag")));
        frag.compile();
        // PROGRAM
        final Program program = context.newProgram();
        program.create();
        program.attachShader(vert);
        program.attachShader(frag);
        program.link();
        programs.put(name, program);
    }

    private static void initTextures() {
        ByteBuffer data;
        final Rectangle size = new Rectangle();
        // CREEPER DIFFUSE
        creeperDiffuseTexture = context.newTexture();
        creeperDiffuseTexture.create();
        creeperDiffuseTexture.setFilters(FilterMode.NEAREST, FilterMode.NEAREST);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_diffuse.png"), Format.RGB, size);
        creeperDiffuseTexture.setImageData(data, size.getWidth(), size.getHeight());
        // CREEPER NORMALS
        creeperNormalsTexture = context.newTexture();
        creeperNormalsTexture.create();
        creeperNormalsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_normals.png"), Format.RGB, size);
        creeperNormalsTexture.setImageData(data, size.getWidth(), size.getHeight());
        // CREEPER SPECULAR
        creeperSpecularTexture = context.newTexture();
        creeperSpecularTexture.create();
        creeperSpecularTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        creeperSpecularTexture.setFormat(Format.RED, InternalFormat.R8);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/creeper_specular.png"), Format.RED, size);
        creeperSpecularTexture.setImageData(data, size.getWidth(), size.getHeight());
        // WOOD DIFFUSE
        woodDiffuseTexture = context.newTexture();
        woodDiffuseTexture.create();
        woodDiffuseTexture.setFilters(FilterMode.LINEAR_MIPMAP_LINEAR, FilterMode.LINEAR);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_diffuse.png"), Format.RGB, size);
        woodDiffuseTexture.setImageData(data, size.getWidth(), size.getHeight());
        woodDiffuseTexture.setAnisotropicFiltering(16);
        // WOOD NORMALS
        woodNormalsTexture = context.newTexture();
        woodNormalsTexture.create();
        woodNormalsTexture.setFilters(FilterMode.LINEAR_MIPMAP_LINEAR, FilterMode.LINEAR);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_normals.png"), Format.RGB, size);
        woodNormalsTexture.setImageData(data, size.getWidth(), size.getHeight());
        woodNormalsTexture.setAnisotropicFiltering(16);
        // WOOD SPECULAR
        woodSpecularTexture = context.newTexture();
        woodSpecularTexture.create();
        woodSpecularTexture.setFormat(Format.RED, InternalFormat.R8);
        woodSpecularTexture.setFilters(FilterMode.LINEAR_MIPMAP_LINEAR, FilterMode.LINEAR);
        data = CausticUtil.getImageData(Sandbox.class.getResourceAsStream("/textures/wood_specular.png"), Format.RED, size);
        woodSpecularTexture.setImageData(data, size.getWidth(), size.getHeight());
        woodSpecularTexture.setAnisotropicFiltering(16);
        // COLORS
        colorsTexture = context.newTexture();
        colorsTexture.create();
        colorsTexture.setFormat(Format.RGBA, InternalFormat.RGBA8);
        colorsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        colorsTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // NORMALS
        normalsTexture = context.newTexture();
        normalsTexture.create();
        normalsTexture.setFormat(Format.RGBA, InternalFormat.RGBA8);
        normalsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        normalsTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // VERTEX NORMALS
        vertexNormals = context.newTexture();
        vertexNormals.create();
        vertexNormals.setFormat(Format.RGBA, InternalFormat.RGBA8);
        vertexNormals.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        vertexNormals.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // MATERIALS
        materialsTexture = context.newTexture();
        materialsTexture.create();
        materialsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        materialsTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // VELOCITIES
        velocitiesTexture = context.newTexture();
        velocitiesTexture.create();
        velocitiesTexture.setFormat(Format.RG, InternalFormat.RG16F);
        velocitiesTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        velocitiesTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // DEPTHS
        depthsTexture = context.newTexture();
        depthsTexture.create();
        depthsTexture.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        depthsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        depthsTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        depthsTexture.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
        // LIGHT DEPTHS
        lightDepthsTexture = context.newTexture();
        lightDepthsTexture.create();
        lightDepthsTexture.setFormat(Format.DEPTH, InternalFormat.DEPTH_COMPONENT32);
        lightDepthsTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        lightDepthsTexture.setImageData(null, SHADOW_SIZE.getX(), SHADOW_SIZE.getY());
        lightDepthsTexture.setWraps(WrapMode.CLAMP_TO_BORDER, WrapMode.CLAMP_TO_BORDER);
        lightDepthsTexture.setCompareMode(CompareMode.LESS);
        // SSAO
        ssaoTexture = context.newTexture();
        ssaoTexture.create();
        ssaoTexture.setFormat(Format.RED);
        ssaoTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        ssaoTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // SHADOW
        shadowTexture = context.newTexture();
        shadowTexture.create();
        shadowTexture.setFormat(Format.RED);
        shadowTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        shadowTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // AUX R
        auxRTexture = context.newTexture();
        auxRTexture.create();
        auxRTexture.setFormat(Format.RED);
        auxRTexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        auxRTexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        // AUX RGBA
        auxRGBATexture = context.newTexture();
        auxRGBATexture.create();
        auxRGBATexture.setFormat(Format.RGBA, InternalFormat.RGBA8);
        auxRGBATexture.setFilters(FilterMode.LINEAR, FilterMode.LINEAR);
        auxRGBATexture.setImageData(null, WINDOW_SIZE.getX(), WINDOW_SIZE.getY());
        auxRGBATexture.setWraps(WrapMode.CLAMP_TO_EDGE, WrapMode.CLAMP_TO_EDGE);
    }

    private static void initMaterials() {
        UniformHolder uniforms;
        // SOLID
        solidMaterial = createMaterial("solid");
        uniforms = solidMaterial.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("specularIntensity", 1));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        // WIREFRAME
        wireframeMaterial = createMaterial("solid");
        uniforms = wireframeMaterial.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0));
        uniforms.add(new FloatUniform("specularIntensity", 0));
        uniforms.add(new FloatUniform("ambientIntensity", 1));
        // CREEPER
        creeperMaterial = createMaterial("textured");
        creeperMaterial.addTexture(0, creeperDiffuseTexture);
        creeperMaterial.addTexture(1, creeperNormalsTexture);
        creeperMaterial.addTexture(2, creeperSpecularTexture);
        uniforms = creeperMaterial.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        // WOOD
        woodMaterial = createMaterial("textured");
        woodMaterial.addTexture(0, woodDiffuseTexture);
        woodMaterial.addTexture(1, woodNormalsTexture);
        woodMaterial.addTexture(2, woodSpecularTexture);
        uniforms = woodMaterial.getUniforms();
        uniforms.add(new FloatUniform("diffuseIntensity", 0.8f));
        uniforms.add(new FloatUniform("ambientIntensity", 0.2f));
        // SSAO
        ssaoMaterial = createMaterial("ssao");
        ssaoMaterial.addTexture(0, normalsTexture);
        ssaoMaterial.addTexture(1, depthsTexture);
        ssaoMaterial.addTexture(2, ssaoEffect.getNoiseTexture());
        uniforms = ssaoMaterial.getUniforms();
        uniforms.add(new Vector2Uniform("projection", PROJECTION));
        uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
        ssaoEffect.addUniforms(uniforms);
        // SHADOW
        shadowMaterial = createMaterial("shadow");
        shadowMaterial.addTexture(0, vertexNormals);
        shadowMaterial.addTexture(1, depthsTexture);
        shadowMaterial.addTexture(2, lightDepthsTexture);
        shadowMaterial.addTexture(3, shadowMappingEffect.getNoiseTexture());
        uniforms = shadowMaterial.getUniforms();
        uniforms.add(new Vector2Uniform("projection", PROJECTION));
        uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
        uniforms.add(lightPositionUniform);
        uniforms.add(inverseViewMatrixUniform);
        uniforms.add(lightViewMatrixUniform);
        uniforms.add(lightProjectionMatrixUniform);
        shadowMappingEffect.addUniforms(uniforms);
        // BLUR
        blurMaterial = createMaterial("blur");
        blurMaterial.addTexture(0, auxRTexture);
        blurMaterial.addTexture(1, auxRGBATexture);
        uniforms = blurMaterial.getUniforms();
        blurEffect.addUniforms(uniforms);
        // LIGHTING
        lightingMaterial = createMaterial("lighting");
        lightingMaterial.addTexture(0, colorsTexture);
        lightingMaterial.addTexture(1, normalsTexture);
        lightingMaterial.addTexture(2, depthsTexture);
        lightingMaterial.addTexture(3, materialsTexture);
        lightingMaterial.addTexture(4, ssaoTexture);
        lightingMaterial.addTexture(5, shadowTexture);
        uniforms = lightingMaterial.getUniforms();
        uniforms.add(new Vector2Uniform("projection", PROJECTION));
        uniforms.add(new FloatUniform("tanHalfFOV", TAN_HALF_FOV));
        uniforms.add(new FloatUniform("aspectRatio", ASPECT_RATIO));
        uniforms.add(lightPositionUniform);
        uniforms.add(lightAttenuationUniform);
        uniforms.add(new FloatUniform("spotCutoff", TrigMath.cos(Sandbox.SPOT_CUTOFF)));
        uniforms.add(spotDirectionUniform);
        // MOTION BLUR
        motionBlurMaterial = createMaterial("motionBlur");
        motionBlurMaterial.addTexture(0, auxRGBATexture);
        motionBlurMaterial.addTexture(1, velocitiesTexture);
        uniforms = motionBlurMaterial.getUniforms();
        uniforms.add(new Vector2Uniform("resolution", WINDOW_SIZE.toFloat()));
        uniforms.add(new IntUniform("sampleCount", 8));
        uniforms.add(blurStrengthUniform);
        // ANTI ALIASING
        antiAliasingMaterial = createMaterial("edaa");
        antiAliasingMaterial.addTexture(0, colorsTexture);
        antiAliasingMaterial.addTexture(1, vertexNormals);
        antiAliasingMaterial.addTexture(2, depthsTexture);
        uniforms = antiAliasingMaterial.getUniforms();
        uniforms.add(new Vector2Uniform("projection", PROJECTION));
        uniforms.add(new Vector2Uniform("resolution", WINDOW_SIZE.toFloat()));
        uniforms.add(new FloatUniform("maxSpan", 8));
        uniforms.add(new Vector2Uniform("barriers", new Vector2f(0.8f, 0.5f)));
        uniforms.add(new Vector2Uniform("weights", new Vector2f(0.25f, 0.6f)));
        uniforms.add(new FloatUniform("kernel", 0.75f));
        // SCREEN
        screenMaterial = createMaterial("screen");
        screenMaterial.addTexture(0, auxRGBATexture);
    }

    private static Material createMaterial(String program) {
        return new Material(programs.get(program));
    }

    private static void initFrameBuffers() {
        // MODEL
        modelFrameBuffer = context.newFrameBuffer();
        modelFrameBuffer.create();
        modelFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
        modelFrameBuffer.attach(AttachmentPoint.COLOR1, normalsTexture);
        modelFrameBuffer.attach(AttachmentPoint.COLOR2, vertexNormals);
        modelFrameBuffer.attach(AttachmentPoint.COLOR3, materialsTexture);
        modelFrameBuffer.attach(AttachmentPoint.COLOR4, velocitiesTexture);
        modelFrameBuffer.attach(AttachmentPoint.DEPTH, depthsTexture);
        // LIGHT MODEL
        lightModelFrameBuffer = context.newFrameBuffer();
        lightModelFrameBuffer.create();
        lightModelFrameBuffer.attach(AttachmentPoint.DEPTH, lightDepthsTexture);
        // SSAO
        ssaoFrameBuffer = context.newFrameBuffer();
        ssaoFrameBuffer.create();
        ssaoFrameBuffer.attach(AttachmentPoint.COLOR0, auxRTexture);
        // SHADOW
        shadowFrameBuffer = context.newFrameBuffer();
        shadowFrameBuffer.create();
        shadowFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
        // BLUR
        blurFrameBuffer = context.newFrameBuffer();
        blurFrameBuffer.create();
        blurFrameBuffer.attach(AttachmentPoint.COLOR0, ssaoTexture);
        blurFrameBuffer.attach(AttachmentPoint.COLOR1, shadowTexture);
        // LIGHTING
        lightingFrameBuffer = context.newFrameBuffer();
        lightingFrameBuffer.create();
        lightingFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
        // MOTION BLUR
        motionBlurFrameBuffer = context.newFrameBuffer();
        motionBlurFrameBuffer.create();
        motionBlurFrameBuffer.attach(AttachmentPoint.COLOR0, colorsTexture);
        // ANTI ALIASING
        antiAliasingFrameBuffer = context.newFrameBuffer();
        antiAliasingFrameBuffer.create();
        antiAliasingFrameBuffer.attach(AttachmentPoint.COLOR0, auxRGBATexture);
    }

    private static void initVertexArrays() {
        // UNIT WIRE CUBE
        unitCubeWireVertexArray = context.newVertexArray();
        unitCubeWireVertexArray.create();
        unitCubeWireVertexArray.setData(MeshGenerator.generateWireCuboid(null, new Vector3f(1, 1, 1)));
        unitCubeWireVertexArray.setDrawingMode(DrawingMode.LINES);
        // DIAMOND MODEL
        diamondModelVertexArray = context.newVertexArray();
        diamondModelVertexArray.create();
        diamondModelVertexArray.setData(loadOBJ(Sandbox.class.getResourceAsStream("/models/diamond.obj")));
        // DEFERRED STAGE SCREEN
        deferredStageScreenVertexArray = context.newVertexArray();
        deferredStageScreenVertexArray.create();
        deferredStageScreenVertexArray.setData(MeshGenerator.generateTexturedPlane(null, new Vector2f(2, 2)));
    }

    public static void dispose() {
        disposeEffects();
        disposePrograms();
        disposeTextures();
        disposeFrameBuffers();
        disposeVertexArrays();
        disposeContext();
    }

    private static void disposeContext() {
        // CONTEXT
        context.destroy();
    }

    private static void disposeEffects() {
        // SSAO
        ssaoEffect.dispose();
        // SHADOW MAPPING
        shadowMappingEffect.dispose();
        // BLUR
        blurEffect.dispose();
    }

    private static void disposePrograms() {
        for (Program program : programs.values()) {
            // SHADERS
            for (Shader shader : program.getShaders()) {
                shader.destroy();
            }
            // PROGRAM
            program.destroy();
        }
    }

    private static void disposeTextures() {
        // CREEPER DIFFUSE
        creeperDiffuseTexture.destroy();
        // CREEPER NORMALS
        creeperNormalsTexture.destroy();
        // CREEPER SPECULAR
        creeperSpecularTexture.destroy();
        // WOOD DIFFUSE
        woodDiffuseTexture.destroy();
        // WOOD NORMALS
        woodNormalsTexture.destroy();
        // WOOD SPECULAR
        woodSpecularTexture.destroy();
        // COLOR
        colorsTexture.destroy();
        // NORMALS
        normalsTexture.destroy();
        // VERTEX NORMALS
        vertexNormals.destroy();
        // MATERIALS
        materialsTexture.destroy();
        // VELOCITIES
        velocitiesTexture.destroy();
        // DEPTHS
        depthsTexture.destroy();
        // LIGHT DEPTHS
        lightDepthsTexture.destroy();
        // SSAO
        ssaoTexture.destroy();
        // SHADOW
        shadowTexture.destroy();
        // AUX R
        auxRTexture.destroy();
        // AUX RGB
        auxRGBATexture.destroy();
    }

    private static void disposeFrameBuffers() {
        // MODEL
        modelFrameBuffer.destroy();
        // SHADOW
        lightModelFrameBuffer.destroy();
        // SSAO
        ssaoFrameBuffer.destroy();
        // SHADOW
        shadowFrameBuffer.destroy();
        // BLUR
        blurFrameBuffer.destroy();
        // LIGHTING
        lightingFrameBuffer.destroy();
        // MOTION BLUR
        motionBlurFrameBuffer.destroy();
        // ANTI ALIASING
        antiAliasingFrameBuffer.destroy();
    }

    private static void disposeVertexArrays() {
        // UNIT WIRE CUBE
        unitCubeWireVertexArray.destroy();
        // DIAMOND MODEL
        diamondModelVertexArray.destroy();
        // DEFERRED STAGE SCREEN
        deferredStageScreenVertexArray.destroy();
    }

    public static void setGLVersion(GLVersion version) {
        switch (version) {
            case GL20:
                context = GLImplementation.get(LWJGLUtil.GL20_IMPL);
                break;
            case GL21:
                context = GLImplementation.get(LWJGLUtil.GL21_IMPL);
                break;
            case GL30:
                context = GLImplementation.get(LWJGLUtil.GL30_IMPL);
                break;
            case GL32:
                context = GLImplementation.get(LWJGLUtil.GL32_IMPL);
                break;
        }
    }

    public static void setCullBackFaces(boolean cull) {
        cullBackFaces = cull;
    }

    public static void setBackgroundColor(Vector4f color) {
        backgroundColor = color;
    }

    public static void setLightAttenuation(float attenuation) {
        lightAttenuationUniform.set(attenuation);
    }

    public static Vector4f getAABBColor() {
        return aabbModelColor;
    }

    public static void setAABBColor(Vector4f color) {
        aabbModelColor = color;
    }

    public static Vector4f getDiamondColor() {
        return diamondModelColor;
    }

    public static void setDiamondColor(Vector4f color) {
        diamondModelColor = color;
    }

    public static Vector4f getCylinderColor() {
        return cylinderModelColor;
    }

    public static void setCylinderColor(Vector4f color) {
        cylinderModelColor = color;
    }

    public static Vector4f getSphereColor() {
        return sphereModelColor;
    }

    public static void setSphereColor(Vector4f color) {
        sphereModelColor = color;
    }

    public static Camera getCamera() {
        return modelCamera;
    }

    public static void setLightPosition(Vector3f position) {
        lightPositionUniform.set(position);
        lightCamera.setPosition(position);
    }

    public static void setLightDirection(Vector3f direction) {
        direction = direction.normalize();
        spotDirectionUniform.set(direction);
        lightCamera.setRotation(Quaternionf.fromRotationTo(Vector3f.FORWARD.negate(), direction));
    }

    public static Model addAABB(Vector3f position, Vector3f size) {
        final Model model = new Model(unitCubeWireVertexArray, wireframeMaterial);
        model.setPosition(position);
        model.setScale(size);
        model.getUniforms().add(new Vector4Uniform("modelColor", aabbModelColor));
        addModel(model);
        return model;
    }

    public static Model addBox(Vector3f position, Quaternionf orientation, Vector3f size) {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(MeshGenerator.generateCuboid(null, size.mul(2)));
        final Model model = new Model(vertexArray, woodMaterial);
        model.setPosition(position);
        model.setRotation(orientation);
        addModel(model);
        return model;
    }

    public static Model addDiamond(Vector3f position, Quaternionf orientation) {
        final Model model = new Model(diamondModelVertexArray, solidMaterial);
        model.setPosition(position);
        model.setRotation(orientation);
        model.getUniforms().add(new Vector4Uniform("modelColor", diamondModelColor));
        addModel(model);
        return model;
    }

    public static Model addCylinder(Vector3f position, Quaternionf orientation, float radius, float height) {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(MeshGenerator.generateCylinder(null, radius, height));
        final Model model = new Model(vertexArray, solidMaterial);
        model.setPosition(position);
        model.setRotation(orientation);
        model.getUniforms().add(new Vector4Uniform("modelColor", cylinderModelColor));
        addModel(model);
        return model;
    }

    public static Model addSphere(Vector3f position, Quaternionf orientation, float radius) {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(MeshGenerator.generateSphere(null, radius));
        final Model model = new Model(vertexArray, solidMaterial);
        model.setPosition(position);
        model.setRotation(orientation);
        model.getUniforms().add(new Vector4Uniform("modelColor", sphereModelColor));
        addModel(model);
        return model;
    }

    public static void addModel(Model model) {
        model.getUniforms().add(new Matrix4Uniform("previousModelMatrix", model.getMatrix()));
        modelRenderList.add(model);
    }

    public static void removeModel(Model model) {
        modelRenderList.remove(model);
    }

    public static void addDefaultObjects() {
        addScreen();
        addCrosshairs();
        addFPSMonitor();
        addCreeper();
        addSuzanne();
    }

    private static void addScreen() {
        guiRenderList.add(new Model(deferredStageScreenVertexArray, screenMaterial));
    }

    private static void addCrosshairs() {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(MeshGenerator.generateCrosshairs(null, 0.02f));
        final Model model = new Model(vertexArray, wireframeMaterial);
        vertexArray.setDrawingMode(DrawingMode.LINES);
        model.getUniforms().add(new Vector4Uniform("modelColor", CausticUtil.WHITE));
        model.setPosition(new Vector3f(0.5, (1 / ASPECT_RATIO) / 2, -0.1));
        guiRenderList.add(model);
    }

    private static void addFPSMonitor() {
        final Font ubuntu;
        try {
            ubuntu = Font.createFont(Font.TRUETYPE_FONT, Sandbox.class.getResourceAsStream("/fonts/ubuntu-r.ttf"));
        } catch (FontFormatException | IOException e) {
            System.out.println(e);
            return;
        }
        final StringModel sandboxModel = new StringModel(context, programs.get("font"), "SandboxPweryCusticRF0123456789,&: ", ubuntu.deriveFont(Font.PLAIN, 15), WINDOW_SIZE.getX());
        final float aspect = 1 / ASPECT_RATIO;
        sandboxModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.315, -0.1));
        final String white = "#ffffffff", brown = "#ffC19953", green = "#ff00ff00", cyan = "#ff4fB5ff";
        sandboxModel.setString(brown + "Sandbox\n" + white + "Powered by " + green + "Caustic" + white + " & " + cyan + "React");
        guiRenderList.add(sandboxModel);
        final StringModel fpsModel = sandboxModel.getInstance();
        fpsModel.setPosition(new Vector3f(0.005, aspect / 2 + 0.285, -0.1));
        fpsModel.setString("FPS: " + fpsMonitor.getFPS());
        guiRenderList.add(fpsModel);
        fpsMonitorModel = fpsModel;
    }

    private static void addCreeper() {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(loadOBJ(Sandbox.class.getResourceAsStream("/models/creeper.obj")));
        final Model mobModel = new Model(vertexArray, creeperMaterial);
        mobModel.setPosition(new Vector3f(10, 10, 0));
        mobModel.setRotation(com.flowpowered.math.imaginary.Quaternionf.fromAngleDegAxis(-90, 0, 1, 0));
        addModel(mobModel);
        // Add a second mob, instanced from the first one
        movingMobModel = mobModel.getInstance();
        addModel(movingMobModel);
    }

    private static void addSuzanne() {
        final VertexArray vertexArray = context.newVertexArray();
        vertexArray.create();
        vertexArray.setData(loadCollada(Sandbox.class.getResourceAsStream("/models/suzanne.dae")));
        final Model model = new Model(vertexArray, solidMaterial);
        model.setPosition(new Vector3f(0, 10, -10));
        model.getUniforms().add(new Vector4Uniform("modelColor", sphereModelColor));
        addModel(model);
    }

    public static void startFPSMonitor() {
        fpsMonitor.start();
    }

    public static void render() {
        // UPDATE PER-FRAME UNIFORMS
        inverseViewMatrixUniform.set(modelCamera.getViewMatrix().invert());
        lightViewMatrixUniform.set(lightCamera.getViewMatrix());
        lightProjectionMatrixUniform.set(lightCamera.getProjectionMatrix());
        blurStrengthUniform.set((float) fpsMonitor.getFPS() / Sandbox.TARGET_FPS);
        // ANIMATE MOVING MOB
        final float time = (System.currentTimeMillis() % 1000) / 1000f;
        movingMobModel.setPosition(new Vector3f(2 * TrigMath.sin(2 * (float) TrigMath.PI * time), 0, 0).add(-10, 10, 0));
        movingMobModel.setRotation(Quaternionf.fromAngleDegAxis(time * 360, 1, 1, 1));
        // RENDER
        pipeline.run(context);
        // UPDATE PREVIOUS FRAME UNIFORMS
        setPreviousModelMatrices();
        previousViewMatrixUniform.set(modelCamera.getViewMatrix());
        previousProjectionMatrixUniform.set(modelCamera.getProjectionMatrix());
        // UPDATE FPS
        updateFPSMonitor();
    }

    private static void setPreviousModelMatrices() {
        for (Model model : modelRenderList) {
            model.getUniforms().getMatrix4("previousModelMatrix").set(model.getMatrix());
        }
    }

    private static void updateFPSMonitor() {
        fpsMonitor.update();
        fpsMonitorModel.setString("FPS: " + fpsMonitor.getFPS());
    }

    private static VertexData loadMesh(Vector3f sizes, TFloatList positions, TFloatList textureCoords, TFloatList normals, TIntList indices) {
        final VertexData vertexData = new VertexData();
        // POSITIONS
        final VertexAttribute positionAttribute = new VertexAttribute("positions", DataType.FLOAT, sizes.getFloorX());
        positionAttribute.setData(positions);
        vertexData.addAttribute(0, positionAttribute);
        // NORMALS
        final VertexAttribute normalAttribute;
        if (sizes.getZ() <= 0) {
            normalAttribute = new VertexAttribute("normals", DataType.FLOAT, 3);
            CausticUtil.generateNormals(positions, indices, normals);
        } else {
            normalAttribute = new VertexAttribute("normals", DataType.FLOAT, sizes.getFloorZ());
        }
        normalAttribute.setData(normals);
        vertexData.addAttribute(1, normalAttribute);
        // TEXTURE COORDS
        if (sizes.getY() > 0) {
            final VertexAttribute textureCoordAttribute = new VertexAttribute("textureCoords", DataType.FLOAT, sizes.getFloorY());
            textureCoordAttribute.setData(textureCoords);
            vertexData.addAttribute(2, textureCoordAttribute);
            // TANGENTS
            final VertexAttribute tangentAttribute = new VertexAttribute("tangents", DataType.FLOAT, 4);
            tangentAttribute.setData(CausticUtil.generateTangents(positions, normals, textureCoords, indices));
            vertexData.addAttribute(3, tangentAttribute);
        }
        // INDICES
        vertexData.getIndices().addAll(indices);
        return vertexData;
    }

    private static VertexData loadOBJ(InputStream file) {
        // LOAD
        final TFloatList positions = new TFloatArrayList();
        final TFloatList textureCoords = new TFloatArrayList();
        final TFloatList normals = new TFloatArrayList();
        final TIntList indices = new TIntArrayList();
        final Vector3f sizes = ObjFileLoader.load(file, positions, textureCoords, normals, indices);
        return loadMesh(sizes, positions, textureCoords, normals, indices);
    }

    private static VertexData loadCollada(InputStream in) {
        final TFloatList positions = new TFloatArrayList();
        final TFloatList textureCoords = new TFloatArrayList();
        final TFloatList normals = new TFloatArrayList();
        final TIntList indices = new TIntArrayList();
        return loadMesh(
                ColladaFileLoader.loadMesh(in, positions, textureCoords, normals, indices),
                positions, textureCoords, normals, indices
        );
    }

    public static void saveScreenshot() {
        final ByteBuffer buffer = context.readFrame(new Rectangle(Vector2i.ZERO, WINDOW_SIZE), InternalFormat.RGB8);
        final int width = context.getWindowWidth();
        final int height = context.getWindowHeight();
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int srcIndex = (x + y * width) * 3;
                final int destIndex = (x + (height - y - 1) * width) * 3;
                data[destIndex + 2] = buffer.get(srcIndex);
                data[destIndex + 1] = buffer.get(srcIndex + 1);
                data[destIndex] = buffer.get(srcIndex + 2);
            }
        }
        try {
            ImageIO.write(image, "PNG", new File("screenshots" + File.separator + SCREENSHOT_DATE_FORMAT.format(Calendar.getInstance().getTime()) + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class DoDeferredStageAction extends RenderModelsAction {
        private final FrameBuffer frameBuffer;

        private DoDeferredStageAction(FrameBuffer frameBuffer, VertexArray screen, Material material) {
            super(Arrays.asList(new Model(screen, material)));
            this.frameBuffer = frameBuffer;
        }

        @Override
        public void execute(Context context) {
            frameBuffer.bind();
            super.execute(context);
        }
    }
}
