#version 120

varying vec3 positionView;
varying vec3 normalView;
varying vec2 textureUV;

uniform sampler2D diffuse;

void main() {
    gl_FragData[0] = texture2D(diffuse, textureUV);

    gl_FragData[1] = vec4(normalView, 1);
}
