#version 330

in vec3 position;
in vec2 texcoord;

out vec2 textureCoord;

void main()
{
	textureCoord = texcoord;
	gl_Position = vec4(position, 1.0);
}
