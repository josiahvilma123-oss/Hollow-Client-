package dev.hollow.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards against shader code that lenient drivers accept but strict ones reject. A shader that
 * fails to compile on a player's driver turns off the effect that uses it, so these mistakes
 * are caught here instead.
 */
class ShaderSourceTest {
	private static final Path SHADERS = Path.of("src/main/resources/assets/hollow/shaders");
	/** GLSL built-in function names; redefining one with a different return type is a compile error on most drivers. */
	private static final Set<String> BUILT_INS = Set.of("noise1", "noise2", "noise3", "noise4", "radians", "degrees", "sin", "cos", "tan",
		"asin", "acos", "atan", "pow", "exp", "log", "exp2", "log2", "sqrt", "inversesqrt", "abs", "sign", "floor", "ceil", "fract", "mod",
		"min", "max", "clamp", "mix", "step", "smoothstep", "length", "distance", "dot", "cross", "normalize", "reflect", "refract",
		"texture", "round", "trunc", "isnan", "isinf", "transpose", "inverse", "determinant", "outerProduct");
	private static final Pattern FUNCTION = Pattern.compile("(?m)^\\s*(?:float|int|bool|void|vec[234]|ivec[234]|mat[234])\\s+(\\w+)\\s*\\(");
	private static final Pattern REVERSED_SMOOTHSTEP = Pattern.compile("smoothstep\\(\\s*([0-9.]+)\\s*,\\s*([0-9.]+)\\s*,");

	@Test
	void noShaderRedefinesBuiltInFunctions() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Path file : shaderFiles()) {
			Matcher m = FUNCTION.matcher(Files.readString(file));
			while (m.find()) {
				if (BUILT_INS.contains(m.group(1))) {
					problems.add(file.getFileName() + " defines built-in name " + m.group(1));
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void smoothstepEdgesAreAscending() throws IOException {
		// smoothstep(edge0, edge1, x) is undefined when edge0 >= edge1; use 1.0 - smoothstep(edge1, edge0, x).
		List<String> problems = new ArrayList<>();
		for (Path file : shaderFiles()) {
			Matcher m = REVERSED_SMOOTHSTEP.matcher(Files.readString(file));
			while (m.find()) {
				if (Double.parseDouble(m.group(1)) >= Double.parseDouble(m.group(2))) {
					problems.add(file.getFileName() + ": " + m.group());
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static List<Path> shaderFiles() throws IOException {
		try (Stream<Path> files = Files.walk(SHADERS)) {
			return files.filter(p -> p.toString().endsWith(".fsh") || p.toString().endsWith(".vsh") || p.toString().endsWith(".glsl")).toList();
		}
	}
}
