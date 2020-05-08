#version 330

in vec3 position;
in vec2 texcoord;

out vec2 textureCoord;

uniform mat4 modelMatrix;
uniform mat4 projectionMatrix;

void main()
{
	textureCoord = texcoord;
	gl_Position = projectionMatrix * modelMatrix * vec4(position, 1.0);
}
