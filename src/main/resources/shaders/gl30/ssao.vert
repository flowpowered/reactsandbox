#version 330

layout(location = 0) in vec3 position;

out vec2 textureUV;
noperspective out vec3 viewDirection;

void main() {
    textureUV = (position.xy + 1) / 2;

    // TODO: make me a uniform
    const float tanHalfFOV = tan(radians(60) / 2);
    const float aspectRatio = 1200.0 / 800.0;
    viewDirection = vec3(position.x * tanHalfFOV * aspectRatio, position.y * tanHalfFOV, -1);

    gl_Position = vec4(position, 1);
}
