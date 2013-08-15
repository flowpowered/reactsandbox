#version 120

varying vec3 positionView;

uniform vec4 modelColor;

void main() {
    gl_FragData[0] = modelColor;

    gl_FragData[1] = vec4(0, 0, 0, 1);
}
